package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxosRequestEvent;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.LoadableSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersister;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WalletDataSupplier extends ExpirableSupplier<WalletResponse>
    implements LoadableSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletDataSupplier.class);

  private final WhirlpoolWalletConfig config;
  private final WalletSupplier walletSupplier;
  private final MinerFeeSupplier minerFeeSupplier;
  protected final Tx0ParamService tx0ParamService;
  protected final PoolSupplier poolSupplier;
  private final WalletStateSupplier walletStateSupplier;
  private final ChainSupplier chainSupplier;

  private final UtxoSupplier utxoSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;

  public WalletDataSupplier(
      int refreshUtxoDelay, WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier)
      throws Exception {
    super(refreshUtxoDelay, null, log);
    this.config = config;
    this.walletSupplier = computeWalletSupplier(config, bip44w, walletIdentifier);
    this.walletStateSupplier = walletSupplier.getWalletStateSupplier();

    this.minerFeeSupplier = computeMinerFeeSupplier(config);
    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);
    this.poolSupplier = computePoolSupplier(config, tx0ParamService);

    this.chainSupplier = new ChainSupplier();

    this.utxoConfigSupplier =
        computeUtxoConfigSupplier(poolSupplier, tx0ParamService, walletIdentifier);
    this.utxoSupplier = computeUtxoSupplier(walletSupplier, utxoConfigSupplier);

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

  protected WalletSupplier computeWalletSupplier(
      WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier) throws Exception {
    int externalIndexDefault =
        config.getExternalDestination() != null
            ? config.getExternalDestination().getStartIndex()
            : 0;
    String walletStateFileName = computeIndexFile(walletIdentifier).getAbsolutePath();
    return new WalletSupplier(
        new WalletStatePersister(walletStateFileName),
        config.getBackendApi(),
        bip44w,
        externalIndexDefault);
  }

  protected MinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWalletConfig config) {
    return new MinerFeeSupplier(config.getFeeMin(), config.getFeeMax(), config.getFeeFallback());
  }

  protected PoolSupplier computePoolSupplier(
      WhirlpoolWalletConfig config, Tx0ParamService tx0ParamService) {
    return new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
  }

  protected UtxoConfigPersister computeUtxoConfigPersister(String walletIdentifier)
      throws Exception {
    String utxoConfigFileName = computeUtxosFile(walletIdentifier).getAbsolutePath();
    return new UtxoConfigPersister(utxoConfigFileName);
  }

  protected UtxoConfigSupplier computeUtxoConfigSupplier(
      PoolSupplier poolSupplier, Tx0ParamService tx0ParamService, String walletIdentifier)
      throws Exception {
    UtxoConfigPersister utxoConfigPersister = computeUtxoConfigPersister(walletIdentifier);
    return new UtxoConfigSupplier(utxoConfigPersister, poolSupplier, tx0ParamService);
  }

  protected UtxoSupplier computeUtxoSupplier(
      WalletSupplier walletSupplier, UtxoConfigSupplier utxoConfigSupplier) {
    return new UtxoSupplier(
        walletSupplier, utxoConfigSupplier, this, config.getNetworkParameters());
  }

  protected File computeIndexFile(String walletIdentifier) throws NotifiableException {
    String path = "whirlpool-cli-state-" + walletIdentifier + ".json";
    return ClientUtils.computeFile(path);
  }

  protected File computeUtxosFile(String walletIdentifier) throws NotifiableException {
    String path = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
    return ClientUtils.computeFile(path);
  }

  protected abstract WalletResponse fetchWalletResponse() throws Exception;

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

  protected WhirlpoolWalletConfig getConfig() {
    return config;
  }

  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
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
}
