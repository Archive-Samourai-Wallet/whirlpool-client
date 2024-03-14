package com.samourai.whirlpool.client.test;

import ch.qos.logback.classic.Level;
import com.samourai.http.client.JettyHttpClient;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.OxtApi;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bip47.rpc.java.SecretPointFactoryJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.constants.BIP_WALLETS;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.tx0.MockTx0PreviewServiceConfig;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.coordinator.MockCoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.MemoryDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DojoDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MockMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersisterFile;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.soroban.payload.tx0.Tx0PushRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTest {
  protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static final String SEED_WORDS = "all all all all all all all all all all all all";
  protected static final String SEED_PASSPHRASE = "whirlpool";
  private static final String STATE_FILENAME = "/tmp/tmp-state";

  protected static final String XPUB_DEPOSIT_BIP84 =
      "tpubDCGZwoNuBCYuS9LbHLzdbfzjYe2fn7dKAHVSUPTkb1vuSfi7hUuiG3eT7tE1DzdcjhBF5SZk3vuu8EkcFUnbsaBpCyB2uDP7v3n774RGre9";
  protected static final String XPUB_PREMIX =
      "tpubDCGZwoP3Ws5sUQb1uwYxqQfmEjiPfSGrBcomWLyYgYw7YP5LenJexEzxwHvJoYUQSCWZupgzcx91fr4wVdJCb21LTr6fcv4GvBio4bzAhvr";
  protected static final String XPUB_POSTMIX =
      "tpubDCGZwoP3Ws5sZLQpXGpDhtbErQPyFdf59k8JmUpnL5fM6qAj8bbPXNwLLtfiS5s8ivZ1W1PQnaET7obFeiDSooTFBKcTweS29BkgHwhhsQD";

  protected IHttpClient httpClient;

  protected ChainSupplier mockChainSupplier =
      () -> {
        WalletResponse.InfoBlock infoBlock = new WalletResponse.InfoBlock();
        infoBlock.height = 1234;
        return infoBlock;
      };

  protected SamouraiNetwork samouraiNetwork = SamouraiNetwork.TESTNET;

  protected OxtApi oxtApi;

  protected Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
  protected BipFormatSupplier bipFormatSupplier = BIP_FORMAT.PROVIDER;
  protected NetworkParameters params = TestNet3Params.get();
  protected HD_WalletFactoryGeneric hdWalletFactory = HD_WalletFactoryGeneric.getInstance();
  protected Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  protected AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected CryptoUtil cryptoUtil = CryptoUtil.getInstanceJava();
  protected Pool pool01btc;
  protected Pool pool05btc;
  protected Pool pool001btc;
  private static final String WALLET_RESPONSE =
      "{\"wallet\": {\"final_balance\": 116640227},\"info\": {\"fees\": {\"2\": 1,\"4\": 1,\"6\": 1,\"12\": 1,\"24\": 1},\"latest_block\": {\"height\": 2064015,\"hash\": \"00000000000000409297f8e0c0e73475cdd215ef675ad82802a08507b1c1d0e1\",\"time\": 1628498860}},\"addresses\": [{\"address\": \"vpub5YEhBtZy85KxLBxQB4MiHZvjjhz5DcYT9DV2gLshFykuWXjqSzLxpLd4TwS8nFxJmXAX8RrxRxpanndBh5a9AJPbrJEtqCcTKAnRYcP4Aed\",\"final_balance\": 116640227,\"account_index\": 511,\"change_index\": 183,\"n_tx\": 137}],\"txs\": [],\"unspent_outputs\": []}";

  protected MinerFeeSupplier mockMinerFeeSupplier = mockMinerFeeSupplier();
  protected Tx0PreviewService mockTx0PreviewService =
      new Tx0PreviewService(
          mockMinerFeeSupplier, new MockTx0PreviewServiceConfig(samouraiNetwork)) {};
  private Tx0Data[] mockTx0Datas;
  protected CoordinatorSupplier coordinatorSupplier =
      new MockCoordinatorSupplier(mockTx0PreviewService, computeWhirlpoolWalletConfig());

  protected MockPushTx pushTx = new MockPushTx(params);
  protected static final String MOCK_SAMOURAI_FEE_ADDRESS =
      "tb1qfd0ukes4xw3xvxwhj9m53nt2huh75khrrdm5dv";

  public AbstractTest() throws Exception {
    ClientUtils.setLogLevel(Level.DEBUG.toString());

    httpClient = new JettyHttpClient(5000, null, null);
    oxtApi = new OxtApi(httpClient);

    pool01btc = new Pool();
    pool01btc.setPoolId("0.01btc");
    pool01btc.setDenomination(1000000);
    pool01btc.setFeeValue(50000);
    pool01btc.setPremixValue(1000175);
    pool01btc.setPremixValueMin(1000170);
    pool01btc.setPremixValueMax(1010000);
    pool01btc.setTx0MaxOutputs(70);
    pool01btc.setAnonymitySet(5);

    pool001btc = new Pool();
    pool001btc.setPoolId("0.01btc");
    pool001btc.setDenomination(100000);
    pool001btc.setFeeValue(5000);
    pool001btc.setPremixValue(100200);
    pool001btc.setPremixValueMin(100017);
    pool001btc.setPremixValueMax(101000);
    pool01btc.setTx0MaxOutputs(70);
    pool001btc.setAnonymitySet(5);

    pool05btc = new Pool();
    pool05btc.setPoolId("0.5btc");
    pool05btc.setDenomination(5000000);
    pool05btc.setFeeValue(250000);
    pool05btc.setPremixValue(5000200);
    pool05btc.setPremixValueMin(5000170);
    pool05btc.setPremixValueMax(5010000);
    pool01btc.setTx0MaxOutputs(70);
    pool05btc.setAnonymitySet(5);

    resetWalletStateFile();
  }

  protected WalletResponse mockWalletResponse() throws Exception {
    return ClientUtils.fromJson(WALLET_RESPONSE, WalletResponse.class);
  }

  protected MinerFeeSupplier mockMinerFeeSupplier() throws Exception {
    return new MockMinerFeeSupplier();
  }

  protected IHttpClientService mockHttpClientService() {
    return new IHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return null;
      }

      @Override
      public void changeIdentity() {}

      @Override
      public void stop() {}
    };
  }

  protected UnspentOutput newUnspentOutput(String hash, int index, long value, HD_Address hdAddress)
      throws Exception {
    String bech32Address = bech32Util.toBech32(hdAddress, params);
    String scriptBytes =
        Hex.toHexString(Bech32UtilGeneric.getInstance().computeScriptPubKey(bech32Address, params));
    UnspentOutput spendFrom = new UnspentOutput();
    spendFrom.tx_hash = hash;
    spendFrom.tx_output_n = index;
    spendFrom.value = value;
    spendFrom.script = scriptBytes;
    spendFrom.addr = bech32Address;
    spendFrom.confirmations = 1234;
    spendFrom.xpub = new UnspentOutput.Xpub();
    spendFrom.xpub.path = "m/0/" + hdAddress.getAddressIndex();
    spendFrom.xpub.m = XPUB_DEPOSIT_BIP84;
    return spendFrom;
  }

  protected IHttpClientService computeHttpClientService() {
    return new IHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return httpClient;
      }

      @Override
      public void changeIdentity() {}

      @Override
      public void stop() {}
    };
  }

  protected void onPushTx0(Tx0PushRequest request, Transaction tx) throws Exception {
    // overridable
  }

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    DataSourceFactory dataSourceFactory =
        new DojoDataSourceFactory(BackendServer.TESTNET, false, null, BIP_WALLETS.WHIRLPOOL);
    ISecretPointFactory secretPointFactory = SecretPointFactoryJava.getInstance();
    IHttpClientService httpClientService = computeHttpClientService();
    CryptoUtil cryptoUtil = CryptoUtil.getInstanceJava();
    RpcClientService rpcClientService =
        new RpcClientService(httpClientService, cryptoUtil, bip47Util, false, params);
    SorobanWalletService sorobanWalletService =
        new SorobanWalletService(bip47Util, bipFormatSupplier, params, rpcClientService);
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(
            dataSourceFactory,
            secretPointFactory,
            cryptoUtil,
            sorobanWalletService,
            httpClientService,
            bip47Util,
            samouraiNetwork,
            false,
            false);
    config.setDataPersisterFactory(new MemoryDataPersisterFactory());
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

  protected UnspentOutput computeUtxo(String hash, int n, String xpub, int confirms) {
    UnspentOutput utxo = new UnspentOutput();
    utxo.tx_hash = hash;
    utxo.tx_output_n = n;
    utxo.xpub = new UnspentOutput.Xpub();
    utxo.xpub.m = xpub;
    utxo.confirmations = confirms;
    return utxo;
  }

  protected void assertUtxoEquals(UnspentOutput[] utxos1, Collection<WhirlpoolUtxo> utxos2) {
    Assertions.assertEquals(utxos1.length, utxos2.size());

    List<String> utxos1Ids =
        Arrays.asList(utxos1).stream()
            .map((Function<UnspentOutput, String>) utxo -> computeUtxoId(utxo))
            .collect(Collectors.<String>toList());
    for (WhirlpoolUtxo whirlpoolUtxo : utxos2) {
      // search utxo by id
      Assertions.assertTrue(utxos1Ids.contains(computeUtxoId(whirlpoolUtxo.getUtxo())));
    }
  }

  protected String computeUtxoId(UnspentOutput utxo) {
    return utxo.tx_hash + ':' + utxo.tx_output_n;
  }

  protected void mockTx0Datas() throws Exception {
    byte[] feePayload =
        computeWhirlpoolWalletConfig()
            .getFeeOpReturnImpl()
            .computeFeePayload(0, (short) 0, (short) 0);
    this.mockTx0Datas =
        new Tx0Data[] {
          new Tx0Data(
              "0.01btc",
              "PM8TJbEnXU7JpR8yMdQee9H5C4RNWTpWAgmb2TVyQ4zfnaQBDMTJ4yYVP9Re8NVsZDSwXvogYbssrqkfVwac9U1QnxdCU2G1zH7Gq6L3JJjzcuWGjB9N",
              42500,
              0,
              0,
              null,
              feePayload,
              feePayload,
              MOCK_SAMOURAI_FEE_ADDRESS)
        };
  }
}
