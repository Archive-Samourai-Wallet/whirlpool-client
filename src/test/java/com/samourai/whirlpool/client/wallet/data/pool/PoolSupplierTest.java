package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.PoolInfo;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoolSupplierTest extends AbstractTest {
  private MockPoolSupplier supplier;

  private PoolInfo POOL_1;
  private PoolInfo POOL_2;
  private PoolInfo POOL_3;

  @BeforeEach
  public void setup() {
    this.supplier = new MockPoolSupplier();
    POOL_1 = new PoolInfo();
    POOL_1.poolId = "POOL_1";

    POOL_2 = new PoolInfo();
    POOL_2.poolId = "POOL_2";

    POOL_3 = new PoolInfo();
    POOL_3.poolId = "POOL_3";
  }

  @Test
  public void testValid() throws Exception {
    // valid
    supplier.mock(POOL_1, POOL_2, POOL_3);
    doTest(POOL_1, POOL_2, POOL_3);

    // non-existing pool
    Assertions.assertNull(supplier.findPoolById("foo"));
  }

  private void doTest(PoolInfo... pools) throws Exception {
    supplier.load();

    // verify getPools
    Collection<Pool> getPools = supplier.getPools();
    Assertions.assertEquals(pools.length, getPools.size());
    for (PoolInfo poolInfo : pools) {
      boolean found = false;
      for (Pool p : getPools) {
        if (p.getPoolId().equals(poolInfo.poolId)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new Exception("Expected pool not found: " + poolInfo.poolId);
      }
    }

    // verify findPoolById
    for (PoolInfo poolInfo : pools) {
      Assertions.assertEquals(supplier.findPoolById(poolInfo.poolId).getPoolId(), poolInfo.poolId);
    }
  }
}
