package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxosRequestEvent;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.LoadableSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersister;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletDataSupplier extends ExpirableSupplier<WalletResponse>
    implements LoadableSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletDataSupplier.class);

  private final WhirlpoolWalletConfig config;
  private final WalletSupplier walletSupplier;
  private final WalletStateSupplier walletStateSupplier;

  private final MinerFeeSupplier minerFeeSupplier;
  private final ChainSupplier chainSupplier;
  private final UtxoSupplier utxoSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final Tx0ParamService tx0ParamService;

  public WalletDataSupplier(
      int refreshUtxoDelay,
      WalletSupplier walletSupplier,
      PoolSupplier poolSupplier,
      String utxoConfigFileName,
      WhirlpoolWalletConfig config)
      throws Exception {
    super(refreshUtxoDelay, null, log);
    this.config = config;
    this.walletSupplier = walletSupplier;
    this.walletStateSupplier = walletSupplier.getWalletStateSupplier();

    this.minerFeeSupplier =
        new MinerFeeSupplier(config.getFeeMin(), config.getFeeMax(), config.getFeeFallback());

    this.chainSupplier = new ChainSupplier();

    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);

    this.utxoConfigSupplier =
        new UtxoConfigSupplier(
            new UtxoConfigPersister(utxoConfigFileName), poolSupplier, tx0ParamService);

    this.utxoSupplier =
        new UtxoSupplier(walletSupplier, utxoConfigSupplier, this, config.getNetworkParameters());

    if (config.isBackendWatch() && config.getBackendWsApi() != null) {
      this.watchBackend();
    }
  }

  private void watchBackend() throws Exception {
    final BackendWsApi backendWsApi = config.getBackendWsApi();
    backendWsApi.connect(
        new MessageListener<Void>() {
          @Override
          public void onMessage(Void foo) {
            try {
              // watch blocks
              backendWsApi.subscribeBlock(
                  new MessageListener() {
                    @Override
                    public void onMessage(Object message) {
                      if (log.isDebugEnabled()) {
                        log.debug("new block received -> refreshing walletData");
                        try {
                          expireAndReload();
                        } catch (Exception e) {
                          log.error("", e);
                        }
                      }
                    }
                  });

              // watch addresses
              String[] pubs = walletSupplier.getPubs(true);
              backendWsApi.subscribeAddress(
                  pubs,
                  new MessageListener() {
                    @Override
                    public void onMessage(Object message) {
                      if (log.isDebugEnabled()) {
                        log.debug("new address received -> refreshing walletData");
                        try {
                          expireAndReload();
                        } catch (Exception e) {
                          log.error("", e);
                        }
                      }
                    }
                  });
            } catch (Exception e) {
              log.error("", e);
            }
          }
        },
        true);
  }

  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] pubs = walletSupplier.getPubs(true);
    return config.getBackendApi().fetchWallet(pubs);
  }

  @Override
  protected WalletResponse fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    WhirlpoolEventService.getInstance().post(new UtxosRequestEvent());
    WalletResponse walletResponse = fetchWalletResponse();

    // update minerFeeSupplier
    try {
      minerFeeSupplier._setValue(walletResponse);
    } catch (Exception e) {
      log.error("minerFeeSupplier._setValue failed => using fallback value", e);
    }

    // update chainSupplier
    chainSupplier._setValue(walletResponse);

    // update utxoSupplier
    utxoSupplier._setValue(walletResponse);

    // update walletStateSupplier
    walletStateSupplier._setValue(walletResponse);

    return walletResponse;
  }

  @Override
  public void stop() {
    super.stop();

    // disconnect backend websocket
    if (config.isBackendWatch() && config.getBackendWsApi() != null) {
      config.getBackendWsApi().disconnect();
    }
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public ChainSupplier getChainSupplier() {
    return chainSupplier;
  }

  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }

  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }
}
