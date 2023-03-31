package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoolSupplierTest extends AbstractTest {
  private ExpirablePoolSupplier supplier;

  public PoolSupplierTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setUp() throws Exception {
    this.supplier = mockPoolSupplier();
  }

  @Test
  public void testValid() throws Exception {
    // valid
    doTest();

    // non-existing pool
    Assertions.assertNull(supplier.findPoolById("foo"));
  }

  private void doTest() throws Exception {
    supplier.load();

    // verify getPools
    Collection<Pool> getPools = supplier.getPools();
    Assertions.assertEquals(4, getPools.size());

    // verify findPoolById
    Pool pool01 = supplier.findPoolById("0.01btc");
    Assertions.assertEquals("0.01btc", pool01.getPoolId());
    Assertions.assertEquals(1000000, pool01.getDenomination());
    Assertions.assertEquals(50000, pool01.getFeeValue());
    Assertions.assertEquals(1000175, pool01.getPremixValue());
    Assertions.assertEquals(1000170, pool01.getPremixValueMin());
    Assertions.assertEquals(1019125, pool01.getPremixValueMax());
    Assertions.assertEquals(70, pool01.getTx0MaxOutputs());
    Assertions.assertEquals(5, pool01.getAnonymitySet());

    // verify getTx0PreviewMin
    Tx0Preview tx0Preview = pool01.getTx0PreviewMin();
    Assertions.assertEquals(1050523, tx0Preview.getTotalValue());
    Assertions.assertEquals(1050523, tx0Preview.getSpendValue());
    Assertions.assertEquals(1000175, tx0Preview.getPremixValue());
    Assertions.assertEquals(50000, tx0Preview.getFeeValue());
    Assertions.assertEquals(0, tx0Preview.getFeeChange());
    Assertions.assertEquals(261, tx0Preview.getTx0Size());
    Assertions.assertEquals(1, tx0Preview.getNbPremix());
    Assertions.assertEquals(262, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(1, tx0Preview.getMixMinerFeePrice());
    Assertions.assertEquals(261, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(1, tx0Preview.getTx0MinerFeePrice());
    Assertions.assertEquals(0, tx0Preview.getChangeValue());
    Assertions.assertEquals("0.01btc", tx0Preview.getPool().getPoolId());
    Assertions.assertEquals(1050523, pool01.getTx0PreviewMinSpendValue());
  }
}
