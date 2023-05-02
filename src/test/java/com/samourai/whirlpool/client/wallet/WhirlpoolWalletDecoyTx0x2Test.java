package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WhirlpoolWalletDecoyTx0x2Test extends WhirlpoolWalletTx0Test {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0Test.class);

    public WhirlpoolWalletDecoyTx0x2Test() throws Exception {
        super();
    }

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
    }

    /**
     * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2()}
     */
    @Test
    public void tx0x2_decoy() throws Exception {
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                10000000,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                20000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Pool pool = poolSupplier.findPoolById("0.01btc");
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(spendFroms, tx0Config, pool);

        // verify
        log.info("decoyTx0x2 = " + decoyTx0x2);
        int nbPremix = 28;
        int expectedOutputs = nbPremix + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(nbPremix, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(expectedOutputs, decoyTx0x2.getTx().getOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));

        Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
        long changeValue = decoyTx0x2.getChangeValue();
        long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
        long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // current wallet utxos should be mocked from tx0 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : decoyTx0x2.getChangeOutputs()) {
            String hash = decoyTx0x2.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    @Test
    public void tx0x2_decoy1() throws Exception {
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                2329991,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                3000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Pool pool = poolSupplier.findPoolById("0.01btc");
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(spendFroms, tx0Config, pool);

        // verify
        log.info("decoyTx0x2 = " + decoyTx0x2);
        int nbPremix = 4;
        int expectedOutputs = nbPremix + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(nbPremix, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(expectedOutputs, decoyTx0x2.getTx().getOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));

        Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
        long changeValue = decoyTx0x2.getChangeValue();
        long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
        long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // current wallet utxos should be mocked from tx0 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : decoyTx0x2.getChangeOutputs()) {
            String hash = decoyTx0x2.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    @Test
    public void tx0x2_decoy_fail() throws Exception {
        log.info("Decoy tx0x2 failure due to not having required amount. Should create normal tx0.");
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        Collection<UnspentOutput> utxos = new ArrayList<>();
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                2329991,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                300000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);
        utxos.add(spendFromUtxo0);
        utxos.add(spendFromUtxo1);

        // configure TX0
        Pool pool = poolSupplier.findPoolById("0.01btc");
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set Decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(utxos, tx0Config, pool);

        log.info("decoyTx0x2 = " + decoyTx0x2);
        Assertions.assertEquals(2, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(1, decoyTx0x2.getChangeOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));
    }

    @Test
    public void tx0x2_decoy_fail_singleUTXO() throws Exception {
        log.info("Decoy tx0x2 failure due to only having 1 utxo. Should create normal tx0.");
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
        tx0Config.setDecoyTx0x2(true); // set Decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(Arrays.asList(spendFromUtxo), tx0Config, pool);

        log.info("decoyTx0x2 = " + decoyTx0x2);
        Assertions.assertEquals(2, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(1, decoyTx0x2.getChangeOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo.tx_hash, spendFromUtxo.tx_output_n));
    }

    @Test
    public void tx0x2_decoy_cascade() throws Exception {
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                2329991,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                3000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Collection<Pool> pools = findPoolsLowerOrEqual("0.01btc", poolSupplier);
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        List<Tx0> decoyTx0x2s = whirlpoolWallet.tx0Cascade(spendFroms, tx0Config, pools);

        // verify
        log.info("decoyTx0x2s = " + decoyTx0x2s);
        Assertions.assertEquals(2, decoyTx0x2s.size());

        // 0.01 pool
        Tx0 tx0x2_pool01 = decoyTx0x2s.get(0);
        log.info("tx0_pool01 = " + tx0x2_pool01);

        int nbPremixSender = 4;
        Assertions.assertTrue(tx0x2_pool01.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool01.getNbPremix());
        Assertions.assertTrue(tx0x2_pool01.getPremixOutputs().size() > 0);

        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool01.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool01.getChangeOutputs().size());
        long changeValue = tx0x2_pool01.getChangeValue();
        long changeValueA = tx0x2_pool01.getChangeOutputs().get(0).getValue().value;
        long changeValueB = tx0x2_pool01.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // 0.001 pool
        Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
        log.info("tx0_pool001 = " + tx0x2_pool001);

        nbPremixSender = 12;
        Assertions.assertTrue(tx0x2_pool001.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool001.getNbPremix());
        Assertions.assertTrue(tx0x2_pool001.getPremixOutputs().size() > 0);

        expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool001.getTx().getOutputs().size());


        Assertions.assertEquals(2, tx0x2_pool001.getChangeOutputs().size());
        changeValue = tx0x2_pool001.getChangeValue();
        changeValueA = tx0x2_pool001.getChangeOutputs().get(0).getValue().value;
        changeValueB = tx0x2_pool001.getChangeOutputs().get(1).getValue().value;
        changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValueA, changeValueB); // split evenly
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // tx0x2_pool01 spends from spendFroms
        Assertions.assertTrue(
            utxosContains(
                tx0x2_pool01.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));

        // tx0x2_pool001 spends from tx0_pool01 outputs
        Assertions.assertEquals(
            tx0x2_pool01.getChangeOutputs().size(), tx0x2_pool001.getSpendFroms().size());
        for (TransactionOutput txOut : tx0x2_pool01.getChangeOutputs()) {
            String hash = tx0x2_pool01.getTx().getHashAsString();
            Assertions.assertTrue(utxosContains(tx0x2_pool001.getSpendFroms(), hash, txOut.getIndex()));
        }

        // current wallet utxos should be mocked from tx0x2_pool001 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : tx0x2_pool001.getChangeOutputs()) {
            String hash = tx0x2_pool001.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    /**
     * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_pool001()}
     */
    @Test
    public void tx0x2_decoy_pool001() throws Exception {
        log.info("Testing Decoy Tx0x2 for pool 0.001");
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                500000,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                1000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Pool pool = poolSupplier.findPoolById("0.001btc");
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(spendFroms, tx0Config, pool);

        // verify
        log.info("decoyTx0x2 = " + decoyTx0x2);
        int nbPremix = 13;
        int expectedOutputs = nbPremix + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(nbPremix, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(expectedOutputs, decoyTx0x2.getTx().getOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));

        Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
        long changeValue = decoyTx0x2.getChangeValue();
        long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
        long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // current wallet utxos should be mocked from tx0 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : decoyTx0x2.getChangeOutputs()) {
            String hash = decoyTx0x2.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    /**
     * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool01()}
     * Change values might differ slightly for lower pools due fake samourai "fee" back to self
     */
    @Test
    public void tx0x2_decoy_cascade_pool01() throws Exception {
        log.info("Testing Decoy Tx0x2s for pools 0.01 & 0.001");
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                10000000,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                20000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Collection<Pool> pools = findPoolsLowerOrEqual("0.01btc", poolSupplier);
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        List<Tx0> decoyTx0x2s = whirlpoolWallet.tx0Cascade(spendFroms, tx0Config, pools);

        // verify
        log.info("decoyTx0x2s = " + decoyTx0x2s);
        Assertions.assertEquals(2, decoyTx0x2s.size());

        // 0.01 pool
        Tx0 tx0x2_pool01 = decoyTx0x2s.get(0);
        log.info("tx0_pool01 = " + tx0x2_pool01);

        int nbPremixSender = 28;
        Assertions.assertTrue(tx0x2_pool01.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool01.getNbPremix());
        Assertions.assertTrue(tx0x2_pool01.getPremixOutputs().size() > 0);

        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool01.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool01.getChangeOutputs().size());
        long changeValue = tx0x2_pool01.getChangeValue();
        long changeValueA = tx0x2_pool01.getChangeOutputs().get(0).getValue().value;
        long changeValueB = tx0x2_pool01.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // 0.001 pool
        Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
        log.info("tx0_pool001 = " + tx0x2_pool001);

        nbPremixSender = 18;
        Assertions.assertTrue(tx0x2_pool001.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool001.getNbPremix());
        Assertions.assertTrue(tx0x2_pool001.getPremixOutputs().size() > 0);

        expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool001.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool001.getChangeOutputs().size());
        changeValue = tx0x2_pool001.getChangeValue();
        changeValueA = tx0x2_pool001.getChangeOutputs().get(0).getValue().value;
        changeValueB = tx0x2_pool001.getChangeOutputs().get(1).getValue().value;
        changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValueA, changeValueB); // split evenly
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // tx0x2_pool01 spends from spendFroms
        Assertions.assertTrue(
            utxosContains(
                tx0x2_pool01.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));

        // tx0x2_pool001 spends from tx0_pool01 outputs
        Assertions.assertEquals(
            tx0x2_pool01.getChangeOutputs().size(), tx0x2_pool001.getSpendFroms().size());
        for (TransactionOutput txOut : tx0x2_pool01.getChangeOutputs()) {
            String hash = tx0x2_pool01.getTx().getHashAsString();
            Assertions.assertTrue(utxosContains(tx0x2_pool001.getSpendFroms(), hash, txOut.getIndex()));
        }

        // current wallet utxos should be mocked from tx0x2_pool001 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : tx0x2_pool001.getChangeOutputs()) {
            String hash = tx0x2_pool001.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    /**
     * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool05()}
     * Change values might differ slightly for lower pools due fake samourai "fee" back to self
     */
    @Test
    public void tx0x2_decoy_cascade_pool05() throws Exception {
        log.info("Testing Decoy Tx0x2s for pools 0.05, 0.01, & 0.001");
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                10000000,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                20000000,
                address1);
        mockUtxos(spendFromUtxo0, spendFromUtxo1);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);

        // configure TX0
        Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", poolSupplier);
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        List<Tx0> decoyTx0x2s = whirlpoolWallet.tx0Cascade(spendFroms, tx0Config, pools);

        // verify
        log.info("decoyTx0x2s = " + decoyTx0x2s);
        Assertions.assertEquals(3, decoyTx0x2s.size());

        // 0.05 pool
        Tx0 tx0x2_pool05 = decoyTx0x2s.get(0);
        log.info("tx0_pool05 = " + tx0x2_pool05);

        int nbPremixSender = 4;
        Assertions.assertTrue(tx0x2_pool05.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool05.getNbPremix());
        Assertions.assertTrue(tx0x2_pool05.getPremixOutputs().size() > 0);

        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool05.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool05.getChangeOutputs().size());
        long changeValue = tx0x2_pool05.getChangeValue();
        long changeValueA = tx0x2_pool05.getChangeOutputs().get(0).getValue().value;
        long changeValueB = tx0x2_pool05.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // 0.01 pool
        Tx0 tx0x2_pool01 = decoyTx0x2s.get(1);
        log.info("tx0_pool01 = " + tx0x2_pool01);

        nbPremixSender = 8;
        Assertions.assertTrue(tx0x2_pool01.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool01.getNbPremix());
        Assertions.assertTrue(tx0x2_pool01.getPremixOutputs().size() > 0);

        expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool01.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool01.getChangeOutputs().size());
        changeValue = tx0x2_pool01.getChangeValue();
        changeValueA = tx0x2_pool01.getChangeOutputs().get(0).getValue().value;
        changeValueB = tx0x2_pool01.getChangeOutputs().get(1).getValue().value;
        changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // 0.001 pool
        Tx0 tx0x2_pool001 = decoyTx0x2s.get(2);
        log.info("tx0_pool001 = " + tx0x2_pool001);

        nbPremixSender = 17;
        Assertions.assertTrue(tx0x2_pool001.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool001.getNbPremix());
        Assertions.assertTrue(tx0x2_pool001.getPremixOutputs().size() > 0);

        expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool001.getTx().getOutputs().size());

        Assertions.assertEquals(2, tx0x2_pool001.getChangeOutputs().size());
        changeValue = tx0x2_pool001.getChangeValue();
        changeValueA = tx0x2_pool001.getChangeOutputs().get(0).getValue().value;
        changeValueB = tx0x2_pool001.getChangeOutputs().get(1).getValue().value;
        changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValueA, changeValueB); // split evenly
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // tx0x2_pool05 spends from spendFroms
        Assertions.assertTrue(
            utxosContains(
                tx0x2_pool05.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));

        // tx0x2_pool01 spends from tx0_pool05 outputs
        Assertions.assertEquals(
            tx0x2_pool05.getChangeOutputs().size(), tx0x2_pool01.getSpendFroms().size());
        for (TransactionOutput txOut : tx0x2_pool05.getChangeOutputs()) {
            String hash = tx0x2_pool05.getTx().getHashAsString();
            Assertions.assertTrue(utxosContains(tx0x2_pool01.getSpendFroms(), hash, txOut.getIndex()));
        }

        // tx0x2_pool001 spends from tx0_pool01 outputs
        Assertions.assertEquals(
            tx0x2_pool01.getChangeOutputs().size(), tx0x2_pool001.getSpendFroms().size());
        for (TransactionOutput txOut : tx0x2_pool01.getChangeOutputs()) {
            String hash = tx0x2_pool01.getTx().getHashAsString();
            Assertions.assertTrue(utxosContains(tx0x2_pool001.getSpendFroms(), hash, txOut.getIndex()));
        }

        // current wallet utxos should be mocked from tx0x2_pool001 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : tx0x2_pool001.getChangeOutputs()) {
            String hash = tx0x2_pool001.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }

    /**
     * Test case for Tx0Service.computeSpendFromAmountsStonewall()
     * Sorts spendFroms in descending order (helps certain cases)
     * ex: 3 utxos [0.0009, 0.003, 0.0009]
     *   - would fail if unsorted; would be [0.0039, 0.0009]
     *   - passes if sorted; would be [0.003, 0.0018]
     */
    @Test
    public void tx0x2_decoy_3utxos() throws Exception {
        PoolSupplier poolSupplier = whirlpoolWallet.getPoolSupplier();

        // mock initial data
        HD_Address address0 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
        UnspentOutput spendFromUtxo0 =
            newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
                1,
                900000,
                address0);
        HD_Address address1 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 62).getHdAddress();
        UnspentOutput spendFromUtxo1 =
            newUnspentOutput(
                "7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                2,
                3000000,
                address1);
        HD_Address address2 = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 63).getHdAddress();
        UnspentOutput spendFromUtxo2 =
            newUnspentOutput(
                "3268819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a",
                3,
                900000,
                address2);
        mockUtxos(spendFromUtxo0, spendFromUtxo1, spendFromUtxo2);

        Collection<UnspentOutput> spendFroms = new ArrayList<>();
        spendFroms.add(spendFromUtxo0);
        spendFroms.add(spendFromUtxo1);
        spendFroms.add(spendFromUtxo2);

        // configure TX0
        Pool pool = poolSupplier.findPoolById("0.01btc");
        Tx0Config tx0Config =
            whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
        tx0Config.setDecoyTx0x2(true); // set decoy Tx0x2 flag

        // run
        Tx0 decoyTx0x2 = whirlpoolWallet.tx0(spendFroms, tx0Config, pool);

        // verify
        log.info("decoyTx0x2 = " + decoyTx0x2);
        Assertions.assertEquals(3, decoyTx0x2.getNbPremix());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));

        int nbPremixSender = 3;
        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, decoyTx0x2.getTx().getOutputs().size());


        Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
        long changeValue = decoyTx0x2.getChangeValue();
        long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
        long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // current wallet utxos should be mocked from tx0 outputs
        whirlpoolWallet.refreshUtxosAsync().blockingAwait();
        for (TransactionOutput txOut : decoyTx0x2.getChangeOutputs()) {
            String hash = decoyTx0x2.getTx().getHashAsString();
            int index = txOut.getIndex();
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
            Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
        }
    }
}
