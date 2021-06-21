package com.samourai.whirlpool.client.test;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
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

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(null, null, null, null, TestNet3Params.get(), false, null, null);
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
