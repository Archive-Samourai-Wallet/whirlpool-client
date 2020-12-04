import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.tx0.UnspentOutputWithKey;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import io.reactivex.functions.Consumer;
import java.util.Collection;
import java8.util.Lists;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;

public class JavaExample {

  // TODO configure these values as you wish
  private WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    IHttpClientService httpClientService = null; // provide impl here, ie: new AndroidHttpClient();
    IStompClientService stompClientService =
        null; // provide impl here, ie: new AndroidStompClientService();

    WhirlpoolServer whirlpoolServer = WhirlpoolServer.TESTNET;

    boolean onion = true;
    String serverUrl = whirlpoolServer.getServerUrl(onion);
    String backendUrl = BackendServer.TESTNET.getBackendUrl(onion);

    ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
    IHttpClient httpClientBackend = httpClientService.getHttpClient(HttpUsage.BACKEND);
    BackendApi backendApi =
        new BackendApi(httpClientBackend, backendUrl, Optional.<OAuthManager>empty());

    NetworkParameters params = whirlpoolServer.getParams();
    boolean isAndroid = false;
    WhirlpoolWalletConfig whirlpoolWalletConfig =
        new WhirlpoolWalletConfig(
            httpClientService, stompClientService, serverApi, params, isAndroid, backendApi);

    whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
    whirlpoolWalletConfig.setAutoMix(false); // disable auto-mix

    // configure optional settings (or don't set anything for using default values)
    whirlpoolWalletConfig.setScode("foo");
    whirlpoolWalletConfig.setMaxClients(1);
    whirlpoolWalletConfig.setClientDelay(15);
    return whirlpoolWalletConfig;
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
    // open wallet
    HD_Wallet bip84w = null; // provide your wallet here
    WhirlpoolWallet whirlpoolWallet =
        whirlpoolWalletService.openWallet(config, bip84w, "/tmp/state", "/tmp/utxos");

    // start whirlpool wallet
    whirlpoolWallet.start();

    // get mixing state (started, utxosMixing, nbMixing, nbQueued...)
    MixingState mixingState = whirlpoolWallet.getMixingState();

    // observe mixing state
    mixingState
        .getObservable()
        .subscribe(
            new Consumer<MixingState>() {
              @Override
              public void accept(MixingState mixingState) throws Exception {
                // get whirlpool status
                boolean whirlpoolStarted = mixingState.isStarted();

                // get mixing utxos
                Collection<WhirlpoolUtxo> mixingUtxos = mixingState.getUtxosMixing();
                if (mixingUtxos.isEmpty()) {
                  // no utxo mixing currently
                  return;
                }

                // one utxo (at least) is mixing currently
                WhirlpoolUtxo mixingUtxo = mixingUtxos.iterator().next();

                // get mixing progress for this utxo
                MixProgress mixProgress = mixingUtxo.getUtxoState().getMixProgress();
                MixStep mixStep = mixProgress.getMixStep(); // CONNECTING, CONNECTED...
                int progressPercent = mixProgress.getProgressPercent();
              }
            });

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

    // configure utxo
    whirlpoolUtxo.setPoolId("0.01btc");

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

      // choose pool
      whirlpoolWallet.setPool(whirlpoolUtxo, pool05btc.getPoolId());

      // preview tx0
      try {
        Tx0Preview tx0Preview =
            whirlpoolWallet.tx0Preview(utxos, pool05btc, tx0Config, tx0FeeTarget, mixFeeTarget);
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
      UnspentOutput spendFrom = null; // provide utxo outpoint
      byte[] spendFromPrivKey = null; // provide utxo private key
      Collection<UnspentOutputWithKey> utxos =
          Lists.of(new UnspentOutputWithKey(spendFrom, spendFromPrivKey));

      // configure tx0
      Tx0Config tx0Config =
          whirlpoolWallet.getTx0Config().setChangeWallet(WhirlpoolAccount.BADBANK);
      Tx0FeeTarget tx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
      Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_4;

      // pool for tx0
      Pool pool = poolSupplier.findPoolById(pool05btc.getPoolId()); // provide poolId

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
        Tx0 tx0 = whirlpoolWallet.tx0(utxos, pool, tx0Config, tx0FeeTarget, mixFeeTarget);
        String txid = tx0.getTx().getHashAsString(); // get txid
      } catch (Exception e) {
        // tx0 failed
      }
    }

    /*
     * MIX
     */
    whirlpoolWallet.mix(whirlpoolUtxo).subscribe(/* ... */ );

    // stop mixing specific utxo (or remove it from mix queue)
    whirlpoolWallet.mixStop(whirlpoolUtxo);

    // stop Whirlpool
    whirlpoolWalletService.closeWallet();
  }
}
