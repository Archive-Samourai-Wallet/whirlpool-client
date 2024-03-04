package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.api.backend.seenBackend.SeenBackendWithFallback;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.BipWalletSupplier;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataSource for Samourai/Dojo backend. */
public class DojoDataSource extends WalletResponseDataSource {
  private static final Logger log = LoggerFactory.getLogger(DojoDataSource.class);

  private static final int INITWALLET_RETRY = 3;
  private static final int INITWALLET_RETRY_TIMEOUT = 3000;
  private static final int FETCH_TXS_PER_PAGE = 200;

  private BackendApi backendApi;
  private BackendWsApi backendWsApi; // may be null
  private ISeenBackend seenBackend;

  public DojoDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      BipWalletSupplier bipWalletSupplier,
      BackendApi backendApi,
      BackendWsApi backendWsApi)
      throws Exception {
    super(whirlpoolWallet, bip44w, walletStateSupplier, utxoConfigSupplier, bipWalletSupplier);

    this.backendApi = backendApi;
    this.backendWsApi = backendWsApi;
    NetworkParameters params = whirlpoolWallet.getConfig().getSamouraiNetwork().getParams();
    this.seenBackend = SeenBackendWithFallback.withOxt(backendApi, params);
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
    Collection<WhirlpoolUtxo> postmixUtxos = getUtxoSupplier().findUtxos(SamouraiAccount.POSTMIX);
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
    String[] zpubs =
        new String[] {
          getWhirlpoolWallet().getWalletPremix().getXPub(),
          getWhirlpoolWallet().getWalletPostmix().getXPub()
        };

    Map<String, TxsResponse.Tx> txs = new LinkedHashMap<String, TxsResponse.Tx>();
    int page = -1;
    TxsResponse txsResponse;
    do {
      page++;
      txsResponse = backendApi.fetchTxs(zpubs, page, FETCH_TXS_PER_PAGE);
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
      log.info(" • Initializing wallet: " + ClientUtils.maskString(xpub));
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
  public void open(CoordinatorSupplier coordinatorSupplier) throws Exception {
    super.open(coordinatorSupplier);

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
                String[] pubs = getWalletSupplier().getXPubs(true);
                backendWsApi.subscribeAddress(
                    pubs,
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
  public ISweepBackend getSweepBackend() {
    return backendApi;
  }

  @Override
  public ISeenBackend getSeenBackend() {
    return seenBackend;
  }

  public BackendApi getBackendApi() {
    return backendApi;
  }

  public BackendWsApi getBackendWsApi() {
    return backendWsApi;
  }
}
