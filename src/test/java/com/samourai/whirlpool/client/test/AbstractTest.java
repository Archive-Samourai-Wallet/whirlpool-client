package com.samourai.whirlpool.client.test;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.tx0.ITx0ParamServiceConfig;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.MockPoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import java.io.File;
import java.util.Collection;
import java8.util.Lists;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTest {
  protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected NetworkParameters params = TestNet3Params.get();
  protected HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();
  protected Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  protected Pool pool01btc;
  protected Pool pool05btc;
  protected Pool pool001btc;
  private static final String POOLS_RESPONSE =
      "{\"pools\":[{\"poolId\":\"0.01btc\",\"denomination\":1000000,\"feeValue\":50000,\"mustMixBalanceMin\":1000170,\"mustMixBalanceCap\":1009690,\"mustMixBalanceMax\":1019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":180,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":672969,\"nbConfirmed\":2},{\"poolId\":\"0.001btc\",\"denomination\":100000,\"feeValue\":5000,\"mustMixBalanceMin\":100170,\"mustMixBalanceCap\":109690,\"mustMixBalanceMax\":119125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":25,\"nbRegistered\":157,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":217766,\"nbConfirmed\":2},{\"poolId\":\"0.05btc\",\"denomination\":5000000,\"feeValue\":175000,\"mustMixBalanceMin\":5000170,\"mustMixBalanceCap\":5009690,\"mustMixBalanceMax\":5019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":126,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":4237382,\"nbConfirmed\":2},{\"poolId\":\"0.5btc\",\"denomination\":50000000,\"feeValue\":1750000,\"mustMixBalanceMin\":50000170,\"mustMixBalanceCap\":50009690,\"mustMixBalanceMax\":50019125,\"minAnonymitySet\":5,\"minMustMix\":2,\"tx0MaxOutputs\":70,\"nbRegistered\":34,\"mixAnonymitySet\":5,\"mixStatus\":\"CONFIRM_INPUT\",\"elapsedTime\":5971543,\"nbConfirmed\":2}]}";

  public AbstractTest() {
    pool01btc = new Pool();
    pool01btc.setPoolId("0.1btc");
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
  }

  protected Tx0ParamService mockTx0ParamService() {
    MinerFeeSupplier minerFeeSupplier = mockMinerFeeSupplier();
    return new Tx0ParamService(
        minerFeeSupplier,
        new ITx0ParamServiceConfig() {
          @Override
          public NetworkParameters getNetworkParameters() {
            return params;
          }

          @Override
          public Long getOverspend(String poolId) {
            return null;
          }
        });
  }

  protected PoolSupplier mockPoolSupplier() {
    try {
      PoolsResponse poolsResponse = ClientUtils.fromJson(POOLS_RESPONSE, PoolsResponse.class);
      return new MockPoolSupplier(mockTx0ParamService(), poolsResponse.pools);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected MinerFeeSupplier mockMinerFeeSupplier() {
    return new MinerFeeSupplier(1, 100, 1);
  }

  protected Collection<Pool> getPools() {
    return Lists.of(pool001btc, pool01btc, pool05btc);
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
    spendFrom.xpub.path = "foo";
    return spendFrom;
  }

  protected IHttpClientService mockHttpClientService() {
    return new IHttpClientService() {
      @Override
      public IHttpClient getHttpClient(HttpUsage httpUsage) {
        return null;
      }
    };
  }

  /*
    protected WhirlpoolUtxo newUtxo(
        String poolId, WhirlpoolAccount whirlpoolAccount, String hash, int confirms, Long lastError) {
      UnspentOutput utxo = newUnspentOutput(hash, 3, 100L);
      utxo.confirmations = confirms;
      WhirlpoolUtxoConfig utxoConfig =
          new WhirlpoolUtxoConfig(poolId, 5, 0, System.currentTimeMillis());
      WhirlpoolUtxo whirlpoolUtxo =
          new WhirlpoolUtxo(utxo, whirlpoolAccount, utxoConfig, WhirlpoolUtxoStatus.READY);
      whirlpoolUtxo.getUtxoState().setLastError(lastError);
      return whirlpoolUtxo;
    }
  */
  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(null, null, null, null, TestNet3Params.get(), false, null);
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
}
