package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.event.DataSourceExpireRequest;
import com.samourai.whirlpool.client.event.UtxoSupplierExpireRequest;
import com.samourai.whirlpool.client.event.UtxoSupplierRefreshRequest;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.BasicChainSupplier;
import com.samourai.whirlpool.client.wallet.data.chain.ChainData;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.minerFee.BasicMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplierImpl;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource based on WalletResponse. */
public abstract class WalletResponseDataSource implements DataSource {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseDataSource.class);

  private AbstractOrchestrator dataOrchestrator;

  private final WhirlpoolWalletConfig config;
  private final DataPersister dataPersister;
  private final WalletResponseSupplier walletResponseSupplier;

  private final WalletSupplierImpl walletSupplier;
  private final BasicMinerFeeSupplier minerFeeSupplier;
  protected final Tx0ParamService tx0ParamService;
  protected final ExpirablePoolSupplier poolSupplier;
  private final BasicChainSupplier chainSupplier;
  private final BasicUtxoSupplier utxoSupplier;

  public WalletResponseDataSource(
      WhirlpoolWalletConfig config,
      HD_Wallet bip44w,
      String walletIdentifier,
      DataPersister dataPersister)
      throws Exception {
    this.config = config;
    this.dataPersister = dataPersister;
    this.walletResponseSupplier = new WalletResponseSupplier(config, this);

    this.walletSupplier = computeWalletSupplier(bip44w, dataPersister.getWalletStateSupplier());
    this.minerFeeSupplier = computeMinerFeeSupplier(config);
    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);
    this.poolSupplier = computePoolSupplier(config, tx0ParamService);
    this.chainSupplier = computeChainSupplier();
    this.utxoSupplier =
        computeUtxoSupplier(
            walletSupplier,
            dataPersister.getUtxoConfigSupplier(),
            chainSupplier,
            poolSupplier,
            tx0ParamService);
  }

  protected WalletSupplierImpl computeWalletSupplier(
      HD_Wallet bip44w, WalletStateSupplier walletStateSupplier) throws Exception {
    return new WalletSupplierImpl(bip44w, walletStateSupplier);
  }

  protected BasicMinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWalletConfig config)
      throws Exception {
    return new BasicMinerFeeSupplier(
        config.getFeeMin(), config.getFeeMax(), config.getFeeFallback());
  }

  protected ExpirablePoolSupplier computePoolSupplier(
      WhirlpoolWalletConfig config, Tx0ParamService tx0ParamService) throws Exception {
    return new ExpirablePoolSupplier(
        config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
  }

  protected BasicChainSupplier computeChainSupplier() throws Exception {
    return new BasicChainSupplier();
  }

  protected BasicUtxoSupplier computeUtxoSupplier(
      WalletSupplierImpl walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      ChainSupplier chainSupplier,
      PoolSupplier poolSupplier,
      Tx0ParamService tx0ParamService)
      throws Exception {
    return new BasicUtxoSupplier(
        walletSupplier,
        utxoConfigSupplier,
        chainSupplier,
        poolSupplier,
        tx0ParamService,
        config.getNetworkParameters());
  }

  @Subscribe
  public void onUtxoSupplierExpireRequest(UtxoSupplierExpireRequest request) {
    walletResponseSupplier.expire();
  }

  @Subscribe
  public void onUtxoSupplierRefreshRequest(UtxoSupplierRefreshRequest request) throws Exception {
    walletResponseSupplier.refresh();
  }

  @Subscribe
  public void onDataSourceExpireRequest(DataSourceExpireRequest request) throws Exception {
    walletResponseSupplier.refresh();
  }

  protected abstract WalletResponse fetchWalletResponse() throws Exception;

  protected void setValue(WalletResponse walletResponse) throws Exception {
    // update minerFeeSupplier
    try {
      if (walletResponse == null
          || walletResponse.info == null
          || walletResponse.info.fees == null) {
        throw new Exception("Invalid walletResponse.info.fees");
      }
      minerFeeSupplier.setValue(new MinerFee(walletResponse.info.fees));
    } catch (Exception e) {
      // keep previous fee value as fallback
      log.error("minerFeeSupplier.setValue failed", e);
    }

    // update chainSupplier (before utxoSupplier)
    if (walletResponse == null
        || walletResponse.info == null
        || walletResponse.info.latest_block == null) {
      throw new Exception("Invalid walletResponse.info.latest_block");
    }
    chainSupplier.setValue(new ChainData(walletResponse.info.latest_block));

    // update utxoSupplier
    if (walletResponse == null
        || walletResponse.unspent_outputs == null
        || walletResponse.txs == null) {
      throw new Exception("Invalid walletResponse.unspent_outputs/txs");
    }
    UtxoData utxoData = new UtxoData(walletResponse.unspent_outputs, walletResponse.txs);
    utxoSupplier.setValue(utxoData);

    // update walletStateSupplier
    setWalletStateValue(walletResponse.getAddressesMap());
  }

  private void setWalletStateValue(Map<String, WalletResponse.Address> addressesMap)
      throws Exception {
    // update indexs from wallet backend
    WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
    for (String pub : addressesMap.keySet()) {
      WalletResponse.Address address = addressesMap.get(pub);
      BipWalletAndAddressType bipWallet = walletSupplier.getWalletByPub(pub);
      if (bipWallet != null) {
        walletStateSupplier.setWalletIndex(
            bipWallet.getAccount(),
            bipWallet.getAddressType(),
            Chain.RECEIVE,
            address.account_index);
        walletStateSupplier.setWalletIndex(
            bipWallet.getAccount(), bipWallet.getAddressType(), Chain.CHANGE, address.change_index);
      } else {
        log.error("No wallet found for: " + pub);
      }
    }
  }

  protected void load(boolean initial) throws Exception {
    // load pools
    poolSupplier.load();

    // load data
    walletResponseSupplier.load();
  }

  @Override
  public void open() throws Exception {
    WhirlpoolEventService.getInstance().register(this);

    // load initial data (or fail)
    load(true);

    // data orchestrator
    runDataOrchestrator();
  }

  protected void runDataOrchestrator() {
    int dataOrchestratorDelay =
        NumberUtils.min(config.getRefreshUtxoDelay(), config.getRefreshPoolsDelay());
    dataOrchestrator =
        new AbstractOrchestrator(dataOrchestratorDelay * 1000) {
          @Override
          protected void runOrchestrator() {
            if (log.isDebugEnabled()) {
              log.debug("Refreshing data...");
            }
            try {
              load(false);
            } catch (Exception e) {
              log.error("", e);
            }
          }
        };
    dataOrchestrator.start(true);
  }

  @Override
  public void close() throws Exception {
    WhirlpoolEventService.getInstance().unregister(this);
    dataOrchestrator.stop();
  }

  protected WhirlpoolWalletConfig getConfig() {
    return config;
  }

  protected DataPersister getDataPersister() {
    return dataPersister;
  }

  @Override
  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  @Override
  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  @Override
  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }

  @Override
  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  @Override
  public ChainSupplier getChainSupplier() {
    return chainSupplier;
  }

  @Override
  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }
}
