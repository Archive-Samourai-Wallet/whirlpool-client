package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import java.util.Collection;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class UtxoSupplierTest extends AbstractTest {
  protected WalletDataSupplier walletDataSupplier;
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;
  protected WalletResponse mockWalletResponse;
  protected boolean mockException;

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE = "whirlpool";

  private static final String ZPUB_DEPOSIT =
      "vpub5YEQpEDPAZWVTkmWASSHyaUMsae7uV9FnRrhZ3cqV6RFbBQx7wjVsUfLqSE3hgNY8WQixurkbWNkfV2sRE7LPfNKQh2t3s5une4QZthwdCu";
  private static final String ZPUB_PREMIX =
      "vpub5YEQpEDXWE3TW21vo2zdDK9PZgKqnonnomB2b18dadRTgtnB5F8SZg1reqvMHEDKq1k3oHz1AXbsD6MCfNcw77BqfZxWmZm4nn16XNC84mL";
  private static final String ZPUB_POSTMIX =
      "vpub5YEQpEDXWE3TawqjQNFt5o4sBM1RP1B1mtVZr8ysEA9hFLsZZ4RB8oxE4Sfkumc47jnVPUgRL9hJf3sWpTYBKtdkP3UK6J8p1n2ykmjHnrW";

  protected UnspentOutput UTXO_DEPOSIT1;
  protected UnspentOutput UTXO_DEPOSIT1_UPDATED;
  protected UnspentOutput UTXO_PREMIX1;
  protected UnspentOutput UTXO_PREMIX2;
  protected UnspentOutput UTXO_POSTMIX1;

  protected WhirlpoolUtxoChanges lastUtxoChanges;

  private WalletSupplier computeWalletSupplier() throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet hdWallet = hdWalletFactory.getBIP84(seed, SEED_PASSPHRASE, params);

    BackendApi backendApi =
        new BackendApi(null, "http://testbackend", null) {
          @Override
          public void initBip84(String zpub) throws Exception {
            // mock
          }
        };
    String fileName = "/tmp/walletState";
    resetFile(fileName);
    WalletStatePersister persister = new WalletStatePersister(fileName);
    return new WalletSupplier(persister, backendApi, hdWallet, 0);
  }

  @Before
  public void setup() throws Exception {
    WalletSupplier walletSupplier = computeWalletSupplier();

    MessageListener<WhirlpoolUtxoChanges> changeListener =
        new MessageListener<WhirlpoolUtxoChanges>() {
          @Override
          public void onMessage(WhirlpoolUtxoChanges message) {
            lastUtxoChanges = message;
          }
        };
    String fileName = "/tmp/utxoConfig";
    resetFile(fileName);
    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig();
    walletDataSupplier =
        new WalletDataSupplier(999999, walletSupplier, changeListener, fileName, config) {
          @Override
          protected WalletResponse fetchWalletResponse() throws Exception {
            if (mockException) {
              throw new Exception("utxos not available");
            }
            return mockWalletResponse;
          }

          @Override
          protected PoolSupplier computePoolSupplier(
              WhirlpoolWalletConfig config, Tx0ParamService tx0ParamService) {
            return mockPoolSupplier();
          }
        };
    walletDataSupplier.getPoolSupplier().load();
    walletSupplier.getWalletStateSupplier().load();
    utxoSupplier = walletDataSupplier.getUtxoSupplier();
    utxoConfigSupplier = walletDataSupplier.getUtxoConfigSupplier();
    utxoConfigSupplier.load();

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
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1};
    setMockWalletResponse(utxos1);

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    // mock new data
    UnspentOutput[] utxos2 = new UnspentOutput[] {UTXO_DEPOSIT1_UPDATED, UTXO_PREMIX2};
    setMockWalletResponse(utxos2);
    lastUtxoChanges = null;

    // should use cached data
    doTest(utxos1);
    Assert.assertEquals(null, lastUtxoChanges);

    // expire data
    utxoSupplier.expire();

    // should use fresh data
    doTest(utxos2);
    assertUtxoChanges(
        new UnspentOutput[] {UTXO_PREMIX2},
        new UnspentOutput[] {UTXO_DEPOSIT1_UPDATED},
        new UnspentOutput[] {UTXO_PREMIX1, UTXO_POSTMIX1});
  }

  @Test
  public void testInitialFailure() throws Exception {
    // mock throwing backend
    mockException = true;

    // verify
    Exception e =
        Assert.assertThrows(
            Exception.class,
            new ThrowingRunnable() {
              @Override
              public void run() throws Throwable {
                doTest(new UnspentOutput[] {});
              }
            });

    Assert.assertEquals("utxos not available", e.getMessage());
  }

  @Test
  public void testSuccessFailureSuccess() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1};
    setMockWalletResponse(utxos1);

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    // expire data
    utxoSupplier.expire();

    // mock throwing backend
    mockException = true;
    lastUtxoChanges = null;

    // should use initial data
    doTest(utxos1);
    Assert.assertEquals(null, lastUtxoChanges);
  }

  protected void setMockWalletResponse(UnspentOutput[] unspentOutputs) {
    mockWalletResponse = new WalletResponse();
    mockWalletResponse.info = new WalletResponse.Info();
    mockWalletResponse.unspent_outputs = unspentOutputs;
  }

  protected void doTest(UnspentOutput[] expected) throws Exception {
    walletDataSupplier.load();

    // getUtxos()
    assertUtxoEquals(expected, utxoSupplier.getUtxos());
  }

  private UnspentOutput computeUtxo(String hash, int n, String xpub, int confirms) {
    UnspentOutput utxo = new UnspentOutput();
    utxo.tx_hash = hash;
    utxo.tx_output_n = n;
    utxo.xpub = new UnspentOutput.Xpub();
    utxo.xpub.m = xpub;
    utxo.confirmations = confirms;
    return utxo;
  }

  private void assertUtxoEquals(UnspentOutput[] utxos1, Collection<WhirlpoolUtxo> utxos2) {
    Assert.assertEquals(utxos1.length, utxos2.size());
    for (WhirlpoolUtxo whirlpoolUtxo : utxos2) {
      Assert.assertTrue(ArrayUtils.contains(utxos1, whirlpoolUtxo.getUtxo()));
    }
  }

  protected void assertUtxoChanges(
      UnspentOutput[] added, UnspentOutput[] updated, UnspentOutput[] removed) {
    assertUtxoEquals(added, lastUtxoChanges.getUtxosAdded());
    assertUtxoEquals(updated, lastUtxoChanges.getUtxosUpdated());
    assertUtxoEquals(removed, lastUtxoChanges.getUtxosRemoved());
  }
}
