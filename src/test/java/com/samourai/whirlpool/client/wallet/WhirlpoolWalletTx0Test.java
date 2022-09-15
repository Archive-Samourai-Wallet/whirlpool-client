package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0ServiceV1Test;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletTx0Test extends Tx0ServiceV1Test {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0Test.class);

  public WhirlpoolWalletTx0Test() throws Exception {
    super();
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
    for (TransactionOutput txOut : tx0.getChangeOutputs()) {
      String hash = tx0.getTx().getHashAsString();
      int index = txOut.getIndex();
      Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier().findUtxo(hash, index));
      Assertions.assertNotNull(whirlpoolWallet.getUtxoSupplier()._getPrivKey(hash, index));
    }
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