package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtxoConfigSupplierTest extends UtxoSupplierTest {

  @Test
  public void testValid() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1, UTXO_PREMIX1, UTXO_POSTMIX1};
    setMockWalletResponse(utxos1);

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    Assertions.assertEquals(utxos1.length, utxoSupplier.getUtxos().size());

    // verify mixsTarget
    Assertions.assertEquals(
        null,
        utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getMixsTarget());
    Assertions.assertEquals(
        null,
        utxoSupplier.findUtxo(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n).getMixsTarget());
    Assertions.assertEquals(
        null,
        utxoSupplier.findUtxo(UTXO_POSTMIX1.tx_hash, UTXO_POSTMIX1.tx_output_n).getMixsTarget());

    // verify mixsDone
    Assertions.assertEquals(
        0, utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getMixsDone());
    Assertions.assertEquals(
        0, utxoSupplier.findUtxo(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n).getMixsDone());
    Assertions.assertEquals(
        1, utxoSupplier.findUtxo(UTXO_POSTMIX1.tx_hash, UTXO_POSTMIX1.tx_output_n).getMixsDone());

    Assertions.assertNull(utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, 99));

    // setPoolId
    utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).setPoolId("test");
    Assertions.assertEquals(
        "test",
        utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getPoolId());

    // setMixsTarget
    utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).setMixsTarget(5);
    Assertions.assertEquals(
        5, utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getMixsTarget());
  }

  @Test
  public void clean() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1};
    setMockWalletResponse(utxos1);

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    Assertions.assertEquals(utxos1.length, utxoSupplier.getUtxos().size());

    // setMixsTarget
    utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).setMixsTarget(123);
    Assertions.assertEquals(
        123,
        utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getMixsTarget());

    // forward
    String fromKey =
        utxoConfigSupplier.computeUtxoConfigKey(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n);
    String toKey =
        utxoConfigSupplier.computeUtxoConfigKey(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n);
    utxoConfigSupplier.forwardUtxoConfig(fromKey, toKey);

    // spent utxo disappears
    UnspentOutput[] utxos2 = new UnspentOutput[] {};
    setMockWalletResponse(utxos2);

    // Thread.sleep(UtxoConfigData.FORWARDING_EXPIRATION_SECONDS+1); // test should fail

    // verify => no change
    utxoSupplier.expire();
    doTest(utxos2);

    // receive utxo appears
    UnspentOutput[] utxos3 = new UnspentOutput[] {UTXO_PREMIX1};
    setMockWalletResponse(utxos3);

    // verify => no change
    utxoSupplier.expire();
    doTest(utxos3);

    // verify config preserved
    WhirlpoolUtxo utxoPremix1 =
        utxoSupplier.findUtxo(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n);
    Assertions.assertEquals(123, utxoPremix1.getMixsTarget());
    // verify forwarding null
    Assertions.assertNull(utxoConfigSupplier.getUtxoConfigPersisted(utxoPremix1).getForwarding());
  }
}
