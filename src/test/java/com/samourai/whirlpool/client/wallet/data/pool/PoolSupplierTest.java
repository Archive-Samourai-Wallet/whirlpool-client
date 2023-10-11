package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoolSupplierTest extends AbstractTest {

  public PoolSupplierTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void findPoolByMaxId() {
    Object[] poolsByMaxId =
        poolSupplier.findPoolsByMaxId("0.01btc").stream().map(p -> p.getPoolId()).toArray();
    Assertions.assertArrayEquals(new String[] {"0.01btc", "0.001btc"}, poolsByMaxId);

    poolsByMaxId =
        poolSupplier.findPoolsByMaxId("0.5btc").stream().map(p -> p.getPoolId()).toArray();
    Assertions.assertArrayEquals(
        new String[] {"0.5btc", "0.05btc", "0.01btc", "0.001btc"}, poolsByMaxId);
  }

  @Test
  public void getPools() throws Exception {
    // verify getPools
    Collection<Pool> getPools = poolSupplier.getPools();
    Object[] poolIds = getPools.stream().map(p -> p.getPoolId()).toArray();
    Assertions.assertArrayEquals(
        new String[] {"0.5btc", "0.05btc", "0.01btc", "0.001btc"}, poolIds);
  }

  @Test
  public void findPoolById() throws Exception {
    // existing pool
    Assertions.assertEquals("0.001btc", poolSupplier.findPoolById("0.001btc").getPoolId());

    // non-existing pool
    Assertions.assertNull(poolSupplier.findPoolById("foo"));
  }

  @Test
  public void poolData() throws Exception {
    // verify pool data
    Pool pool01 = poolSupplier.findPoolById("0.01btc");
    Assertions.assertEquals("0.01btc", pool01.getPoolId());
    Assertions.assertEquals(1000000, pool01.getDenomination());
    Assertions.assertEquals(50000, pool01.getFeeValue());
    Assertions.assertEquals(1000170, pool01.getMustMixBalanceMin());
    Assertions.assertEquals(1009690, pool01.getMustMixBalanceCap());
    Assertions.assertEquals(1019125, pool01.getMustMixBalanceMax());
    Assertions.assertEquals(5, pool01.getMinAnonymitySet());
    Assertions.assertEquals(2, pool01.getMinMustMix());
    Assertions.assertEquals(70, pool01.getTx0MaxOutputs());
    Assertions.assertEquals(180, pool01.getNbRegistered());
    Assertions.assertEquals(5, pool01.getMixAnonymitySet());
    Assertions.assertEquals(MixStatus.CONFIRM_INPUT, pool01.getMixStatus());
    Assertions.assertEquals(672969, pool01.getElapsedTime());
    Assertions.assertEquals(2, pool01.getNbConfirmed());
    Assertions.assertEquals(1050519, pool01.getTx0PreviewMinSpendValue());
    Assertions.assertEquals(70070253, pool01.getTx0PreviewMaxSpendValue());
    Assertions.assertEquals(72582636, pool01.getTx0PreviewMaxSpendValueCascading());

    // verify getTx0PreviewMin
    Tx0Preview tx0Preview = pool01.getTx0PreviewMin();
    Assertions.assertEquals(1050519, tx0Preview.getTotalValue());
    Assertions.assertEquals(1050519, tx0Preview.getSpendValue());
    Assertions.assertEquals(1000255, tx0Preview.getPremixValue());
    Assertions.assertEquals(50000, tx0Preview.getFeeValue());
    Assertions.assertEquals(0, tx0Preview.getFeeChange());
    Assertions.assertEquals(264, tx0Preview.getTx0Size());
    Assertions.assertEquals(1, tx0Preview.getNbPremix());
    Assertions.assertEquals(255, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(1, tx0Preview.getMixMinerFeePrice());
    Assertions.assertEquals(264, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(1, tx0Preview.getTx0MinerFeePrice());
    Assertions.assertEquals(0, tx0Preview.getChangeValue());
    Assertions.assertEquals("0.01btc", tx0Preview.getPool().getPoolId());
  }
}
