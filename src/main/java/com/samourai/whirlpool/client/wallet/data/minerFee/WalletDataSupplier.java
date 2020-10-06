package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersister;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletDataSupplier extends AbstractSupplier<WalletResponse> {
  private static final Logger log = LoggerFactory.getLogger(WalletDataSupplier.class);

  private final BackendApi backendApi;
  private final WalletSupplier walletSupplier;

  private final MinerFeeSupplier minerFeeSupplier;
  private final UtxoSupplier utxoSupplier;
  private final PoolSupplier poolSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final Tx0ParamService tx0ParamService;

  public WalletDataSupplier(
      int refreshUtxoDelay,
      WalletSupplier walletSupplier,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      String utxoConfigFileName,
      WhirlpoolWalletConfig config) {
    super(refreshUtxoDelay, null, log);
    this.backendApi = config.getBackendApi();
    this.walletSupplier = walletSupplier;

    this.minerFeeSupplier =
        new MinerFeeSupplier(config.getFeeMin(), config.getFeeMax(), config.getFeeFallback()) {
          @Override
          protected MinerFee fetch() throws Exception {
            // already fetch by walletDataSupplier
            throw new Exception("Not supported");
          }

          @Override
          public void expire() {
            WalletDataSupplier.this.expire();
          }
        };

    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);

    this.poolSupplier = new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi());
    this.utxoConfigSupplier =
        new UtxoConfigSupplier(
            new UtxoConfigPersister(utxoConfigFileName), poolSupplier, tx0ParamService);

    this.utxoSupplier =
        new UtxoSupplier(walletSupplier, utxoConfigSupplier, utxoChangesListener) {
          @Override
          protected UtxoData fetch() throws Exception {
            // already fetch by walletDataSupplier
            throw new Exception("Not supported");
          }

          @Override
          public void expire() {
            WalletDataSupplier.this.expire();
          }
        };
  }

  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] utxoZpubs = walletSupplier.getZpubs(false);
    return backendApi.fetchWallet(utxoZpubs);
  }

  @Override
  protected WalletResponse fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    WalletResponse walletResponse = fetchWalletResponse();

    // update minerFeeSupplier
    minerFeeSupplier._setValue(walletResponse);

    // update utxoSupplier
    utxoSupplier._setValue(walletResponse);

    return walletResponse;
  }

  // accessed by sub-suppliers
  @Override
  public WalletResponse getValue() {
    return super.getValue();
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }
}
