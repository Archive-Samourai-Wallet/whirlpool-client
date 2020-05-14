package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.ITx0ParamServiceConfig;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.MockPoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java8.util.Lists;
import org.bitcoinj.core.NetworkParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class UtxoSupplierTest extends AbstractTest {
  private BackendApi backendApi;
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;
  protected List<UnspentResponse.UnspentOutput> mockUtxos;
  protected boolean mockException;

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE = "whirlpool";

  private static final String ZPUB_DEPOSIT =
      "vpub5YEQpEDPAZWVTkmWASSHyaUMsae7uV9FnRrhZ3cqV6RFbBQx7wjVsUfLqSE3hgNY8WQixurkbWNkfV2sRE7LPfNKQh2t3s5une4QZthwdCu";
  private static final String ZPUB_PREMIX =
      "vpub5YEQpEDXWE3TW21vo2zdDK9PZgKqnonnomB2b18dadRTgtnB5F8SZg1reqvMHEDKq1k3oHz1AXbsD6MCfNcw77BqfZxWmZm4nn16XNC84mL";
  private static final String ZPUB_POSTMIX =
      "vpub5YEQpEDXWE3TawqjQNFt5o4sBM1RP1B1mtVZr8ysEA9hFLsZZ4RB8oxE4Sfkumc47jnVPUgRL9hJf3sWpTYBKtdkP3UK6J8p1n2ykmjHnrW";

  protected UnspentResponse.UnspentOutput UTXO_DEPOSIT1;
  protected UnspentResponse.UnspentOutput UTXO_DEPOSIT1_UPDATED;
  protected UnspentResponse.UnspentOutput UTXO_PREMIX1;
  protected UnspentResponse.UnspentOutput UTXO_PREMIX2;
  protected UnspentResponse.UnspentOutput UTXO_POSTMIX1;

  protected WhirlpoolUtxoChanges lastUtxoChanges;

  private WalletSupplier computeWalletSupplier() throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet hdWallet = hdWalletFactory.getBIP84(seed, SEED_PASSPHRASE, params);

    return new WalletSupplier(999999, null, backendApi, hdWallet);
  }

  @BeforeEach
  public void setup() throws Exception {
    backendApi =
        new BackendApi(null, "http://testbackend", null) {
          @Override
          public List<UnspentResponse.UnspentOutput> fetchUtxos(String[] zpubs) throws Exception {
            if (mockException) {
              throw new Exception("utxos not available");
            }
            return mockUtxos;
          }
        };

    WalletSupplier walletSupplier = computeWalletSupplier();

    MinerFeeSupplier minerFeeSupplier = new MinerFeeSupplier(9999999, backendApi, 10, 10000, 123);
    // minerFeeSupplier.load();

    PoolSupplier poolSupplier = new MockPoolSupplier();
    poolSupplier.load();

    String fileName = "/tmp/utxoConfig";
    File f = new File(fileName);
    if (f.exists()) {
      f.delete();
    }
    f.createNewFile();
    UtxoConfigPersister persister = new UtxoConfigPersister(fileName);
    Tx0ParamService tx0ParamService =
        new Tx0ParamService(
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

              @Override
              public MinerFeeTarget getFeeTargetPremix() {
                return MinerFeeTarget.BLOCKS_4;
              }
            });
    utxoConfigSupplier = new UtxoConfigSupplier(persister, poolSupplier, tx0ParamService);
    utxoConfigSupplier.load();
    MessageListener<WhirlpoolUtxoChanges> changeListener =
        new MessageListener<WhirlpoolUtxoChanges>() {
          @Override
          public void onMessage(WhirlpoolUtxoChanges message) {
            lastUtxoChanges = message;
          }
        };
    utxoSupplier =
        new UtxoSupplier(9999999, walletSupplier, utxoConfigSupplier, backendApi, changeListener);
    mockException = false;

    UTXO_DEPOSIT1 = computeUtxo("deposit1", 1, ZPUB_DEPOSIT, 1);
    UTXO_DEPOSIT1_UPDATED = computeUtxo("deposit1", 1, ZPUB_DEPOSIT, 2);
    UTXO_PREMIX1 = computeUtxo("premix1", 1, ZPUB_PREMIX, 0);
    UTXO_PREMIX2 = computeUtxo("premix2", 2, ZPUB_PREMIX, 100);
    UTXO_POSTMIX1 = computeUtxo("postmix1", 1, ZPUB_POSTMIX, 50);
  }

  @Test
  public void testValid() throws Exception {
    // mock initial data
    List<UnspentResponse.UnspentOutput> utxos1 =
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1});
    mockUtxos = utxos1;

    // verify
    doTest(utxos1);
    assertUtxoChanges(
        utxos1,
        Lists.of(new UnspentResponse.UnspentOutput[] {}),
        Lists.of(new UnspentResponse.UnspentOutput[] {}));

    // mock new data
    List<UnspentResponse.UnspentOutput> utxos2 =
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_DEPOSIT1_UPDATED, UTXO_PREMIX2});
    mockUtxos = utxos2;
    lastUtxoChanges = null;

    // should use cached data
    doTest(utxos1);
    Assertions.assertEquals(null, lastUtxoChanges);

    // expire data
    utxoSupplier.expire();

    // should use fresh data
    doTest(utxos2);
    assertUtxoChanges(
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_PREMIX2}),
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_DEPOSIT1_UPDATED}),
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_PREMIX1, UTXO_POSTMIX1}));
  }

  @Test
  public void testInitialFailure() throws Exception {
    // mock throwing backend
    mockException = true;

    // verify
    Exception e =
        Assertions.assertThrows(
            Exception.class,
            new Executable() {
              @Override
              public void execute() throws Throwable {
                doTest(new ArrayList<UnspentResponse.UnspentOutput>());
              }
            });

    Assertions.assertEquals("utxos not available", e.getMessage());
  }

  @Test
  public void testSuccessFailureSuccess() throws Exception {
    // mock initial data
    List<UnspentResponse.UnspentOutput> utxos1 =
        Lists.of(new UnspentResponse.UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1});
    mockUtxos = utxos1;

    // verify
    doTest(utxos1);
    assertUtxoChanges(
        utxos1,
        Lists.of(new UnspentResponse.UnspentOutput[] {}),
        Lists.of(new UnspentResponse.UnspentOutput[] {}));

    // expire data
    utxoSupplier.expire();

    // mock throwing backend
    mockException = true;
    lastUtxoChanges = null;

    // should use initial data
    doTest(utxos1);
    Assertions.assertEquals(null, lastUtxoChanges);
  }

  protected void doTest(List<UnspentResponse.UnspentOutput> expected) throws Exception {
    utxoSupplier.load();

    // getUtxos()
    assertUtxoEquals(expected, utxoSupplier.getUtxos());
  }

  private UnspentResponse.UnspentOutput computeUtxo(String hash, int n, String xpub, int confirms) {
    UnspentResponse.UnspentOutput utxo = new UnspentResponse.UnspentOutput();
    utxo.tx_hash = hash;
    utxo.tx_output_n = n;
    utxo.xpub = new UnspentResponse.UnspentOutput.Xpub();
    utxo.xpub.m = xpub;
    utxo.confirmations = confirms;
    return utxo;
  }

  private void assertUtxoEquals(
      Collection<UnspentResponse.UnspentOutput> utxos1, Collection<WhirlpoolUtxo> utxos2) {
    Assertions.assertEquals(utxos1.size(), utxos2.size());
    for (WhirlpoolUtxo whirlpoolUtxo : utxos2) {
      Assertions.assertTrue(utxos1.contains(whirlpoolUtxo.getUtxo()));
    }
  }

  protected void assertUtxoChanges(
      Collection<UnspentResponse.UnspentOutput> added,
      Collection<UnspentResponse.UnspentOutput> updated,
      Collection<UnspentResponse.UnspentOutput> removed) {
    assertUtxoEquals(added, lastUtxoChanges.getUtxosAdded());
    assertUtxoEquals(updated, lastUtxoChanges.getUtxosUpdated());
    assertUtxoEquals(removed, lastUtxoChanges.getUtxosRemoved());
  }
}
