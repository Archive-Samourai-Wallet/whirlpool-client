package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import java.util.Map;
import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletService {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWalletService.class);

  private Optional<WhirlpoolWallet> whirlpoolWallet;

  public WhirlpoolWalletService() {
    this.whirlpoolWallet = Optional.empty();

    // set user-agent
    ClientUtils.setupEnv();
  }

  public synchronized void closeWallet() {
    if (whirlpoolWallet.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("Closing wallet");
      }
      WhirlpoolWallet wp = whirlpoolWallet.get();
      wp.stop();
      wp.close();
      whirlpoolWallet = Optional.empty();
    } else {
      if (log.isDebugEnabled()) {
        log.debug("closeWallet skipped: no wallet opened");
      }
    }
  }

  public WhirlpoolWallet openWallet(
      WhirlpoolWalletConfig config,
      HD_Wallet bip84w,
      String walletStateFileName,
      String utxoConfigFileName)
      throws Exception {
    WhirlpoolWallet wp =
        computeWhirlpoolWallet(config, bip84w, walletStateFileName, utxoConfigFileName);
    return openWallet(wp);
  }

  protected synchronized WhirlpoolWallet openWallet(WhirlpoolWallet wp) throws Exception {
    if (whirlpoolWallet.isPresent()) {
      throw new Exception("WhirlpoolWallet already opened");
    }

    wp.open(); // load initial data
    whirlpoolWallet = Optional.of(wp);

    Bip84Wallet depositWallet = wp.getWalletDeposit();
    Bip84Wallet premixWallet = wp.getWalletPremix();
    Bip84Wallet postmixWallet = wp.getWalletPostmix();

    // log zpubs
    if (log.isDebugEnabled()) {
      log.debug(
          "Deposit wallet: accountIndex="
              + depositWallet.getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(depositWallet.getZpub()));
      log.debug(
          "Premix wallet: accountIndex="
              + premixWallet.getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(premixWallet.getZpub()));
      log.debug(
          "Postmix wallet: accountIndex="
              + postmixWallet.getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(postmixWallet.getZpub()));
    }
    return wp;
  }

  protected WhirlpoolWallet computeWhirlpoolWallet(
      WhirlpoolWalletConfig config,
      HD_Wallet hdWallet,
      String walletStateFileName,
      String utxoConfigFileName) {
    // debug whirlpoolWalletConfig
    if (log.isDebugEnabled()) {
      log.debug("openWallet with whirlpoolWalletConfig:");
      for (Map.Entry<String, String> entry : config.getConfigInfo().entrySet()) {
        log.debug("[whirlpoolWalletConfig/" + entry.getKey() + "] " + entry.getValue());
      }
      if (log.isDebugEnabled()) {
        log.debug("walletStateFile: " + walletStateFileName);
        log.debug("utxoConfigFile: " + utxoConfigFileName);
      }
    }

    Tx0Service tx0Service = new Tx0Service(config);
    Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

    int externalIndexDefault =
        config.getExternalDestination() != null
            ? config.getExternalDestination().getStartIndex()
            : 0;
    WalletSupplier walletSupplier =
        new WalletSupplier(
            new WalletStatePersister(walletStateFileName),
            config.getBackendApi(),
            hdWallet,
            externalIndexDefault);

    PoolSupplier poolSupplier =
        new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi());

    WalletDataSupplier walletDataSupplier =
        computeWalletDataSupplier(
            walletSupplier, poolSupplier, computeUtxoChangesListener(), utxoConfigFileName, config);

    return new WhirlpoolWallet(
        config,
        walletDataSupplier.getTx0ParamService(),
        tx0Service,
        bech32Util,
        walletSupplier,
        poolSupplier,
        walletDataSupplier);
  }

  // overridable for android
  protected WalletDataSupplier computeWalletDataSupplier(
      WalletSupplier walletSupplier,
      PoolSupplier poolSupplier,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      String utxoConfigFileName,
      WhirlpoolWalletConfig config) {
    return new WalletDataSupplier(
        config.getRefreshUtxoDelay(),
        walletSupplier,
        poolSupplier,
        utxoChangesListener,
        utxoConfigFileName,
        config);
  }

  protected MessageListener<WhirlpoolUtxoChanges> computeUtxoChangesListener() {
    return new MessageListener<WhirlpoolUtxoChanges>() {
      @Override
      public void onMessage(WhirlpoolUtxoChanges message) {
        if (!whirlpoolWallet.isPresent()) {
          // this happens on first data load
          if (log.isDebugEnabled()) {
            log.debug("ignoring onUtxoChanges: no wallet opened");
          }
          return;
        }
        whirlpoolWallet.get()._onUtxoChanges(message);
      }
    };
  }

  public Optional<WhirlpoolWallet> getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
