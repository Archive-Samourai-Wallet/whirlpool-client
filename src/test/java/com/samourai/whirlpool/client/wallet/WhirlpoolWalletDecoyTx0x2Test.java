package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.exception.NotifiableException;
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

    @Test
    public void tx0x2_decoy() throws Exception {
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
        Assertions.assertEquals(5, decoyTx0x2.getNbPremix());
        Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo0.tx_hash, spendFromUtxo0.tx_output_n));
        Assertions.assertTrue(
            utxosContains(
                decoyTx0x2.getSpendFroms(), spendFromUtxo1.tx_hash, spendFromUtxo1.tx_output_n));

        int nbPremixSender = 5;
        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, decoyTx0x2.getTx().getOutputs().size());

        long changeValue = decoyTx0x2.getChangeValue();
        long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().getValue();
        long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().getValue();
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
        log.info("Decoy tx0x2 failure due to not having required amount");
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
                1,
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

        try {
            // run
            Tx0 decoyTx0x2 = whirlpoolWallet.tx0(utxos, tx0Config, pool);
        } catch (NotifiableException e) {
            Assertions.assertEquals("Can't build Decoy Tx0x2. Required amount not met.", e.getMessage());
        }
    }

    @Test
    public void tx0x2_decoy_fail_singleUTXO() throws Exception {
        log.info("Decoy tx0x2 failure due to only having 1 utxo");
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

        try {
            // run
            Tx0 decoyTx0x2 = whirlpoolWallet.tx0(Arrays.asList(spendFromUtxo), tx0Config, pool);
        } catch (NotifiableException e) {
            Assertions.assertEquals("Can't build Decoy Tx0x2 with 1 utxo", e.getMessage());
        }
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

        int nbPremixSender = 5;
        Assertions.assertTrue(tx0x2_pool01.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool01.getNbPremix());
        Assertions.assertEquals(2, tx0x2_pool01.getChangeOutputs().size());
        Assertions.assertTrue(tx0x2_pool01.getPremixOutputs().size() > 0);

        int expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool01.getTx().getOutputs().size());

        long changeValue = tx0x2_pool01.getChangeValue();
        long changeValueA = tx0x2_pool01.getChangeOutputs().get(0).getValue().getValue();
        long changeValueB = tx0x2_pool01.getChangeOutputs().get(1).getValue().getValue();
        long changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValue, changeOutputsSum);

        // 0.001 pool
        Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
        log.info("tx0_pool001 = " + tx0x2_pool001);

        nbPremixSender = 2;
        Assertions.assertTrue(tx0x2_pool001.getNbPremix() > 0);
        Assertions.assertEquals(nbPremixSender, tx0x2_pool001.getNbPremix());
        Assertions.assertEquals(2, tx0x2_pool001.getChangeOutputs().size());
        Assertions.assertTrue(tx0x2_pool001.getPremixOutputs().size() > 0);

        expectedOutputs = nbPremixSender + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
        Assertions.assertEquals(expectedOutputs, tx0x2_pool001.getTx().getOutputs().size());

        changeValue = tx0x2_pool001.getChangeValue();
        changeValueA = tx0x2_pool001.getChangeOutputs().get(0).getValue().getValue();
        changeValueB = tx0x2_pool001.getChangeOutputs().get(1).getValue().getValue();
        changeOutputsSum = changeValueA + changeValueB;
        Assertions.assertEquals(changeValueA, changeValueB); // split evenly
        Assertions.assertEquals(changeValue, changeOutputsSum + 1); // 1 sat to miner fees

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

}
