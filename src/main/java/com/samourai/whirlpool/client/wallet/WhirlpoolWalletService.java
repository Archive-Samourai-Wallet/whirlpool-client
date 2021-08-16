package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.BackendWalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import java.util.Map;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
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
      WhirlpoolWalletConfig config, byte[] seed, String seedPassphrase) throws Exception {
    WhirlpoolWallet wp = computeWhirlpoolWallet(config, seed, seedPassphrase);
    return openWallet(wp);
  }

  public WhirlpoolWallet openWallet(WhirlpoolWalletConfig config, HD_Wallet bip84w)
      throws Exception {
    WhirlpoolWallet wp = computeWhirlpoolWallet(config, bip84w);
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

  protected String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  protected WhirlpoolWallet computeWhirlpoolWallet(WhirlpoolWalletConfig config, HD_Wallet bip84w)
      throws Exception {
    return computeWhirlpoolWallet(config, bip84w.getSeed(), bip84w.getPassphrase());
  }

  protected WhirlpoolWallet computeWhirlpoolWallet(
      WhirlpoolWalletConfig config, byte[] seed, String seedPassphrase) throws Exception {
    NetworkParameters params = config.getNetworkParameters();
    if (seedPassphrase == null) {
      seedPassphrase = "";
    }
    String walletIdentifier = computeWalletIdentifier(seed, seedPassphrase, params);
    HD_Wallet bip84w = HD_WalletFactoryJava.getInstance().getBIP84(seed, seedPassphrase, params);

    // debug whirlpoolWalletConfig
    if (log.isDebugEnabled()) {
      log.debug("openWallet with whirlpoolWalletConfig:");
      for (Map.Entry<String, String> entry : config.getConfigInfo().entrySet()) {
        log.debug("[whirlpoolWalletConfig/" + entry.getKey() + "] " + entry.getValue());
      }
      log.debug("walletIdentifier: " + walletIdentifier);
    }

    Tx0Service tx0Service = new Tx0Service(config);
    Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

    WalletDataSupplier walletDataSupplier =
        computeWalletDataSupplier(computeUtxoChangesListener(), config, bip84w, walletIdentifier);

    return new WhirlpoolWallet(
        walletIdentifier, config, tx0Service, bech32Util, walletDataSupplier);
  }

  // overridable for android
  protected WalletDataSupplier computeWalletDataSupplier(
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      WhirlpoolWalletConfig config,
      HD_Wallet bip84w,
      String walletIdentifier)
      throws Exception {
    return new BackendWalletDataSupplier(
        config.getRefreshUtxoDelay(), utxoChangesListener, config, bip84w, walletIdentifier);
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
