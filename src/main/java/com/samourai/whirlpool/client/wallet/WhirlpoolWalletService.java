package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.event.WalletCloseEvent;
import com.samourai.whirlpool.client.event.WalletOpenEvent;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
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

      // notify close
      WhirlpoolEventService.getInstance().post(new WalletCloseEvent(wp));
    } else {
      if (log.isDebugEnabled()) {
        log.debug("closeWallet skipped: no wallet opened");
      }
    }
  }

  public WhirlpoolWallet openWallet(
      String walletIdentifier,
      WhirlpoolWalletConfig config,
      HD_Wallet bip44w,
      String walletStateFileName,
      String utxoConfigFileName)
      throws Exception {
    WhirlpoolWallet wp =
        computeWhirlpoolWallet(
            walletIdentifier, config, bip44w, walletStateFileName, utxoConfigFileName);
    return openWallet(wp);
  }

  protected synchronized WhirlpoolWallet openWallet(WhirlpoolWallet wp) throws Exception {
    if (whirlpoolWallet.isPresent()) {
      throw new Exception("WhirlpoolWallet already opened");
    }

    wp.open(); // load initial data
    whirlpoolWallet = Optional.of(wp);

    BipWalletAndAddressType depositWallet = wp.getWalletDeposit();
    BipWalletAndAddressType premixWallet = wp.getWalletPremix();
    BipWalletAndAddressType postmixWallet = wp.getWalletPostmix();

    // log zpubs
    if (log.isDebugEnabled()) {
      log.debug(
          "Deposit wallet: accountIndex="
              + depositWallet.getAccount().getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(depositWallet.getPub()));
      log.debug(
          "Premix wallet: accountIndex="
              + premixWallet.getAccount().getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(premixWallet.getPub()));
      log.debug(
          "Postmix wallet: accountIndex="
              + postmixWallet.getAccount().getAccountIndex()
              + ", zpub="
              + ClientUtils.maskString(postmixWallet.getPub()));
    }

    // notify open
    WhirlpoolEventService.getInstance().post(new WalletOpenEvent(wp));
    return wp;
  }

  public String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  protected WhirlpoolWallet computeWhirlpoolWallet(
      String walletIdentifier,
      WhirlpoolWalletConfig config,
      HD_Wallet bip44w,
      String walletStateFileName,
      String utxoConfigFileName)
      throws Exception {
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

    // verify config
    config.verify();

    Tx0Service tx0Service = new Tx0Service(config);
    NetworkParameters params = config.getNetworkParameters();
    Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
    WalletAggregateService walletAggregateService = new WalletAggregateService(params, bech32Util);

    int externalIndexDefault =
        config.getExternalDestination() != null
            ? config.getExternalDestination().getStartIndex()
            : 0;
    WalletSupplier walletSupplier =
        new WalletSupplier(
            new WalletStatePersister(walletStateFileName),
            config.getBackendApi(),
            bip44w,
            externalIndexDefault);

    PoolSupplier poolSupplier =
        new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi());

    WalletDataSupplier walletDataSupplier =
        computeWalletDataSupplier(walletSupplier, poolSupplier, utxoConfigFileName, config);

    return new WhirlpoolWallet(
        walletIdentifier,
        config,
        walletDataSupplier.getTx0ParamService(),
        tx0Service,
        walletAggregateService,
        bech32Util,
        walletSupplier,
        poolSupplier,
        walletDataSupplier);
  }

  // overridable for android
  protected WalletDataSupplier computeWalletDataSupplier(
      WalletSupplier walletSupplier,
      PoolSupplier poolSupplier,
      String utxoConfigFileName,
      WhirlpoolWalletConfig config)
      throws Exception {
    return new WalletDataSupplier(
        config.getRefreshUtxoDelay(), walletSupplier, poolSupplier, utxoConfigFileName, config);
  }

  public Optional<WhirlpoolWallet> getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
