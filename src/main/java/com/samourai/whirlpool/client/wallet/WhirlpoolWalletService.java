package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.whirlpool.client.event.WalletCloseEvent;
import com.samourai.whirlpool.client.event.WalletOpenEvent;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import java.util.Map;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletService {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWalletService.class);

  private DataPersisterFactory dataPersisterFactory;
  private DataSourceFactory dataSourceFactory;
  private Optional<WhirlpoolWallet> whirlpoolWallet;

  public WhirlpoolWalletService(
      DataPersisterFactory dataPersisterFactory, DataSourceFactory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
    this.dataPersisterFactory = dataPersisterFactory;
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
    HD_Wallet bip44w = HD_WalletFactoryJava.getInstance().getBIP44(seed, seedPassphrase, params);

    // debug whirlpoolWalletConfig
    if (log.isDebugEnabled()) {
      log.debug("openWallet with whirlpoolWalletConfig:");
      for (Map.Entry<String, String> entry : config.getConfigInfo().entrySet()) {
        log.debug("[whirlpoolWalletConfig/" + entry.getKey() + "] " + entry.getValue());
      }
      log.debug("walletIdentifier: " + walletIdentifier);
    }

    // verify config
    config.verify();

    DataPersister dataPersister =
        dataPersisterFactory.createDataPersister(config, bip44w, walletIdentifier);
    DataSource dataSource =
        dataSourceFactory.createDataSource(config, bip44w, walletIdentifier, dataPersister);
    return new WhirlpoolWallet(walletIdentifier, config, dataSource, dataPersister);
  }

  public Optional<WhirlpoolWallet> getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
