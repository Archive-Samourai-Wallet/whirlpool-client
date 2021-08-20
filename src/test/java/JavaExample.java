import com.google.common.eventbus.Subscribe;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.websocket.client.IWebsocketClient;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.SamouraiDataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.WalletResponseDataSource;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java8.util.Lists;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;

public class JavaExample {
  // TODO configure these values as you wish
  private WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    IStompClientService stompClientService =
        null; // provide impl here, ie: AndroidStompClientService or
    // https://code.samourai.io/whirlpool/whirlpool-client-cli/-/blob/develop/src/main/java/com/samourai/stomp/client/JavaStompClient.java

    WhirlpoolServer whirlpoolServer = WhirlpoolServer.TESTNET;

    boolean onion = true; // use Tor onion services?
    String serverUrl = whirlpoolServer.getServerUrl(onion);
    IHttpClientService httpClientService = null; // provide impl here, ie: new AndroidHttpClient();
    ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
    TorClientService torClientService = null; // provide impl here

    NetworkParameters params = whirlpoolServer.getParams();
    boolean mobile = false; // true for mobile configuration, false for desktop/CLI
    WhirlpoolWalletConfig whirlpoolWalletConfig =
        new WhirlpoolWalletConfig(
            httpClientService, stompClientService, torClientService, serverApi, params, mobile);

