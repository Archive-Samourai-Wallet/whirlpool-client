package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipFormat.BipFormatSupplierImpl;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
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
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource based on WalletResponse. */
public abstract class WalletResponseDataSource implements DataSource {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseDataSource.class);

  private AbstractOrchestrator dataOrchestrator;

  private final WhirlpoolWallet whirlpoolWallet;
  private final DataPersister dataPersister;
  private final WalletResponseSupplier walletResponseSupplier;

  private final WalletSupplier walletSupplier;
  private final BasicMinerFeeSupplier minerFeeSupplier;
  protected final Tx0PreviewService tx0PreviewService;
  protected final ExpirablePoolSupplier poolSupplier;
  private final BasicChainSupplier chainSupplier;
  private final BasicUtxoSupplier utxoSupplier;
  private final BipFormatSupplier bipFormatSupplier;

  public WalletResponseDataSource(
      WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister)
      throws Exception {
    this.whirlpoolWallet = whirlpoolWallet;
    this.dataPersister = dataPersister;
    this.walletResponseSupplier = new WalletResponseSupplier(whirlpoolWallet, this);

    this.walletSupplier =
        computeWalletSupplier(whirlpoolWallet, bip44w, dataPersister.getWalletStateSupplier());
    this.minerFeeSupplier = computeMinerFeeSupplier(whirlpoolWallet);
    this.tx0PreviewService = new Tx0PreviewService(minerFeeSupplier, whirlpoolWallet.getConfig());
    this.poolSupplier = computePoolSupplier(whirlpoolWallet, tx0PreviewService);
    this.chainSupplier = computeChainSupplier();
    this.bipFormatSupplier = computeBipFormatSupplier();
    this.utxoSupplier =
        computeUtxoSupplier(
            whirlpoolWallet,
            walletSupplier,
            dataPersister.getUtxoConfigSupplier(),
            chainSupplier,
            poolSupplier,
            bipFormatSupplier);
  }

  protected WalletSupplierImpl computeWalletSupplier(
      WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, WalletStateSupplier walletStateSupplier)
      throws Exception {
    return new WalletSupplierImpl(walletStateSupplier, bip44w);
  }

  protected BasicMinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    BasicMinerFeeSupplier minerFeeSupplier =
        new BasicMinerFeeSupplier(config.getFeeMin(), config.getFeeMax());
    minerFeeSupplier.setValue(config.getFeeFallback());
    return minerFeeSupplier;
  }

  protected ExpirablePoolSupplier computePoolSupplier(
      WhirlpoolWallet whirlpoolWallet, Tx0PreviewService tx0PreviewService) throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    return new ExpirablePoolSupplier(
        config.getRefreshPoolsDelay(), config.getServerApi(), tx0PreviewService);
  }

  protected BasicChainSupplier computeChainSupplier() throws Exception {
    return new BasicChainSupplier();
  }

  protected BipFormatSupplier computeBipFormatSupplier() throws Exception {
    return new BipFormatSupplierImpl();
  }

  protected BasicUtxoSupplier computeUtxoSupplier(
      final WhirlpoolWallet whirlpoolWallet,
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      ChainSupplier chainSupplier,
      PoolSupplier poolSupplier,
      BipFormatSupplier bipFormatSupplier)
      throws Exception {
    return new BasicUtxoSupplier(
        walletSupplier,
        utxoConfigSupplier,
        chainSupplier,
        poolSupplier,
        bipFormatSupplier,
        whirlpoolWallet.getConfig().getNetworkParameters()) {
      @Override
      public void refresh() throws Exception {
        WalletResponseDataSource.this.refresh();
      }

      @Override
      protected void onUtxoChanges(UtxoData utxoData) {
        super.onUtxoChanges(utxoData);
        whirlpoolWallet.onUtxoChanges(utxoData);
      }
    };
  }

  public void refresh() throws Exception {
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

  private void setWalletStateValue(Map<String, WalletResponse.Address> addressesMap) {
    // update indexs from wallet backend
    WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
    for (String pub : addressesMap.keySet()) {
      WalletResponse.Address address = addressesMap.get(pub);
      BipWallet bipWallet = walletSupplier.getWalletByPub(pub);
      if (bipWallet != null) {
        bipWallet.getIndexHandlerReceive().set(address.account_index, false);
        bipWallet.getIndexHandlerChange().set(address.change_index, false);
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
    // load initial data (or fail)
    load(true);

    // data orchestrator
    runDataOrchestrator();
  }

  protected void runDataOrchestrator() {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
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
    dataOrchestrator.stop();
  }

  protected WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
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
  public Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
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

  protected WalletResponseSupplier getWalletResponseSupplier() {
    return walletResponseSupplier;
  }
}
