package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource for Samourai/Dojo backend. */
public class DojoDataSource extends WalletResponseDataSource
    implements DataSourceWithStrictMode, DataSourceWithSweep {
  private static final Logger log = LoggerFactory.getLogger(DojoDataSource.class);

  private static final int INITWALLET_RETRY = 3;
  private static final int INITWALLET_RETRY_TIMEOUT = 3000;
  private static final int FETCH_TXS_PER_PAGE = 200;

  private BackendApi backendApi;
  private BackendWsApi backendWsApi; // may be null

  public DojoDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      BackendApi backendApi,
      BackendWsApi backendWsApi)
      throws Exception {
    super(whirlpoolWallet, bip44w, walletStateSupplier, utxoConfigSupplier);

    this.backendApi = backendApi;
    this.backendWsApi = backendWsApi;
  }

  @Override
  protected void load(boolean initial) throws Exception {
    boolean isInitialized = getWalletStateSupplier().isInitialized();

    // initialize wallet BEFORE loading
    if (initial && !isInitialized) {
      // initialize bip84 wallets on backend
      String[] activeXPubs = getWalletSupplier().getXPubs(true, BIP_FORMAT.SEGWIT_NATIVE);
      for (String xpub : activeXPubs) {
        initWallet(xpub);
      }
      getWalletStateSupplier().setInitialized(true);
    }

    // load
    super.load(initial);

    // resync postmix AFTER loading
    if (initial && !isInitialized) {
      if (getWhirlpoolWallet().getConfig().isResyncOnFirstRun()) {
        // resync postmix indexs
        resyncMixsDone();
      }
    }
  }

  public void resyncMixsDone() {
    // only resync if we have remixable utxos
    Collection<WhirlpoolUtxo> postmixUtxos = getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX);
    if (!filterRemixableUtxos(postmixUtxos).isEmpty()) {
      // there are remixable postmix utxos
      if (log.isDebugEnabled()) {
        log.debug("First run => resync mixsDone");
      }
      try {
        Map<String, TxsResponse.Tx> postmixTxs = fetchTxsPostmix();
        new MixsDoneResyncManager().resync(postmixUtxos, postmixTxs);
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }

  private Map<String, TxsResponse.Tx> fetchTxsPostmix() throws Exception {
    String[] xpubs =
        new String[] {
          getWhirlpoolWallet().getWalletPremix().getXPub(),
          getWhirlpoolWallet().getWalletPostmix().getXPub()
        };

    Map<String, TxsResponse.Tx> txs = new LinkedHashMap<String, TxsResponse.Tx>();
    int page = -1;
    TxsResponse txsResponse;
    do {
      page++;
      txsResponse = backendApi.fetchTxs(xpubs, page, FETCH_TXS_PER_PAGE);
      if (txsResponse == null) {
        log.warn("Resync aborted: fetchTxs() is not available");
        break;
      }

      if (txsResponse.txs != null) {
        for (TxsResponse.Tx tx : txsResponse.txs) {
          txs.put(tx.hash, tx);
        }
      }
      log.info("Resync: fetching postmix history... " + txs.size() + "/" + txsResponse.n_tx);
    } while ((page * FETCH_TXS_PER_PAGE) < txsResponse.n_tx);
    return txs;
  }

  private Collection<WhirlpoolUtxo> filterRemixableUtxos(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    return whirlpoolUtxos.stream()
        .filter(
            whirlpoolUtxo ->
                !MixableStatus.NO_POOL.equals(whirlpoolUtxo.getUtxoState().getMixableStatus()))
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  private void initWallet(String xpub) throws Exception {
    for (int i = 0; i < INITWALLET_RETRY; i++) {
      log.info(" • Initializing wallet");
      try {
        backendApi.initBip84(xpub);
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
        (MessageListener<Void>)
            foo -> {
              try {
                // watch blocks
                backendWsApi.subscribeBlock(
                    (MessageListener)
                        message -> {
                          if (log.isDebugEnabled()) {
                            log.debug("new block received -> refreshing walletData");
                            try {
                              refresh();
                            } catch (Exception e) {
                              log.error("", e);
                            }
                          }
                        });

                // watch addresses
                String[] xpubs = getWalletSupplier().getXPubs(true);
                backendWsApi.subscribeAddress(
                    xpubs,
                    (MessageListener)
                        message -> {
                          if (log.isDebugEnabled()) {
                            log.debug("new address received -> refreshing walletData");
                            try {
                              refresh();
                            } catch (Exception e) {
                              log.error("", e);
                            }
                          }
                        });
              } catch (Exception e) {
                log.error("", e);
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
    String[] pubs = getWalletSupplier().getXPubs(true);
    return backendApi.fetchWallet(pubs);
  }

  @Override
  public IPushTx getPushTx() {
    return backendApi;
  }

  @Override
  public String pushTx(String txHex, Collection<Integer> strictModeVouts) throws Exception {
    return backendApi.pushTx(txHex, strictModeVouts);
  }

  @Override
  public ISweepBackend getSweepBackend() {
    return backendApi;
  }

  public BackendApi getBackendApi() {
    return backendApi;
  }

  public BackendWsApi getBackendWsApi() {
    return backendWsApi;
  }
}
