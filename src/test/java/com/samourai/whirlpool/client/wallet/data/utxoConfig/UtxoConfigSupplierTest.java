package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplierTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtxoConfigSupplierTest extends UtxoSupplierTest {

  public UtxoConfigSupplierTest() throws Exception {
    super();
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

    Assertions.assertEquals(1, utxoSupplier.getUtxos(WhirlpoolAccount.DEPOSIT).size());
    Assertions.assertEquals(1, utxoSupplier.getUtxos(WhirlpoolAccount.PREMIX).size());
    Assertions.assertEquals(1, utxoSupplier.getUtxos(WhirlpoolAccount.POSTMIX).size());
    Assertions.assertEquals(0, utxoSupplier.getUtxos(WhirlpoolAccount.BADBANK).size());
    Assertions.assertEquals(0, utxoSupplier.getUtxos(WhirlpoolAccount.RICOCHET).size());

    // verify mixsDone
    Assertions.assertEquals(
        0, utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, UTXO_DEPOSIT1.tx_output_n).getMixsDone());
    Assertions.assertEquals(
        0, utxoSupplier.findUtxo(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n).getMixsDone());
    Assertions.assertEquals(
        1, utxoSupplier.findUtxo(UTXO_POSTMIX1.tx_hash, UTXO_POSTMIX1.tx_output_n).getMixsDone());

    Assertions.assertNull(utxoSupplier.findUtxo(UTXO_DEPOSIT1.tx_hash, 99));
  }

  @Test
  public void clean() throws Exception {
    // mock initial data
    UnspentOutput[] utxos1 = new UnspentOutput[] {UTXO_DEPOSIT1};
    setMockWalletResponse(utxos1);
    dataSource.open();

    // verify
    doTest(utxos1);
    assertUtxoChanges(utxos1, new UnspentOutput[] {}, new UnspentOutput[] {});

    // verify => no change
    utxoSupplier.refresh();
    doTest(utxos1);

    // receive utxo appears
    UnspentOutput[] utxos3 = new UnspentOutput[] {UTXO_PREMIX1};
    setMockWalletResponse(utxos3);

    // verify => no change
    utxoSupplier.refresh();
    doTest(utxos3);

    // verify config preserved
    WhirlpoolUtxo utxoPremix1 =
        utxoSupplier.findUtxo(UTXO_PREMIX1.tx_hash, UTXO_PREMIX1.tx_output_n);
  }
}