    // configure optional settings (or don't set anything for using default values)
    whirlpoolWalletConfig.setScode("foo");
    return whirlpoolWalletConfig;
  }

  private DataPersisterFactory computeDataPersisterFactory() {
    return new DataPersisterFactory() {
      @Override
      public DataPersister createDataPersister(
          WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier)
          throws Exception {
        // use system files (or use your own implementation of DataPersister)
        return new FileDataPersister(config, bip44w, walletIdentifier);
      }
    };
  }

  // Example 1: get data from Samourai / Dojo backend
  private DataSourceFactory computeDataSourceFactorySamourai() {
    return new DataSourceFactory() {
      @Override
      public DataSource createDataSource(
          WhirlpoolWalletConfig config,
          HD_Wallet bip44w,
          String walletIdentifier,
          DataPersister dataPersister)
          throws Exception {
        // configure Samourai/Dojo backend
        boolean onion = true; // use Tor onion services?
        String backendUrl = BackendServer.TESTNET.getBackendUrl(onion);
        IHttpClient httpClientBackend = config.getHttpClient(HttpUsage.BACKEND);
        Optional<OAuthManager> oAuthManager =
            null; // NULL for Samourai backend, not NULL for Dojo, ie: new OAuthManagerJava()
        BackendApi backendApi = new BackendApi(httpClientBackend, backendUrl, oAuthManager);

        // enable real-time update from backend? (optional)
        IWebsocketClient wsClient = null; // provide impl here
        BackendWsApi backendWsApi =
            new BackendWsApi(
                wsClient,
                backendUrl,
                Optional.<OAuthManager>empty()); // or NULL to disable backend real-time sync

        return new SamouraiDataSource(
            config, bip44w, walletIdentifier, dataPersister, backendApi, backendWsApi);
      }
    };
  }

  // Example 2: get data from external backend
  private DataSourceFactory computeDataSourceFactoryExternal() {
    // note: when external data changed, use WalletResponseDataSource.refresh() to refresh it
    return new DataSourceFactory() {
      @Override
      public DataSource createDataSource(
          WhirlpoolWalletConfig config,
          HD_Wallet bip44w,
          String walletIdentifier,
          DataPersister dataPersister)
          throws Exception {
        // use WalletResponse data (or use your own implementation of DataSource)
        return new WalletResponseDataSource(config, bip44w, walletIdentifier, dataPersister) {
          @Override
          protected WalletResponse fetchWalletResponse() throws Exception {
            WalletResponse walletResponse = null; // provide data here
            return walletResponse;
          }

          @Override
          public void pushTx(String txHex) throws Exception {
            // provide pushTx service here
          }

          @Override
          public TxsResponse fetchTxs(String[] zpubs, int page, int count) throws Exception {
            TxsResponse txsResponse =
                null; // optionnal: provide data here or NULL (only used for postmix counters
            // resync)
            return null;
          }
        };
      };
    };
  }

  public void example() throws Exception {
    /*
     * CONFIGURATION
     */
    // configure whirlpool
    DataPersisterFactory dataPersisterFactory = computeDataPersisterFactory();
    DataSourceFactory dataSourceFactory =
        computeDataSourceFactorySamourai(); // or computeDataSourceFactoryExternal()
    WhirlpoolWalletService whirlpoolWalletService =
        new WhirlpoolWalletService(dataPersisterFactory, dataSourceFactory);
    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig();

    /*
     * WALLET
     */
    // open wallet: standard way
    byte[] seed = null; // provide seed here
    String seedPassphrase = null; // provide seed passphrase here (or null if none)
    WhirlpoolWallet whirlpoolWallet = whirlpoolWalletService.openWallet(config, seed, seedPassphrase);

    // open wallet: alternate way
    HD_Wallet bip44w = null; // provide bip44 wallet here
    NetworkParameters params = config.getNetworkParameters();
    whirlpoolWallet = whirlpoolWalletService.openWallet(config, bip44w);

    // start whirlpool wallet
    whirlpoolWallet.start();

    // get mixing state (started, utxosMixing, nbMixing, nbQueued...)
    MixingState mixingState = whirlpoolWallet.getMixingState();

    /*
     * POOLS
     */
    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // list pools
    Collection<Pool> pools = poolSupplier.getPools();

    // find pool by poolId
    Pool pool05btc = poolSupplier.findPoolById("0.5btc");

    /*
     * UTXOS
     */
    UtxoSupplier utxoSupplier = whirlpoolWallet.getUtxoSupplier();

    // list utxos
    Collection<WhirlpoolUtxo> utxosDeposit = utxoSupplier.findUtxos(WhirlpoolAccount.DEPOSIT);

    // get specific utxo
    WhirlpoolUtxo whirlpoolUtxo =
        utxoSupplier.findUtxo(
            "040df121854c7db49e38b6fcb61c2b0953c8b234ce53c1b2a2fb122a4e1c3d2e", 1);

    // get utxo state (status, mixStep, mixableStatus, progressPercent, message, error...)
    WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();

    // observe utxo state
    utxoState.getObservable().subscribe(/* ... */ );

    /*
     * TX0
     */
    // tx0 method 1: spending a whirlpool-managed utxo
    {
      // whirlpool utxo for tx0
      String utxoHash = "6517ece36402a89d76d075c60a8d3d0e051e4e5efa42a01c9033328707631b61";
      int utxoIndex = 2;
      whirlpoolUtxo = utxoSupplier.findUtxo(utxoHash, utxoIndex);
      if (whirlpoolUtxo == null) {} // utxo not found
      Collection<WhirlpoolUtxo> utxos = Lists.of(whirlpoolUtxo);

      // configure tx0
      Tx0Config tx0Config =
          whirlpoolWallet.getTx0Config().setChangeWallet(WhirlpoolAccount.BADBANK);
      Tx0FeeTarget tx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_4;

      // preview tx0
      try {
        Tx0Preview tx0Preview =
            whirlpoolWallet.tx0Preview(pool05btc, utxos, tx0Config, tx0FeeTarget, mixFeeTarget);
        long minerFee =
            tx0Preview
                .getTx0MinerFee(); // get minerFee, poolFee, premixValue, changeValue, nbPremix...
      } catch (Exception e) {
        // preview tx0 failed
      }

      // execute tx0
      try {
        Tx0 tx0 = whirlpoolWallet.tx0(utxos, pool05btc, tx0FeeTarget, mixFeeTarget, tx0Config);
        String txid = tx0.getTx().getHashAsString(); // get txid
      } catch (Exception e) {
        // tx0 failed
      }
    }

    // tx0 method 2: spending an external utxo
    {
      // external utxo for tx0
      WhirlpoolUtxo spendFrom = null; // provide utxo outpoint
      Collection<WhirlpoolUtxo> utxos = Lists.of(spendFrom);

      // configure tx0
      Tx0Config tx0Config =
          whirlpoolWallet.getTx0Config().setChangeWallet(WhirlpoolAccount.BADBANK);
      Tx0FeeTarget tx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_4;

      // preview tx0
      try {
        Tx0Preview tx0Preview =
            whirlpoolWallet.tx0Preview(pool05btc, utxos, tx0Config, tx0FeeTarget, mixFeeTarget);
        long minerFee =
            tx0Preview
                .getTx0MinerFee(); // get minerFee, poolFee, premixValue, changeValue, nbPremix...
      } catch (Exception e) {
        // preview tx0 failed
      }

      // execute tx0
      try {
        Tx0 tx0 = whirlpoolWallet.tx0(utxos, pool05btc, tx0Config, tx0FeeTarget, mixFeeTarget);
        String txid = tx0.getTx().getHashAsString(); // get txid
        // mixing will start automatically when tx0 gets confirmed
      } catch (Exception e) {
        // tx0 failed
      }
    }

    // manually start mixing specific utxo and observe its progress
    whirlpoolWallet.mix(whirlpoolUtxo).subscribe(/* ... */ );

    // stop mixing specific utxo (or remove it from mix queue)
    whirlpoolWallet.mixStop(whirlpoolUtxo);

    // stop Whirlpool
    whirlpoolWalletService.closeWallet();

    // subscribe events for this class (see @Subscribe methods below)
    WhirlpoolEventService.getInstance().register(this);
  }

  // OBSERVE EVENTS
  @Subscribe
  public void onChainBlockChange(ChainBlockChangeEvent e) {
    // new block confirmed
  }

  @Subscribe
  public void onChainBlockChange(MinerFeeChangeEvent e) {
    // miner fee estimation changed
  }

  @Subscribe
  public void onChainBlockChange(MixFailEvent e) {
    // mix failed
  }

  @Subscribe
  public void onMixSuccess(MixSuccessEvent e) {
    // mix success
  }

  @Subscribe
  public void onTx0(Tx0Event e) {
    // tx0 success
  }

  @Subscribe
  public void onUtxosChange(UtxosChangeEvent e) {
    // utxos changed
  }

  @Subscribe
  public void onUtxosRequest(UtxosRequestEvent e) {
    // manual utxos refresh in progress
  }

  @Subscribe
  public void onUtxosResponse(UtxosResponseEvent e) {
    // manual utxos refresh completed
  }

  @Subscribe
  public void onWalletClose(WalletCloseEvent e) {
    // wallet closed
  }

  @Subscribe
  public void onWalletOpen(WalletOpenEvent e) {
    // wallet opened
  }

  @Subscribe
  public void onWalletStart(WalletStartEvent e) {
    // wallet started
  }

  @Subscribe
  public void onWalletStop(WalletStopEvent e) {
    // wallet stopped
  }
}
