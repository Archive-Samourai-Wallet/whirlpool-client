package com.samourai.whirlpool.client.test;

import ch.qos.logback.classic.Level;
import com.samourai.http.client.*;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.rpc.java.SecretPointFactoryJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.tx0.ITx0PreviewServiceConfig;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.dataPersister.MemoryDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DojoDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.minerFee.BasicMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.MockPoolSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersisterFile;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import com.samourai.whirlpool.protocol.rest.PushTxSuccessResponse;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import io.reactivex.Observable;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractTest {
  protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static final String SEED_WORDS = "all all all all all all all all all all all all";
  protected static final String SEED_PASSPHRASE = "whirlpool";
  private static final String STATE_FILENAME = "/tmp/tmp-state";

  protected IHttpClient httpClient;

  protected NetworkParameters params = TestNet3Params.get();
  protected HD_WalletFactoryGeneric hdWalletFactory = HD_WalletFactoryGeneric.getInstance();
  protected Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  protected AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected Pool pool01btc;
  protected Pool pool05btc;
  protected Pool pool001btc;
  private static final String POOLS_RESPONSE =
      "{\"pools\":[{\"poolId\":\"0.01btc\",\"denomination\":1000000,\"feeValue\":50000,\"mustMixBalanceMin\":1000170,\"mustMixBalanceCap\":1009690,\"mustMixBalanceMax\":1019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":180,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":672969,\"nbConfirmed\":2},{\"poolId\":\"0.001btc\",\"denomination\":100000,\"feeValue\":5000,\"mustMixBalanceMin\":100170,\"mustMixBalanceCap\":109690,\"mustMixBalanceMax\":119125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":25,\"nbRegistered\":157,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":217766,\"nbConfirmed\":2},{\"poolId\":\"0.05btc\",\"denomination\":5000000,\"feeValue\":175000,\"mustMixBalanceMin\":5000170,\"mustMixBalanceCap\":5009690,\"mustMixBalanceMax\":5019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":126,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":4237382,\"nbConfirmed\":2},{\"poolId\":\"0.5btc\",\"denomination\":50000000,\"feeValue\":1750000,\"mustMixBalanceMin\":50000170,\"mustMixBalanceCap\":50009690,\"mustMixBalanceMax\":50019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":34,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":5971543,\"nbConfirmed\":2}]}";
  private static final String WALLET_RESPONSE =
      "{\"wallet\": {\"final_balance\": 116640227},\"info\": {\"fees\": {\"2\": 1,\"4\": 1,\"6\": 1,\"12\": 1,\"24\": 1},\"latest_block\": {\"height\": 2064015,\"hash\": \"00000000000000409297f8e0c0e73475cdd215ef675ad82802a08507b1c1d0e1\",\"time\": 1628498860}},\"addresses\": [{\"address\": \"vpub5YEhBtZy85KxLBxQB4MiHZvjjhz5DcYT9DV2gLshFykuWXjqSzLxpLd4TwS8nFxJmXAX8RrxRxpanndBh5a9AJPbrJEtqCcTKAnRYcP4Aed\",\"final_balance\": 116640227,\"account_index\": 511,\"change_index\": 183,\"n_tx\": 137}],\"txs\": [],\"unspent_outputs\": []}";

  public AbstractTest() throws Exception {
    ClientUtils.setLogLevel(Level.DEBUG, Level.DEBUG);

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
    pool001btc.setPoolId("0.01btc");
    pool001btc.setDenomination(100000);
    pool001btc.setFeeValue(5000);
    pool001btc.setMustMixBalanceMin(100017);
    pool001btc.setMustMixBalanceCap(100950);
    pool001btc.setMustMixBalanceMax(101000);
    pool001btc.setMinAnonymitySet(5);
    pool001btc.setMinMustMix(3);
    pool01btc.setTx0MaxOutputs(70);
    pool001btc.setNbRegistered(0);
    pool001btc.setMixAnonymitySet(5);
    pool001btc.setMixStatus(MixStatus.CONFIRM_INPUT);
    pool001btc.setElapsedTime(1000);
    pool001btc.setNbConfirmed(0);

    pool05btc = new Pool();
    pool05btc.setPoolId("0.5btc");
    pool05btc.setDenomination(5000000);
    pool05btc.setFeeValue(250000);
    pool05btc.setMustMixBalanceMin(5000170);
    pool05btc.setMustMixBalanceCap(5009500);
    pool05btc.setMustMixBalanceMax(5010000);
    pool05btc.setMinAnonymitySet(5);
    pool05btc.setMinMustMix(3);
    pool01btc.setTx0MaxOutputs(70);
    pool05btc.setNbRegistered(0);
    pool05btc.setMixAnonymitySet(5);
    pool05btc.setMixStatus(MixStatus.CONFIRM_INPUT);
    pool05btc.setElapsedTime(1000);
    pool05btc.setNbConfirmed(0);

    resetWalletStateFile();
  }

  protected WalletResponse mockWalletResponse() throws Exception {
    return ClientUtils.fromJson(WALLET_RESPONSE, WalletResponse.class);
  }

  protected Tx0PreviewService mockTx0PreviewService(boolean isOpReturnV0) throws Exception {
    MinerFeeSupplier minerFeeSupplier = mockMinerFeeSupplier();
    return new Tx0PreviewService(
        minerFeeSupplier,
        new ITx0PreviewServiceConfig() {
          @Override
          public NetworkParameters getNetworkParameters() {
            return params;
          }

          @Override
          public Long getOverspend(String poolId) {
            return null;
          }

          @Override
          public int getFeeMin() {
            return 1;
          }

          @Override
          public int getFeeMax() {
            return 9999;
          }

          @Override
          public int getTx0MaxOutputs() {
            return 70;
          }

          @Override
          public ServerApi getServerApi() {
            return null;
          }

          @Override
          public String getPartner() {
            return null;
          }

          @Override
          public String getScode() {
            return null;
          }

          @Override
          public boolean isOpReturnV0() {
            return isOpReturnV0;
          }
        });
  }

  protected ExpirablePoolSupplier mockPoolSupplier() {
    try {
      PoolsResponse poolsResponse = ClientUtils.fromJson(POOLS_RESPONSE, PoolsResponse.class);
      return new MockPoolSupplier(mockTx0PreviewService(false), poolsResponse.pools);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected MinerFeeSupplier mockMinerFeeSupplier() throws Exception {
    BasicMinerFeeSupplier minerFeeSupplier = new BasicMinerFeeSupplier(1, 100);
    minerFeeSupplier.setValue(1);
    return minerFeeSupplier;
  }

  protected IHttpClientService mockHttpClientService() {
    return new IHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return null;
      }

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
    spendFrom.xpub.m =
        "vpub5YEQpEDPAZWVTkmWASSHyaUMsae7uV9FnRrhZ3cqV6RFbBQx7wjVsUfLqSE3hgNY8WQixurkbWNkfV2sRE7LPfNKQh2t3s5une4QZthwdCu";
    return spendFrom;
  }

  protected IHttpClientService computeHttpClientService() {
    return new IHttpClientService() {
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

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    ServerApi serverApi =
        new ServerApi(WhirlpoolServer.TESTNET.getServerUrlClear(), computeHttpClientService()) {
          @Override
          public Observable<PushTxSuccessResponse> pushTx0(Tx0PushRequest request)
              throws Exception {
            // mock pushtx0
            byte[] txBytes = WhirlpoolProtocol.decodeBytes(request.tx64);
            Transaction tx = new Transaction(params, txBytes);
            onPushTx0(request, tx);
            return Observable.just(new PushTxSuccessResponse(tx.getHashAsString()));
          }
        };
    return computeWhirlpoolWalletConfig(serverApi);
  }

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(ServerApi serverApi) {
    DataSourceFactory dataSourceFactory =
        new DojoDataSourceFactory(BackendServer.TESTNET, false, null);
    ISecretPointFactory secretPointFactory = SecretPointFactoryJava.getInstance();
    IHttpClientService httpClientService = computeHttpClientService();
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(
            dataSourceFactory,
            secretPointFactory,
            httpClientService,
            null,
            null,
            serverApi,
            TestNet3Params.get(),
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
}
