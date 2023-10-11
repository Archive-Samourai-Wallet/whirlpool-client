package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.util.Util;
import com.samourai.whirlpool.client.wallet.AbstractWhirlpoolWalletTest;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTx0ServiceTest extends AbstractWhirlpoolWalletTest {
  private Logger log = LoggerFactory.getLogger(AbstractTx0ServiceTest.class);

  protected static final long FEE_VALUE = 10000;
  protected final int FEE_PAYLOAD_LENGTH;

  public AbstractTx0ServiceTest(int FEE_PAYLOAD_LENGTH) throws Exception {
    super();
    this.FEE_PAYLOAD_LENGTH = FEE_PAYLOAD_LENGTH;
  }

  public void setup(boolean isOpReturnV0) throws Exception {
    super.setup(isOpReturnV0);
  }

  protected byte[] encodeFeePayload(int feeIndice, short scodePayload, short partner) {
    return whirlpoolWalletConfig
        .getFeeOpReturnImpl()
        .computeFeePayload(feeIndice, scodePayload, partner);
  }

  protected Tx0 tx0(Tx0Config tx0Config, Tx0Preview tx0Preview) throws Exception {
    Tx0 tx0 = whirlpoolWallet.getTx0Service().buildTx0(tx0Config, tx0Preview).get();

    Assertions.assertEquals(FEE_PAYLOAD_LENGTH, tx0.getTx0Data().getFeePayload().length);
    return tx0;
  }

  protected void assertEquals(Tx0Preview tp, Tx0Preview tp2) {
    Assertions.assertEquals(tp.getTx0MinerFee(), tp2.getTx0MinerFee());
    Assertions.assertEquals(tp.getFeeValue(), tp2.getFeeValue());
    Assertions.assertEquals(tp.getFeeChange(), tp2.getFeeChange());
    Assertions.assertEquals(tp.getFeeDiscountPercent(), tp2.getFeeDiscountPercent());
    Assertions.assertEquals(tp.getPremixValue(), tp2.getPremixValue());
    Assertions.assertEquals(tp.getChangeValue(), tp2.getChangeValue());
    Assertions.assertEquals(tp.getNbPremix(), tp2.getNbPremix());
  }

  protected void assertTx0Previews(
      List<? extends Tx0Preview> tx0Previews,
      List<String> poolIds,
      List<Boolean> tx0x2Decoys,
      List<Integer> nbPremixs) {
    Assertions.assertArrayEquals(
        poolIds.toArray(),
        tx0Previews.stream().map(t -> t.getPool().getPoolId()).toArray(),
        "poolIds");
    Assertions.assertArrayEquals(
        tx0x2Decoys.toArray(),
        tx0Previews.stream().map(t -> t.isTx0x2Decoy()).toArray(),
        "tx0x2Decoys");
    Assertions.assertArrayEquals(
        nbPremixs.toArray(), tx0Previews.stream().map(t -> t.getNbPremix()).toArray(), "nbPremixs");
  }

  protected void assertTx0Preview(
      Tx0Preview tx0Preview, String poolId, boolean tx0Decoy, int nbPremix) {
    Assertions.assertEquals(tx0Preview.getPool().getPoolId(), poolId, "poolId");
    Assertions.assertEquals(tx0Decoy, tx0Preview.isTx0x2Decoy(), "decoyTx0x2");
    Assertions.assertEquals(nbPremix, tx0Preview.getNbPremix(), "nbPremix");
  }

  protected void assertTx0(
      Tx0 tx0, String poolId, boolean tx0Decoy, int nbPremix, List<Long> changeAmounts) {
    assertTx0Preview(tx0, poolId, tx0Decoy, nbPremix);

    // verify changes
    Assertions.assertArrayEquals(
        changeAmounts.toArray(), tx0.getChangeAmountsAll().toArray(), "changeAmounts");
    Assertions.assertArrayEquals(
        changeAmounts.toArray(),
        tx0.getChangeOutputs().stream().map(o -> o.getValue().getValue()).toArray(),
        "changeOutputs");
    Assertions.assertEquals(tx0.getChangeValue(), Util.sumLong(changeAmounts));

    // verify tx outputs
    int expectedOutputs =
        nbPremix + changeAmounts.size() + 1 + 1; // changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx0.getTx().getOutputs().size());
  }

  protected void check(Tx0Preview tx0Preview) {
    Assertions.assertEquals(FEE_PAYLOAD_LENGTH, tx0Preview.getTx0Data().getFeePayload().length);
  }
}
