package com.samourai.whirlpool.client.wallet.data.utxo;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxoChangesEvent;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.WalletResponseDataSource;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UtxoSupplierTest extends AbstractTest {
  protected WalletResponseDataSource dataSource;
  protected DataPersister dataPersister;
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;
  protected WalletResponse mockWalletResponse;
  protected boolean mockException;

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

  @Before
  public void setup() throws Exception {
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

    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(null);
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(config, bip44w, "test");

    dataPersister = new FileDataPersisterFactory().createDataPersister(whirlpoolWallet, bip44w);
    dataPersister.load();
    dataPersister.open();
    dataSource =
        new WalletResponseDataSource(whirlpoolWallet, bip44w, dataPersister) {
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
            return mockPoolSupplier();
          }

          @Override
          public void pushTx(String txHex) throws Exception {
            // do nothing
          }
        };

    utxoSupplier = dataSource.getUtxoSupplier();
    utxoConfigSupplier = dataPersister.getUtxoConfigSupplier();

    mockException = false;

    UTXO_DEPOSIT1 = computeUtxo("deposit1", 1, ZPUB_DEPOSIT, 0);
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
    Assert.assertEquals(null, lastUtxoChanges);

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
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertEquals("utxos not available", e.getMessage());
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
    Assert.assertEquals(null, lastUtxoChanges);
  }

  protected void setMockWalletResponse(UnspentOutput[] unspentOutputs) throws Exception {
    mockWalletResponse = mockWalletResponse();
    mockWalletResponse.unspent_outputs = unspentOutputs;
  }

  protected void doTest(UnspentOutput[] expected) throws Exception {
    assertUtxoEquals(expected, utxoSupplier.findUtxos(WhirlpoolAccount.values()));
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

    List<String> utxos1Ids =
        StreamSupport.stream(Arrays.asList(utxos1))
            .map(
                new Function<UnspentOutput, String>() {
                  @Override
                  public String apply(UnspentOutput utxo) {
                    return computeUtxoId(utxo);
                  }
                })
            .collect(Collectors.<String>toList());
    for (WhirlpoolUtxo whirlpoolUtxo : utxos2) {
      // search utxo by id
      Assert.assertTrue(utxos1Ids.contains(computeUtxoId(whirlpoolUtxo.getUtxo())));
    }
  }

  private String computeUtxoId(UnspentOutput utxo) {
    return utxo.tx_hash + ':' + utxo.tx_output_n;
  }

  protected void assertUtxoChanges(
      UnspentOutput[] added, UnspentOutput[] confirmed, UnspentOutput[] removed) {
    assertUtxoEquals(added, lastUtxoChanges.getUtxosAdded());
    assertUtxoEquals(confirmed, lastUtxoChanges.getUtxosConfirmed());
    assertUtxoEquals(removed, lastUtxoChanges.getUtxosRemoved());
  }
}
