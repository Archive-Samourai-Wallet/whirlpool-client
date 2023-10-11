package com.samourai.whirlpool.client.test;

import ch.qos.logback.classic.Level;
import com.samourai.http.client.*;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.rpc.java.SecretPointFactoryJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.BipUtxoImpl;
import com.samourai.wallet.utxo.UtxoConfirmInfoImpl;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.chain.BasicChainSupplier;
import com.samourai.whirlpool.client.wallet.data.chain.ChainData;
import com.samourai.whirlpool.client.wallet.data.dataPersister.MemoryDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DojoDataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DojoDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.minerFee.BasicMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.MockPoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersisterFile;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import com.samourai.whirlpool.protocol.rest.PushTxSuccessResponse;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import io.reactivex.Single;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTest {
  protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static final String SEED_WORDS = "all all all all all all all all all all all all";
  protected static final String SEED_PASSPHRASE = "whirlpool";
  private static final String STATE_FILENAME = "/tmp/tmp-state";
  protected static final UtxoUtil utxoUtil = UtxoUtil.getInstance();
  protected static final TxUtil txUtil = TxUtil.getInstance();

  protected IHttpClient httpClient;

  protected BasicChainSupplier chainSupplier =
      new BasicChainSupplier() {
        @Override
        public ChainData getValue() {
          WalletResponse.InfoBlock infoBlock = new WalletResponse.InfoBlock();
          infoBlock.height = 1234;
          return new ChainData(infoBlock);
        }

        @Override
        public void setValue(ChainData value) throws Exception {
          // ignore
        }
      };

  protected BipFormatSupplier bipFormatSupplier = BIP_FORMAT.PROVIDER;
  protected NetworkParameters params = TestNet3Params.get();
  protected HD_WalletFactoryGeneric hdWalletFactory = HD_WalletFactoryGeneric.getInstance();
  protected Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  protected AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected Pool pool01btc;
  protected Pool pool05btc;
  protected Pool pool001btc;
  private static final String XPUB_DEPOSIT_BIP84 =
      "tpubDCGZwoP3Ws5sZLQpXGpDhtbErQPyFdf59k8JmUpnL5fM6qAj8bbPXNwLLtfiS5s8ivZ1W1PQnaET7obFeiDSooTFBKcTweS29BkgHwhhsQD";
  private static final String POOLS_RESPONSE =
      "{\"pools\":[{\"poolId\":\"0.01btc\",\"denomination\":1000000,\"feeValue\":50000,\"mustMixBalanceMin\":1000170,\"mustMixBalanceCap\":1009690,\"mustMixBalanceMax\":1019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":180,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":672969,\"nbConfirmed\":2},{\"poolId\":\"0.001btc\",\"denomination\":100000,\"feeValue\":5000,\"mustMixBalanceMin\":100170,\"mustMixBalanceCap\":109690,\"mustMixBalanceMax\":119125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":25,\"nbRegistered\":157,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":217766,\"nbConfirmed\":2},{\"poolId\":\"0.05btc\",\"denomination\":5000000,\"feeValue\":175000,\"mustMixBalanceMin\":5000170,\"mustMixBalanceCap\":5009690,\"mustMixBalanceMax\":5019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":126,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":4237382,\"nbConfirmed\":2},{\"poolId\":\"0.5btc\",\"denomination\":50000000,\"feeValue\":1750000,\"mustMixBalanceMin\":50000170,\"mustMixBalanceCap\":50009690,\"mustMixBalanceMax\":50019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":34,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":5971543,\"nbConfirmed\":2}]}";
  private static final String WALLET_RESPONSE =
      "{\"wallet\": {\"final_balance\": 116640227},\"info\": {\"fees\": {\"2\": 1,\"4\": 1,\"6\": 1,\"12\": 1,\"24\": 1},\"latest_block\": {\"height\": 2064015,\"hash\": \"00000000000000409297f8e0c0e73475cdd215ef675ad82802a08507b1c1d0e1\",\"time\": 1628498860}},\"addresses\": [{\"address\": \""
          + XPUB_DEPOSIT_BIP84
          + "\",\"final_balance\": 116640227,\"account_index\": 511,\"change_index\": 183,\"n_tx\": 137}],\"txs\": [],\"unspent_outputs\": []}";

  protected MockPushTx pushTx = new MockPushTx(params);
  protected Map<String, Tx0Data> mockTx0Datas = new LinkedHashMap<>();
  protected static final String MOCK_SAMOURAI_FEE_ADDRESS =
      "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx";

  protected WhirlpoolWalletConfig whirlpoolWalletConfig;
  protected ExpirablePoolSupplier poolSupplier;
  protected Tx0PreviewService tx0PreviewService;
  protected Tx0Service tx0Service;

  public AbstractTest() throws Exception {
    ClientUtils.setLogLevel(Level.DEBUG, Level.DEBUG);

    RandomUtil._setTestMode();

    httpClient = new JettyHttpClient(5000, Optional.<HttpProxy>empty(), "test");

    pool01btc = new Pool();
    pool01btc.setPoolId("0.01btc");
    pool01btc.setDenomination(1000000);
    pool01btc.setFeeValue(50000);
    pool01btc.setMustMixBalanceMin(1000170);
    pool01btc.setMustMixBalanceCap(1009500);
    pool01btc.setMustMixBalanceMax(1010000);
    pool01btc.setMinAnonymitySet(5);
    pool01btc.setMinMustMix(3);
    pool01btc.setTx0MaxOutputs(70);
    pool01btc.setNbRegistered(0);
    pool01btc.setMixAnonymitySet(5);
    pool01btc.setMixStatus(MixStatus.CONFIRM_INPUT);
    pool01btc.setElapsedTime(1000);
    pool01btc.setNbConfirmed(0);

    pool001btc = new Pool();
    pool001btc.setPoolId("0.001btc");
    pool001btc.setDenomination(100000);
    pool001btc.setFeeValue(5000);
    pool001btc.setMustMixBalanceMin(100017);
    pool001btc.setMustMixBalanceCap(100950);
    pool001btc.setMustMixBalanceMax(101000);
    pool001btc.setMinAnonymitySet(5);
    pool001btc.setMinMustMix(3);
    pool001btc.setTx0MaxOutputs(70);
    pool001btc.setNbRegistered(0);
    pool001btc.setMixAnonymitySet(5);
    pool001btc.setMixStatus(MixStatus.CONFIRM_INPUT);
    pool001btc.setElapsedTime(1000);
    pool001btc.setNbConfirmed(0);

    pool05btc = new Pool();
    pool05btc.setPoolId("0.05btc");
    pool05btc.setDenomination(5000000);
    pool05btc.setFeeValue(250000);
    pool05btc.setMustMixBalanceMin(5000170);
    pool05btc.setMustMixBalanceCap(5009500);
    pool05btc.setMustMixBalanceMax(5010000);
    pool05btc.setMinAnonymitySet(5);
    pool05btc.setMinMustMix(3);
    pool05btc.setTx0MaxOutputs(70);
    pool05btc.setNbRegistered(0);
    pool05btc.setMixAnonymitySet(5);
    pool05btc.setMixStatus(MixStatus.CONFIRM_INPUT);
    pool05btc.setElapsedTime(1000);
    pool05btc.setNbConfirmed(0);

    resetWalletStateFile();
  }

  protected void setup() throws Exception {
    AbstractTest.this.setup(false);
  }

  protected void setup(boolean isOpReturnV0) throws Exception {
    // mock Tx0Data for reproductible test
    whirlpoolWalletConfig = computeWhirlpoolWalletConfig(isOpReturnV0);
    tx0PreviewService = mockTx0PreviewService();
    poolSupplier = mockPoolSupplier();
    tx0PreviewService._setPoolSupplier(poolSupplier);

    tx0Service =
        new Tx0Service(params, tx0PreviewService, whirlpoolWalletConfig.getFeeOpReturnImpl());

    List<Pool> pools = Arrays.asList(pool001btc, pool01btc, pool05btc);
    tx0PreviewService.initPools(pools);

    mockTx0Datas();
  }

  protected WalletResponse mockWalletResponse() throws Exception {
    return ClientUtils.fromJson(WALLET_RESPONSE, WalletResponse.class);
  }

  private Tx0PreviewService mockTx0PreviewService() throws Exception {
    MinerFeeSupplier minerFeeSupplier = mockMinerFeeSupplier();
    return new Tx0PreviewService(minerFeeSupplier, bipFormatSupplier, whirlpoolWalletConfig) {
      @Override
      public Map<String, Tx0Data> fetchTx0Data(boolean cascading) throws Exception {
        if (mockTx0Datas != null) {
          return mockTx0Datas;
        }
        return super.fetchTx0Data(cascading);
      }
    };
  }

  private ExpirablePoolSupplier mockPoolSupplier() throws Exception {
    PoolsResponse poolsResponse = ClientUtils.fromJson(POOLS_RESPONSE, PoolsResponse.class);
    return new MockPoolSupplier(tx0PreviewService, poolsResponse.pools);
  }

  protected MinerFeeSupplier mockMinerFeeSupplier() throws Exception {
    BasicMinerFeeSupplier minerFeeSupplier = new BasicMinerFeeSupplier(1, 100);
    minerFeeSupplier.setValue(1);
    return minerFeeSupplier;
  }

  protected IWhirlpoolHttpClientService mockHttpClientService() {
    return new IWhirlpoolHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return null;
      }

      @Override
      public void stop() {}
    };
  }

  protected BipUtxo newUtxo(String hash, int index, long value, HD_Address hdAddress)
      throws Exception {
    String bech32Address = bech32Util.toBech32(hdAddress, params);
    byte[] scriptBytes = Bech32UtilGeneric.getInstance().computeScriptPubKey(bech32Address, params);
    BipUtxo spendFrom =
        new BipUtxoImpl(
            hash,
            index,
            value,
            bech32Address,
            new UtxoConfirmInfoImpl(1234),
            scriptBytes,
            XPUB_DEPOSIT_BIP84,
            false,
            0,
            hdAddress.getAddressIndex());
    return spendFrom;
  }

  protected UnspentOutput newUnspentOutput(String hash, int n, String xpub, int confirms) {
    UnspentOutput utxo = new UnspentOutput();
    utxo.tx_hash = hash;
    utxo.tx_output_n = n;
    utxo.xpub = new UnspentOutput.Xpub();
    utxo.xpub.path = utxoUtil.computePath(0, n);
    utxo.xpub.m = xpub;
    utxo.confirmations = confirms;
    return utxo;
  }

  protected IWhirlpoolHttpClientService computeHttpClientService() {
    return new IWhirlpoolHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return httpClient;
      }

      @Override
      public void stop() {}
    };
  }

  protected void onPushTx0(Tx0PushRequest request, Transaction tx) throws Exception {
    // overridable
  }

  protected ServerApi computeServerApi() {
    return new ServerApi(WhirlpoolServer.TESTNET.getServerUrlClear(), computeHttpClientService()) {
      @Override
      public Single<PushTxSuccessResponse> pushTx0(Tx0PushRequest request) throws Exception {
        // mock pushtx0
        byte[] txBytes = WhirlpoolProtocol.decodeBytes(request.tx64);
        Transaction tx = new Transaction(params, txBytes);
        onPushTx0(request, tx);
        return Single.just(new PushTxSuccessResponse(tx.getHashAsString()));
      }
    };
  }

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(boolean isOpReturnV0) {
    ServerApi serverApi = computeServerApi();
    return computeWhirlpoolWalletConfig(isOpReturnV0, serverApi);
  }

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(
      boolean isOpReturnV0, ServerApi serverApi) {
    BackendServer backendServer = BackendServer.TESTNET;
    boolean onion = false;
    DataSourceFactory dataSourceFactory =
        new DojoDataSourceFactory(backendServer, onion, null) {
          @Override
          public DataSource createDataSource(
              WhirlpoolWallet whirlpoolWallet,
              HD_Wallet bip44w,
              String passphrase,
              WalletStateSupplier walletStateSupplier,
              UtxoConfigSupplier utxoConfigSupplier)
              throws Exception {
            // DataSource with mocked PoolSupplier & Tx0PreviewService
            String dojoUrl = backendServer.getBackendUrl(onion);
            WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
            IHttpClient httpClientBackend = config.getHttpClient(HttpUsage.BACKEND);
            BackendApi backendApi = BackendApi.newBackendApiSamourai(httpClientBackend, dojoUrl);
            return new DojoDataSource(
                whirlpoolWallet,
                bip44w,
                walletStateSupplier,
                utxoConfigSupplier,
                backendApi,
                null) {
              @Override
              protected ExpirablePoolSupplier computePoolSupplier(
                  WhirlpoolWallet whirlpoolWallet, Tx0PreviewService tx0PreviewService)
                  throws Exception {
                return AbstractTest.this.poolSupplier;
              }

              @Override
              protected Tx0PreviewService computeTx0PreviewService(
                  WhirlpoolWallet whirlpoolWallet,
                  MinerFeeSupplier minerFeeSupplier,
                  BipFormatSupplier bipFormatSupplier) {
                return AbstractTest.this.tx0PreviewService;
              }

              @Override
              protected BasicChainSupplier computeChainSupplier() throws Exception {
                return AbstractTest.this.chainSupplier;
              }
            };
          }
        };
    ISecretPointFactory secretPointFactory = SecretPointFactoryJava.getInstance();
    IWhirlpoolHttpClientService httpClientService = computeHttpClientService();
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(
            dataSourceFactory,
            secretPointFactory,
            null,
            httpClientService,
            null,
            null,
            serverApi,
            TestNet3Params.get(),
            false);
    config.setDataPersisterFactory(new MemoryDataPersisterFactory());
    if (isOpReturnV0) {
      config.setFeeOpReturnImplV0();
    }
    config.setTx0MaxOutputs(70);
    config.getFeeOpReturnImpl().setTestMode(true);
    return config;
  }

  protected File resetFile(String fileName) throws Exception {
    File f = new File(fileName);
    if (f.exists()) {
      f.delete();
    }
    f.createNewFile();
    return f;
  }

  protected WalletStateSupplier computeWalletStateSupplier() throws Exception {
    ClientUtils.createFile(STATE_FILENAME);
    WalletStateSupplier walletStateSupplier =
        new WalletStatePersistableSupplier(new WalletStatePersisterFile(STATE_FILENAME), null);
    walletStateSupplier.load();
    return walletStateSupplier;
  }

  protected void resetWalletStateFile() throws Exception {
    resetFile(STATE_FILENAME);
  }

  protected void assertUtxoEquals(BipUtxo[] utxos1, Collection<WhirlpoolUtxo> utxos2) {
    Assertions.assertEquals(utxos1.length, utxos2.size());

    List<String> utxos1Ids =
        Arrays.asList(utxos1).stream()
            .map(utxo -> computeUtxoId(utxo))
            .collect(Collectors.<String>toList());
    for (WhirlpoolUtxo whirlpoolUtxo : utxos2) {
      // search utxo by id
      Assertions.assertTrue(utxos1Ids.contains(computeUtxoId(whirlpoolUtxo)));
    }
  }

  protected String computeUtxoId(BipUtxo utxo) {
    return utxo.getTxHash() + ':' + utxo.getTxOutputIndex();
  }

  private void mockTx0Datas() throws Exception {
    whirlpoolWalletConfig.setScode(null);
    mockTx0Datas.clear();
    byte[] feePayload =
        whirlpoolWalletConfig.getFeeOpReturnImpl().computeFeePayload(0, (short) 0, (short) 0);
    mockTx0Datas.put(
        "0.5btc",
        new Tx0Data(
            "0.5btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            1487500,
            0,
            0,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.05btc",
        new Tx0Data(
            "0.05btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            148750,
            0,
            0,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.01btc",
        new Tx0Data(
            "0.01btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            42500,
            0,
            0,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.001btc",
        new Tx0Data(
            "0.001btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            5000,
            0,
            0,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
  }

  protected void mockTx0Datas_SCODE_100PERCENT() throws Exception {
    whirlpoolWalletConfig.setScode("MOCK_100PERCENT");
    mockTx0Datas.clear();
    byte[] feePayload =
        whirlpoolWalletConfig.getFeeOpReturnImpl().computeFeePayload(0, (short) 0, (short) 0);
    mockTx0Datas.put(
        "0.5btc",
        new Tx0Data(
            "0.5btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            0,
            1487500,
            100,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.05btc",
        new Tx0Data(
            "0.05btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            0,
            148750,
            100,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.01btc",
        new Tx0Data(
            "0.01btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            0,
            42500,
            100,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.001btc",
        new Tx0Data(
            "0.001btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            0,
            5000,
            100,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
  }

  protected void mockTx0Datas_SCODE_50PERCENT() throws Exception {
    whirlpoolWalletConfig.setScode("MOCK_50PERCENT");
    mockTx0Datas.clear();
    byte[] feePayload =
        whirlpoolWalletConfig.getFeeOpReturnImpl().computeFeePayload(0, (short) 0, (short) 0);
    mockTx0Datas.put(
        "0.5btc",
        new Tx0Data(
            "0.5btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            743750,
            0,
            50,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.05btc",
        new Tx0Data(
            "0.05btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            74375,
            0,
            50,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.01btc",
        new Tx0Data(
            "0.01btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            21250,
            0,
            50,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
    mockTx0Datas.put(
        "0.001btc",
        new Tx0Data(
            "0.001btc",
            "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
            2500,
            0,
            50,
            null,
            feePayload,
            MOCK_SAMOURAI_FEE_ADDRESS));
  }
}
