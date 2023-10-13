package com.samourai.whirlpool.client.wallet.data.utxo;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxoChangesEvent;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.WalletResponseDataSource;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UtxoSupplierTest extends AbstractTest {
  protected WalletResponseDataSource dataSource;
  protected DataPersister dataPersister;
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;
  protected WalletResponse mockWalletResponse;
  protected boolean mockException;

  private String XPUB_DEPOSIT;
  private String XPUB_PREMIX;
  private String XPUB_POSTMIX;

  protected UnspentOutput UTXO_DEPOSIT1;
  protected UnspentOutput UTXO_DEPOSIT1_UPDATED;
  protected UnspentOutput UTXO_PREMIX1;
  protected UnspentOutput UTXO_PREMIX2;
  protected UnspentOutput UTXO_POSTMIX1;

  protected WhirlpoolUtxoChanges lastUtxoChanges;

  public UtxoSupplierTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
    WhirlpoolEventService.getInstance()
        .register(
            new MessageListener<UtxoChangesEvent>() {
              @Subscribe
              @Override
              public void onMessage(UtxoChangesEvent message) {
                lastUtxoChanges = message.getUtxoData().getUtxoChanges();
              }
            });

    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip44w = hdWalletFactory.getBIP44(seed, SEED_PASSPHRASE, params);

    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, bip44w, "test");

    dataPersister = new FileDataPersisterFactory().createDataPersister(whirlpoolWallet, bip44w);
    dataPersister.load();
    dataPersister.open();
    dataSource =
        new WalletResponseDataSource(
            whirlpoolWallet,
            bip44w,
            dataPersister.getWalletStateSupplier(),
            dataPersister.getUtxoConfigSupplier()) {
          @Override
          protected WalletResponse fetchWalletResponse() throws Exception {
            if (mockException) {
              throw new Exception("utxos not available");
            }
            return mockWalletResponse;
          }

          @Override
          protected ExpirablePoolSupplier computePoolSupplier(
              WhirlpoolWallet whirlpoolWallet, Tx0PreviewService tx0PreviewService) {
            return UtxoSupplierTest.this.poolSupplier;
          }

          @Override
          public IPushTx getPushTx() {
            return new IPushTx() {
              @Override
              public String pushTx(String hexTx) throws Exception {
                // do nothing
                return "txid-test";
              }
            };
          }
        };

    utxoSupplier = dataSource.getUtxoSupplier();
    utxoConfigSupplier = dataPersister.getUtxoConfigSupplier();

    XPUB_DEPOSIT = dataSource.getWalletSupplier().getWallet(BIP_WALLET.DEPOSIT_BIP84).getXPub();
    XPUB_PREMIX = dataSource.getWalletSupplier().getWallet(BIP_WALLET.PREMIX_BIP84).getXPub();
    XPUB_POSTMIX = dataSource.getWalletSupplier().getWallet(BIP_WALLET.POSTMIX_BIP84).getXPub();

    mockException = false;

    UTXO_DEPOSIT1 =
        newUnspentOutput(
            "3f5122c24bd7e7d570c602110c8f133a642433a476f64b5e06b8b2de2edaa2b4", 1, XPUB_DEPOSIT, 0);
    UTXO_DEPOSIT1_UPDATED =
        newUnspentOutput(
            "3f5122c24bd7e7d570c602110c8f133a642433a476f64b5e06b8b2de2edaa2b4", 1, XPUB_DEPOSIT, 2);
    UTXO_PREMIX1 =
        newUnspentOutput(
            "c667c31b0dd971c12ab461b8c76015536dd36a7f66dfded847238aad5c53e6ab", 1, XPUB_PREMIX, 0);
    UTXO_PREMIX2 =
        newUnspentOutput(
            "c667c31b0dd971c12ab461b8c76015536dd36a7f66dfded847238aad5c53e6ab",
            2,
            XPUB_PREMIX,
            100);
    UTXO_POSTMIX1 =
        newUnspentOutput(
            "df81bbe761911938fd598a3e2dc10066951eab552c3c888fd293765cee65ca79",
            1,
            XPUB_POSTMIX,
            50);
  }

  @Test
  public void testValid() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1};
    setMockWalletResponse(utxos1);
    dataSource.open();

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    // mock new data
    UnspentOutput[] utxos2 = new UnspentOutput[] {UTXO_DEPOSIT1_UPDATED, UTXO_PREMIX2};
    setMockWalletResponse(utxos2);
    lastUtxoChanges = null;

    // should use cached data
    doTest(utxos1);
    Assertions.assertEquals(null, lastUtxoChanges);

    // expire data
    utxoSupplier.refresh();

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
    try {
      dataSource.open();
      Assertions.assertTrue(false);
    } catch (Exception e) {
      Assertions.assertEquals("utxos not available", e.getMessage());
    }
  }

  @Test
  public void testSuccessFailureSuccess() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1};
    setMockWalletResponse(utxos1);
    dataSource.open();

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    // expire data
    utxoSupplier.refresh();

    // mock throwing backend
    mockException = true;
    lastUtxoChanges = null;

    // should use initial data
    doTest(utxos1);
    Assertions.assertEquals(null, lastUtxoChanges);
  }

  protected void setMockWalletResponse(UnspentOutput[] unspentOutputs) throws Exception {
    mockWalletResponse = mockWalletResponse();
    mockWalletResponse.unspent_outputs = unspentOutputs;
  }

  protected void doTest(UnspentOutput[] expected) throws Exception {
    assertUtxoEquals(expected, utxoSupplier.findUtxos(WhirlpoolAccount.values()));
  }

  protected void assertUtxoChanges(
      UnspentOutput[] added, UnspentOutput[] confirmed, UnspentOutput[] removed) {
    assertUtxoEquals(added, lastUtxoChanges.getUtxosAdded());
    assertUtxoEquals(confirmed, lastUtxoChanges.getUtxosConfirmed());
    assertUtxoEquals(removed, lastUtxoChanges.getUtxosRemoved());
  }
}
