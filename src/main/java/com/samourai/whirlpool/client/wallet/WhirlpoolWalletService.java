package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
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
  private HD_WalletFactoryGeneric hdWalletFactory;
  private WhirlpoolWallet whirlpoolWallet; // or null

  public WhirlpoolWalletService(
      DataPersisterFactory dataPersisterFactory, DataSourceFactory dataSourceFactory) {
    this(dataPersisterFactory, dataSourceFactory, HD_WalletFactoryGeneric.getInstance());
  }

  public WhirlpoolWalletService(
      DataPersisterFactory dataPersisterFactory,
      DataSourceFactory dataSourceFactory,
      HD_WalletFactoryGeneric hdWalletFactory) {
    this.dataSourceFactory = dataSourceFactory;
    this.dataPersisterFactory = dataPersisterFactory;
    this.hdWalletFactory = hdWalletFactory;
    this.whirlpoolWallet = null;

    // set user-agent
    ClientUtils.setupEnv();
  }

  public synchronized void closeWallet() {
    if (whirlpoolWallet != null) {
      if (log.isDebugEnabled()) {
        log.debug("Closing wallet");
      }
      WhirlpoolWallet wp = whirlpoolWallet;
      wp.stop();
      wp.close();
      whirlpoolWallet = null;

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
    if (whirlpoolWallet != null) {
      throw new Exception("WhirlpoolWallet already opened");
    }

    wp.open(); // load initial data
    whirlpoolWallet = wp;

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
    HD_Wallet bip44w = hdWalletFactory.getBIP44(seed, seedPassphrase, params);

    // debug whirlpoolWalletConfig
    if (log.isDebugEnabled()) {
      log.debug("openWallet with whirlpoolWalletConfig:");
      for (Map.Entry<String, String> entry : config.getConfigInfo().entrySet()) {
        log.debug("[whirlpoolWalletConfig/" + entry.getKey() + "] " + entry.getValue());
      }
      log.debug("[walletIdentifier] " + walletIdentifier);
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
    return Optional.of(whirlpoolWallet);
  }

  public WhirlpoolWallet getWhirlpoolWalletOrNull() {
    return whirlpoolWallet;
  }
}
