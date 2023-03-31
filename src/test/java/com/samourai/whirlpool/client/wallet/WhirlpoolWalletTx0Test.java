package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.tx0.AbstractTx0ServiceTest;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletTx0Test extends AbstractTx0ServiceTest {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0Test.class);

  public WhirlpoolWalletTx0Test() throws Exception {
    super(46);
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void tx0() throws Exception {
    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            5432999,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);

    // run
    Tx0 tx0 = whirlpoolWallet.tx0(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(1, tx0.getNbPremix());
    Assertions.assertTrue(
        utxosContains(tx0.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // current wallet utxos should be mocked from tx0 outputs
    whirlpoolWallet.refreshUtxosAsync().blockingAwait();
    for (TransactionOutput txOut : tx0.getChangeOutputs()) {
      String hash = tx0.getTx().getHashAsString();
      int index = txOut.getIndex();
      Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
      Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
    }
  }

  @Test
  public void tx0_cascading() throws Exception {
    log.info("Testing 0.05432999 btc. Makes Tx0s for pools 0.05 & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            5432999,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    Tx0 tx0_pool05 = whirlpoolWallet.tx0(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    log.info("tx0_pool05 = " + tx0_pool05);

    Assertions.assertEquals(1, tx0_pool05.getNbPremix());

    // tx0_pool05 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(
            tx0_pool05.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));
  }

  @Test
  public void runTx0Cascade_test0() throws Exception {
    log.info("Testing 0.05432999 btc. Makes Tx0s for pools 0.05 & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            5432999,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.runTx0Cascade(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(2, tx0s.size());
    Tx0 tx0_pool05 = tx0s.get(0);
    Tx0 tx0_pool001 = tx0s.get(1);

    log.info("tx0_pool05 = " + tx0_pool05);
    log.info("tx0_pool001 = " + tx0_pool001);

    Assertions.assertTrue(tx0_pool05.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool001.getNbPremix() > 0);

    int totalNbPremix = tx0_pool05.getNbPremix() + tx0_pool001.getNbPremix();
    Assertions.assertEquals(3, totalNbPremix);
    log.info("Total nbPremix: " + totalNbPremix);

    // tx0_pool05 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(
            tx0_pool05.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // tx0_pool001 spends from tx0_pool01 outputs
    Assertions.assertEquals(
        tx0_pool05.getChangeOutputs().size(), tx0_pool001.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool05.getChangeOutputs()) {
      String txid = tx0_pool05.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool001.getSpendFroms(), txid, txOut.getIndex()));
    }
  }

  @Test
  public void runTx0Cascade_test1() throws Exception {
    log.info("Testing 0.06432999 btc. Makes Tx0s for pools 0.05, 0.01, & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            6432999,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.runTx0Cascade(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(3, tx0s.size());
    Tx0 tx0_pool05 = tx0s.get(0);
    Tx0 tx0_pool01 = tx0s.get(1);
    Tx0 tx0_pool001 = tx0s.get(2);

    log.info("tx0_pool05 = " + tx0_pool05);
    log.info("tx0_pool01 = " + tx0_pool01);
    log.info("tx0_pool001 = " + tx0_pool001);

    Assertions.assertTrue(tx0_pool05.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool01.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool001.getNbPremix() > 0);

    int totalNbPremix =
        tx0_pool05.getNbPremix() + tx0_pool01.getNbPremix() + tx0_pool001.getNbPremix();
    Assertions.assertEquals(4, totalNbPremix);
    log.info("Total nbPremix: " + totalNbPremix);

    // tx0_pool05 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(
            tx0_pool05.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // tx0_pool01 spends from tx0_pool05 outputs
    Assertions.assertEquals(
        tx0_pool05.getChangeOutputs().size(), tx0_pool01.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool05.getChangeOutputs()) {
      String txid = tx0_pool05.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool01.getSpendFroms(), txid, txOut.getIndex()));
    }

    // tx0_pool001 spends from tx0_pool01 outputs
    Assertions.assertEquals(
        tx0_pool01.getChangeOutputs().size(), tx0_pool001.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool01.getChangeOutputs()) {
      String txid = tx0_pool01.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool001.getSpendFroms(), txid, txOut.getIndex()));
    }
  }

  @Test
  public void runTx0Cascade_test2() throws Exception {
    log.info("Testing 0.74329991 btc. Makes Tx0s for pools 0.5, 0.05, 0.01, & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            74329991,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.5btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.runTx0Cascade(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(4, tx0s.size());
    Tx0 tx0_pool5 = tx0s.get(0);
    Tx0 tx0_pool05 = tx0s.get(1);
    Tx0 tx0_pool01 = tx0s.get(2);
    Tx0 tx0_pool001 = tx0s.get(3);

    log.info("tx0_pool5 = " + tx0_pool5);
    log.info("tx0_pool05 = " + tx0_pool05);
    log.info("tx0_pool01 = " + tx0_pool01);
    log.info("tx0_pool001 = " + tx0_pool001);

    Assertions.assertTrue(tx0_pool5.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool05.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool01.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool001.getNbPremix() > 0);

    int totalNbPremix =
        tx0_pool5.getNbPremix()
            + tx0_pool05.getNbPremix()
            + tx0_pool01.getNbPremix()
            + tx0_pool001.getNbPremix();
    Assertions.assertEquals(13, totalNbPremix);
    log.info("Total nbPremix: " + totalNbPremix);

    // tx0_pool5 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(tx0_pool5.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // tx0_pool05 spends from tx0_pool5 outputs
    Assertions.assertEquals(tx0_pool5.getChangeOutputs().size(), tx0_pool05.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool5.getChangeOutputs()) {
      String txid = tx0_pool5.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool05.getSpendFroms(), txid, txOut.getIndex()));
    }

    // tx0_pool01 spends from tx0_pool05 outputs
    Assertions.assertEquals(
        tx0_pool05.getChangeOutputs().size(), tx0_pool01.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool05.getChangeOutputs()) {
      String txid = tx0_pool05.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool01.getSpendFroms(), txid, txOut.getIndex()));
    }

    // tx0_pool001 spends from tx0_pool01 outputs
    Assertions.assertEquals(
        tx0_pool01.getChangeOutputs().size(), tx0_pool001.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool01.getChangeOutputs()) {
      String txid = tx0_pool01.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool001.getSpendFroms(), txid, txOut.getIndex()));
    }
  }

  @Test
  public void runTx0Cascade_test3() throws Exception {
    log.info("Testing 0.52329991 btc. Makes Tx0s for pools 0.5 & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            52329991,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.5btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.runTx0Cascade(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(2, tx0s.size());
    Tx0 tx0_pool5 = tx0s.get(0);
    Tx0 tx0_pool001 = tx0s.get(1);

    log.info("tx0_pool5 = " + tx0_pool5);
    log.info("tx0_pool001 = " + tx0_pool001);

    Assertions.assertTrue(tx0_pool5.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool001.getNbPremix() > 0);

    int totalNbPremix = tx0_pool5.getNbPremix() + tx0_pool001.getNbPremix();
    Assertions.assertEquals(9, totalNbPremix);
    log.info("Total nbPremix: " + totalNbPremix);

    // tx0_pool5 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(tx0_pool5.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // tx0_pool001 spends from tx0_pool5 outputs
    Assertions.assertEquals(
        tx0_pool5.getChangeOutputs().size(), tx0_pool001.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool5.getChangeOutputs()) {
      String txid = tx0_pool5.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool001.getSpendFroms(), txid, txOut.getIndex()));
    }
  }

  @Test
  public void runTx0Cascade_test4() throws Exception {
    log.info("Testing 0.02329991 btc. Makes Tx0s for pools 0.01 & 0.001");

    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

    // mock initial data
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            2329991,
            address);
    mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.runTx0Cascade(Arrays.asList(spendFromUtxo), tx0Config, pool);

    // verify
    Assertions.assertEquals(2, tx0s.size());
    Tx0 tx0_pool01 = tx0s.get(0);
    Tx0 tx0_pool001 = tx0s.get(1);

    log.info("tx0_pool5 = " + tx0_pool01);
    log.info("tx0_pool001 = " + tx0_pool001);

    Assertions.assertTrue(tx0_pool01.getNbPremix() > 0);
    Assertions.assertTrue(tx0_pool001.getNbPremix() > 0);

    int totalNbPremix = tx0_pool01.getNbPremix() + tx0_pool001.getNbPremix();
    Assertions.assertEquals(4, totalNbPremix);
    log.info("Total nbPremix: " + totalNbPremix);

    // tx0_pool01 spends from spendFroms
    Assertions.assertTrue(
        utxosContains(
            tx0_pool01.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));

    // tx0_pool001 spends from tx0_pool01 outputs
    Assertions.assertEquals(
        tx0_pool01.getChangeOutputs().size(), tx0_pool001.getSpendFroms().size());
    for (TransactionOutput txOut : tx0_pool01.getChangeOutputs()) {
      String txid = tx0_pool01.getTx().getHashAsString();
      Assertions.assertTrue(utxosContains(tx0_pool001.getSpendFroms(), txid, txOut.getIndex()));
    }
  }

  @Disabled // uncomment to manually broadcast a new tx0Cascade
  @Test
  public void manualTx0Cascade() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";

    // init whirlpoolWallet
    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    ServerApi serverApi =
        new ServerApi(WhirlpoolServer.TESTNET.getServerUrlClear(), computeHttpClientService());
    WhirlpoolWalletConfig whirlpoolWalletConfig = computeWhirlpoolWalletConfig(serverApi);
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, seed, passphrase);
    whirlpoolWallet = whirlpoolWalletService.openWallet(whirlpoolWallet, passphrase);
    log.info("Deposit address: " + whirlpoolWallet.getDepositAddress(false));

    // configure TX0
    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();
    Pool pool = poolSupplier.findPoolById("0.01btc");
    log.info("Min deposit for TX0: " + pool.computePremixBalanceMin(false));
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true);

    // run
    Collection<WhirlpoolUtxo> spendFroms =
        whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT);
    Tx0 firstTx0 = whirlpoolWallet.tx0(spendFroms, pool, tx0Config);

    log.info("Tx0: " + firstTx0.getSpendFroms() + " " + firstTx0.getTx());
  }

  @Disabled // uncomment to manually broadcast a new fake tx0Cascade
  @Test
  public void manualTx0FakeCascade() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";

    // init whirlpoolWallet
    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    ServerApi serverApi =
        new ServerApi(WhirlpoolServer.TESTNET.getServerUrlClear(), computeHttpClientService());
    WhirlpoolWalletConfig whirlpoolWalletConfig = computeWhirlpoolWalletConfig(serverApi);
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, seed, passphrase);
    whirlpoolWallet = whirlpoolWalletService.openWallet(whirlpoolWallet, passphrase);
    log.info("Deposit address: " + whirlpoolWallet.getDepositAddress(false));

    // configure TX0
    PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();
    Pool pool = poolSupplier.findPoolById("0.001btc");
    log.info("Min deposit for TX0: " + pool.computePremixBalanceMin(false));
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setCascading(true); // fake TX0 cascading

    // run
    Collection<WhirlpoolUtxo> spendFroms =
        whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT);
    Tx0 tx0 = whirlpoolWallet.tx0(spendFroms, pool, tx0Config);
    log.info("Tx0: " + tx0.getSpendFroms() + " " + tx0.getTx());
  }

  private boolean utxosContains(Collection<UnspentOutput> unspentOutputs, String hash, int index) {
    return unspentOutputs.stream()
            .filter(
                unspentOutput ->
                    unspentOutput.tx_hash.equals(hash) && index == unspentOutput.tx_output_n)
            .count()
        > 0;
  }

  @Override
  protected void onPushTx0(Tx0PushRequest request, Transaction tx) throws Exception {
    super.onPushTx0(request, tx);

    // mock utxos from tx0 outputs
    List<UnspentOutput> unspentOutputs = new LinkedList<>();
    for (TransactionOutput txOut : tx.getOutputs()) {
      HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
      UnspentOutput unspentOutput =
          newUnspentOutput(
              tx.getHashAsString(), txOut.getIndex(), txOut.getValue().getValue(), address);
      unspentOutputs.add(unspentOutput);
    }
    mockUtxos(unspentOutputs.toArray(new UnspentOutput[] {}));
  }
}
