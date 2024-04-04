import com.google.common.eventbus.Subscribe;
import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.constants.BIP_WALLETS;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.ExtLibJConfig;
import com.samourai.wallet.websocketClient.IWebsocketClient;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.WhirlpoolInfo;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.*;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Arrays;
import java.util.Collection;

public class JavaExample {
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  // configure these values as you wish
  private WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    // option 1 - use Samourai backend
    BackendServer backendServer = BackendServer.TESTNET;
    boolean onion = true; // use Tor onion services?
    IWebsocketClient wsClient = null; // provide impl, or null to disable real-time sync backend
    DataSourceFactory dataSourceFactory =
        new DojoDataSourceFactory(backendServer, onion, wsClient, BIP_WALLETS.WHIRLPOOL);

    // option 2 - use Dojo backend
    String dojoUrl = ""; // provide Dojo onion URL
    String dojoApiKey = ""; // provide Dojo apiKey
    wsClient = null; // provide impl, or null to disable real-time sync backend
    dataSourceFactory =
        new DojoDataSourceFactory(dojoUrl, dojoApiKey, wsClient, BIP_WALLETS.WHIRLPOOL);

    // option 3 - use external backend
    dataSourceFactory =
        computeDataSourceFactoryExternal(); // example of external backend integration

    SamouraiNetwork samouraiNetwork = SamouraiNetwork.TESTNET;

    // configuration
    boolean mobile = false; // true for mobile configuration, false for desktop/CLI
    ExtLibJConfig extLibJConfig = null; // TODO provide impl
    SorobanConfig sorobanConfig = new SorobanConfig(extLibJConfig);
    WhirlpoolWalletConfig whirlpoolWalletConfig =
        new WhirlpoolWalletConfig(dataSourceFactory, sorobanConfig, mobile);

    // optional - SCODE
    // whirlpoolWalletConfig.setScode("foo");

    // optional - external persistence
    whirlpoolWalletConfig.setDataPersisterFactory(computeDataPersisterFactoryExternal());

    // optional - external partner
    // whirlpoolWalletConfig.setPartner("SPARROW");

    // optional: configure mix to external destination
    String xpub = "xpub..."; // external xpub
    int chain = 0;
    int mixs = 5; // minimum mixs to achieve
    int mixsRandomFactor = 4; // randomness factor: 1/4 probability to mix to ExternalDestination
    ExternalDestination externalDestination =
        new ExternalDestination(xpub, chain, mixs, mixsRandomFactor);
    whirlpoolWalletConfig.setExternalDestination(externalDestination);

