package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentResponse;
import java.util.List;
import java8.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtxoConfigSupplierTest extends UtxoSupplierTest {

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

    Assertions.assertEquals(utxos1.size(), utxoSupplier.getUtxos().size());

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
}
