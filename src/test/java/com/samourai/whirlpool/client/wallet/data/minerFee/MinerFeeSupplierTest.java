package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MinerFeeSupplierTest extends AbstractTest {
  private MockMinerFeeSupplier supplier;
  private final int FEE_MIN = 50;
  private final int FEE_MAX = 500;
  private final int FEE_FALLBACK = 123;

  @BeforeEach
  public void setup() throws Exception {
    this.supplier = new MockMinerFeeSupplier(FEE_MIN, FEE_MAX, FEE_FALLBACK);
  }

  @Test
  public void testValid() throws Exception {
    // valid
    supplier.setMockFeeValue(100);
    doTest(100);

    // should use cached data
    supplier.setMockFeeValue(200);
    doTest(100);

    // should use fresh data
    supplier.expire();
    doTest(200);
  }

  @Test
  public void testMinMax() throws Exception {
    // to low => min
    supplier.setMockFeeValue(FEE_MIN - 5);
    doTest(FEE_MIN);

    // should use cached data
    supplier.setMockFeeValue(FEE_MAX + 5);
    supplier.expire();
    doTest(FEE_MAX);
  }

  @Test
  public void testLastValueFallback() throws Exception {
    // valid
    supplier.setMockFeeValue(100);
    doTest(100);

    // invalid => should use last valid data
    supplier.setMockException(true);
    supplier.expire();
    doTest(100);

    // back to valid
    supplier.setMockException(false);
    supplier.setMockFeeValue(200);
    supplier.expire();
    doTest(200);
  }

  @Test
  public void testInitialFailure() throws Exception {

    // initial failure => should use feeFallback
    supplier.setMockException(true);
    doTest(FEE_FALLBACK);

    // valid
    supplier.setMockException(false);
    supplier.setMockFeeValue(100);
    supplier.expire();
    doTest(100);
  }

  private void doTest(int expected) throws Exception {

    supplier.load();

    // getFee(MinerFeeTarget)
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      Assertions.assertEquals(expected, supplier.getFee(minerFeeTarget));
    }

    // getFee(Tx0FeeTarget)
    for (Tx0FeeTarget tx0FeeTarget : Tx0FeeTarget.values()) {
      Assertions.assertEquals(expected, supplier.getFee(tx0FeeTarget));
    }
  }
}
