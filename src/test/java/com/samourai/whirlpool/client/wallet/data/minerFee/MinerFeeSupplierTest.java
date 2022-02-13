package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MinerFeeSupplierTest extends AbstractTest {
  private BasicMinerFeeSupplier supplier;
  private final int FEE_MIN = 50;
  private final int FEE_MAX = 500;
  private final int FEE_FALLBACK = 123;

  public MinerFeeSupplierTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.supplier = new BasicMinerFeeSupplier(FEE_MIN, FEE_MAX);
    this.supplier.setValue(FEE_FALLBACK);
  }

  @Test
  public void testValid() throws Exception {
    // valid
    setMockFeeValue(100);
    doTest(100);

    // should use cached data
    setMockFeeValue(200);
    doTest(200);
  }

  @Test
  public void testMinMax() throws Exception {
    // too low => min
    setMockFeeValue(FEE_MIN - 5);
    doTest(FEE_MIN);

    // too high => max
    setMockFeeValue(FEE_MAX + 5);
    doTest(FEE_MAX);
  }

  private void setMockFeeValue(int feeValue) throws Exception {
    MinerFee value = BasicMinerFeeSupplier.mockMinerFee(feeValue);
    supplier.setValue(value);
  }

  private void doTest(int expected) throws Exception {
    // getFee(MinerFeeTarget)
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      Assertions.assertEquals(expected, supplier.getFee(minerFeeTarget));
    }

    // getFee(Tx0FeeTarget)
    for (Tx0FeeTarget tx0FeeTarget : Tx0FeeTarget.values()) {
      Assertions.assertEquals(expected, supplier.getFee(tx0FeeTarget.getFeeTarget()));
    }
  }
}
