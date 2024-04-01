package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.BipWalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.BasicChainSupplier;
import com.samourai.whirlpool.client.wallet.data.chain.ChainData;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.BasicMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource based on WalletResponse. */
public abstract class WalletResponseDataSource extends AbstractDataSource {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseDataSource.class);

  private AbstractOrchestrator dataOrchestrator;

  private final WalletResponseSupplier walletResponseSupplier;
  private final BasicUtxoSupplier utxoSupplier;

  public WalletResponseDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      BipWalletSupplier bipWalletSupplier)
      throws Exception {
    super(
        whirlpoolWallet,
        bip44w,
        walletStateSupplier,
        computeDataSourceConfig(whirlpoolWallet, bipWalletSupplier));
    this.walletResponseSupplier = new WalletResponseSupplier(whirlpoolWallet, this);
    this.utxoSupplier =
        computeUtxoSupplier(walletSupplier, utxoConfigSupplier, getDataSourceConfig());
  }

  protected static DataSourceConfig computeDataSourceConfig(
      WhirlpoolWallet whirlpoolWallet, BipWalletSupplier bipWalletSupplier) throws Exception {
    return new DataSourceConfig(
        computeMinerFeeSupplier(whirlpoolWallet),
        computeChainSupplier(),
        computeBipFormatSupplier(),
        bipWalletSupplier);
  }

  protected static BasicMinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    BasicMinerFeeSupplier minerFeeSupplier =
        new BasicMinerFeeSupplier(config.getFeeMin(), config.getFeeMax(), config.getFeeFallback());
    return minerFeeSupplier;
  }

  protected static BasicChainSupplier computeChainSupplier() throws Exception {
    return new BasicChainSupplier();
  }

  protected static BipFormatSupplier computeBipFormatSupplier() {
    return BIP_FORMAT.PROVIDER;
  }

  protected BasicUtxoSupplier computeUtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      DataSourceConfig dataSourceConfig)
      throws Exception {
    NetworkParameters params = whirlpoolWallet.getConfig().getSamouraiNetwork().getParams();
    return new BasicUtxoSupplier(walletSupplier, utxoConfigSupplier, dataSourceConfig, params) {
      @Override
      public void refresh() throws Exception {
        WalletResponseDataSource.this.refresh();
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
      ((BasicMinerFeeSupplier) dataSourceConfig.getMinerFeeSupplier())
          .setValue(new MinerFee(walletResponse.info.fees));
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
    ((BasicChainSupplier) getDataSourceConfig().getChainSupplier())
        .setValue(new ChainData(walletResponse.info.latest_block));

    // update utxoSupplier
    if (walletResponse == null
        || walletResponse.unspent_outputs == null
        || walletResponse.txs == null) {
      throw new Exception("Invalid walletResponse.unspent_outputs/txs");
    }
    UtxoData utxoData =
        new UtxoData(
            walletResponse.unspent_outputs,
            walletResponse.txs,
            walletResponse.info.latest_block.height);
    utxoSupplier.setValue(utxoData);

    // update walletStateSupplier
    setWalletStateValue(walletResponse.getAddressesMap());
  }

  private void setWalletStateValue(Map<String, WalletResponse.Address> addressesMap) {
    // update indexs from wallet backend
    for (String pub : addressesMap.keySet()) {
      WalletResponse.Address address = addressesMap.get(pub);
      BipWallet bipWallet = walletSupplier.getWalletByXPub(pub);
      if (bipWallet != null) {
        bipWallet.getIndexHandlerReceive().set(address.account_index, false);
        bipWallet.getIndexHandlerChange().set(address.change_index, false);
      } else {
        log.error("No BipWallet found for: " + ClientUtils.maskString(pub));
      }
    }
  }

  protected void load(boolean initial) throws Exception {
    // load coordinators + paynym
    super.load(initial);

    // load data
    walletResponseSupplier.load();
  }

  @Override
  public void open(CoordinatorSupplier coordinatorSupplier) throws Exception {
    super.open(coordinatorSupplier);

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

  @Override
  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  protected WalletResponseSupplier getWalletResponseSupplier() {
    return walletResponseSupplier;
  }
}