    return whirlpoolWalletConfig;
  }

  // Example of external backend integration
  private DataSourceFactory computeDataSourceFactoryExternal() {
    // note: when external data changed, use WalletResponseDataSource.refresh() to refresh it
    return new DataSourceFactory() {

      @Override
      public DataSource createDataSource(
          WhirlpoolWallet whirlpoolWallet,
          HD_Wallet bip44w,
          String passphrase,
          WalletStateSupplier walletStateSupplier,
          UtxoConfigSupplier utxoConfigSupplier)
          throws Exception {
        // use WalletResponse data (or use your own implementation of DataSource)
        return new AbstractDataSource(
            whirlpoolWallet,
            bip44w,
            walletStateSupplier,
            new DataSourceConfig(
                null, // provide minerFeeSupplier impl here
                null, // provide chainSupplier impl here
                null, // provide bipFormatSupplier impl here
                null // provide bipWalletSupplier impl here
                )) {

          @Override
          public UtxoSupplier getUtxoSupplier() {
            return null; // provide impl here
          }

          @Override
          public IPushTx getPushTx() {
            return null; // provide impl here
          }

          @Override
          public ISweepBackend getSweepBackend() {
            return null; // provide impl here
          }

          @Override
          public ISeenBackend getSeenBackend() {
            return null; // provide impl here
          }

          @Override
          public void close() throws Exception {}
        };
      }
    };
  }

  // example of external persistence
  private DataPersisterFactory computeDataPersisterFactoryExternal() {
    return new DataPersisterFactory() {
      @Override
      public DataPersister createDataPersister(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
          throws Exception {
        return new DataPersister() {

          @Override
          public void open() throws Exception {
            // on wallet open
          }

          @Override
          public void close() throws Exception {
            // on wallet closed
          }

          @Override
          public WalletStateSupplier getWalletStateSupplier() {
            return null; // provide impl
          }

          @Override
          public UtxoConfigSupplier getUtxoConfigSupplier() {
            return null; // provide impl
          }

          @Override
          public void load() throws Exception {}

          @Override
          public void persist(boolean force) throws Exception {
            // persist data
          }
        };
      }
    };
  }

  public void example() throws Exception {
    /*
     * CONFIGURATION
     */
    // configure whirlpool
    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig();

    /*
     * WALLET
     */
    // open wallet: standard way
    byte[] seed = null; // provide seed here
    String seedPassphrase = null; // provide seed passphrase here (or null if none)
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(config, seed, seedPassphrase);
    whirlpoolWalletService.openWallet(whirlpoolWallet, seedPassphrase);

    // open wallet: alternate way
    HD_Wallet bip44w = null; // provide bip44 wallet here
    whirlpoolWallet = new WhirlpoolWallet(config, bip44w);
    whirlpoolWalletService.openWallet(whirlpoolWallet, seedPassphrase);

    // whirlpoolInfo can be used to fetch info without opening WhirlpoolWallet
    WhirlpoolInfo whirlpoolInfo = whirlpoolWallet.getWhirlpoolInfo();

    // start whirlpool wallet
    whirlpoolWallet.startAsync().subscribe();

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

    // get min deposit for pool
    long minDeposit = pool05btc.getTx0PreviewMinSpendValue();

    // check spend amount for tx0
    long spendValue = 100000;
    boolean isTx0Possible = pool05btc.isTx0Possible(spendValue);

    // preview smallest possible tx0 for pool (without taking SCODE into account)
    Tx0Preview txOPreviewMin = pool05btc.getTx0PreviewMin();

    WalletSupplier walletSupplier = whirlpoolWallet.getWalletSupplier();

    /*
     * UTXOS
     */
    UtxoSupplier utxoSupplier = whirlpoolWallet.getUtxoSupplier();

    // list utxos
    Collection<WhirlpoolUtxo> utxosDeposit = utxoSupplier.findUtxos(SamouraiAccount.DEPOSIT);

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
      Collection<WhirlpoolUtxo> utxos = Arrays.asList(whirlpoolUtxo);

      // fetch tx0Datas once (you should refresh it after pushing any TX0)
      Tx0Info tx0Info = whirlpoolInfo.fetchTx0Info("scode");

      // configure tx0
      Tx0FeeTarget tx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0Config tx0Config =
          tx0Info.getTx0Config(tx0FeeTarget, mixFeeTarget).setChangeWallet(SamouraiAccount.BADBANK);

      // preview tx0
      try {
        // preview all pools
        Tx0Previews tx0Previews = tx0Info.tx0Previews(utxos, tx0Config);

        // pool preview
        Tx0Preview tx0Preview = tx0Previews.getTx0Preview("0.5btc");
        long minerFee =
            tx0Preview
                .getTx0MinerFee(); // get minerFee, poolFee, premixValue, changeValue, nbPremix...
      } catch (Exception e) {
        // preview tx0 failed
      }

      // execute tx0
      try {
        Tx0 tx0 = tx0Info.tx0(walletSupplier, utxoSupplier, utxos, pool05btc, tx0Config);
        String txid = tx0.getTx().getHashAsString(); // get txid
      } catch (Exception e) {
        // tx0 failed
      }
    }

    // tx0 method 2: spending an external utxo
    {
      // external utxo for tx0
      UnspentOutput spendFrom = null; // provide utxo outpoint
      Collection<UnspentOutput> utxos = Arrays.asList(spendFrom);

      // fetch tx0Datas once
      Tx0Info tx0Info = whirlpoolInfo.fetchTx0Info("scode");

      // configure tx0
      Tx0FeeTarget tx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0Config tx0Config =
          tx0Info.getTx0Config(tx0FeeTarget, mixFeeTarget).setChangeWallet(SamouraiAccount.BADBANK);

      // preview tx0
      try {
        // preview all pools
        Tx0Previews tx0Previews = tx0Info.tx0Previews(tx0Config, utxos);

        // pool preview
        Tx0Preview tx0Preview = tx0Previews.getTx0Preview("0.5btc");

        long minerFee =
            tx0Preview
                .getTx0MinerFee(); // get minerFee, poolFee, premixValue, changeValue, nbPremix...
      } catch (Exception e) {
        // preview tx0 failed
      }

      // execute tx0
      try {
        Tx0 tx0 = tx0Info.tx0(walletSupplier, utxoSupplier, utxos, tx0Config, pool05btc);
        String txid = tx0.getTx().getHashAsString(); // get txid
        // mixing will start automatically when tx0 gets confirmed
      } catch (Exception e) {
        // tx0 failed
      }
    }

    /*
     * PAYNYM
     */
    PaynymSupplier paynymSupplier = whirlpoolWallet.getPaynymSupplier();

    // get Paynym state
    PaynymState paynymState = paynymSupplier.getPaynymState();

    // claim paynym
    paynymSupplier.claim();

    // follow
    paynymSupplier.follow("friendPaymentCode");

    // unfollow
    paynymSupplier.unfollow("friendPaymentCode");

    // refresh
    paynymSupplier.refresh();

    /*
     * WALLET MANAGEMENT
     */
    // manually start mixing specific utxo
    whirlpoolWallet.mix(whirlpoolUtxo);

    // manually refresh utxos
    whirlpoolWallet.refreshUtxosAsync().subscribe();

    // stop mixing specific utxo (or remove it from mix queue)
    whirlpoolWallet.mixStop(whirlpoolUtxo);

    // stop Whirlpool
    whirlpoolWalletService.closeWallet();

    // subscribe events for this class (see @Subscribe methods below)
    WhirlpoolEventService.getInstance().register(this);

    // get debug info
    String debug = whirlpoolWallet.getDebug();
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
  public void onMixProgress(MixProgress e) {
    // mix progress
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
  public void onUtxoChanges(UtxoChangesEvent e) {
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

  @Subscribe
  public void onPostmixIndexAlreadyUsedEvent(PostmixIndexAlreadyUsedEvent e) {
    // postmix index problem detected (will be automatically fixed)
  }

  @Subscribe
  public void onPostmixIndexFixProgressEvent(PostmixIndexFixProgressEvent e) {
    // postmix index problem is being fixed
  }

  @Subscribe
  public void onPostmixIndexFixSuccessEvent(PostmixIndexFixSuccessEvent e) {
    // postmix index problem fixed successfully
  }

  @Subscribe
  public void onPostmixIndexFixFailEvent(PostmixIndexFixFailEvent e) {
    // postmix index problem could not be fixed
  }

  @Subscribe
  public void onPaynymChangeEvent(PaynymChangeEvent e) {
    // paynym update
  }
}
