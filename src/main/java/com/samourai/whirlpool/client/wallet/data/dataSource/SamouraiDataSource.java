package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource for Samourai/Dojo backend. */
public class SamouraiDataSource extends WalletResponseDataSource {
  private static final Logger log = LoggerFactory.getLogger(SamouraiDataSource.class);

  private static final int INITWALLET_RETRY = 3;
  private static final int INITWALLET_RETRY_TIMEOUT = 3000;

  private BackendApi backendApi;
  private BackendWsApi backendWsApi; // may be null

  public SamouraiDataSource(
      WhirlpoolWalletConfig config,
      HD_Wallet bip44w,
      String walletIdentifier,
      DataPersister dataPersister,
      BackendApi backendApi,
      BackendWsApi backendWsApi)
      throws Exception {
    super(config, bip44w, walletIdentifier, dataPersister);

    this.backendApi = backendApi;
    this.backendWsApi = backendWsApi;
  }

  @Override
  protected void load(boolean initial) throws Exception {
    super.load(initial);

    if (initial) {
      WalletStateSupplier walletStateSupplier = getDataPersister().getWalletStateSupplier();
      boolean isInitialized = walletStateSupplier.isInitialized();

      // initialize wallets
      if (!isInitialized) {
        String[] activePubs = getWalletSupplier().getPubs(true);
        for (String pub : activePubs) {
          initWallet(pub);
        }
        walletStateSupplier.setInitialized(true);

        // when wallet is not initialized, counters are not synced
        walletStateSupplier.setSynced(false);
      }
    }
  }

  private void initWallet(String pub) throws Exception {
    for (int i = 0; i < INITWALLET_RETRY; i++) {
      log.info(" • Initializing wallet");
      try {
        backendApi.initBip84(pub);
        return; // success
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("", e);
        }
        log.error(
            " x Initializing wallet failed, retrying... ("
                + (i + 1)
                + "/"
                + INITWALLET_RETRY
                + ")");
        Thread.sleep(INITWALLET_RETRY_TIMEOUT);
      }
    }
    throw new NotifiableException("Unable to initialize Bip84 wallet");
  }

  @Override
  public void open() throws Exception {
    super.open();

    if (backendWsApi != null) {
      this.startBackendWsApi();
    }
  }

  protected void startBackendWsApi() throws Exception {
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
                          refresh();
                        } catch (Exception e) {
                          log.error("", e);
                        }
                      }
                    }
                  });

              // watch addresses
              String[] pubs = getWalletSupplier().getPubs(true);
              backendWsApi.subscribeAddress(
                  pubs,
                  new MessageListener() {
                    @Override
                    public void onMessage(Object message) {
                      if (log.isDebugEnabled()) {
                        log.debug("new address received -> refreshing walletData");
                        try {
                          refresh();
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

  @Override
  public void close() throws Exception {
    super.close();

    // disconnect backend websocket
    if (backendWsApi != null) {
      backendWsApi.disconnect();
    }
  }

  @Override
  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] pubs = getWalletSupplier().getPubs(true);
    return backendApi.fetchWallet(pubs);
  }

  @Override
  public TxsResponse fetchTxs(String[] zpubs, int page, int count) throws Exception {
    return backendApi.fetchTxs(zpubs, page, count);
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    backendApi.pushTx(txHex);
  }

  public BackendApi getBackendApi() {
    return backendApi;
  }

  public BackendWsApi getBackendWsApi() {
    return backendWsApi;
  }
}
