package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.AbstractWhirlpoolWalletTest;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTx0ServiceTest extends AbstractWhirlpoolWalletTest {
  private Logger log = LoggerFactory.getLogger(AbstractTx0ServiceTest.class);

  protected static final long FEE_VALUE = 10000;
  protected final int FEE_PAYLOAD_LENGTH;

  protected Tx0PreviewService tx0PreviewService;

  public AbstractTx0ServiceTest(int FEE_PAYLOAD_LENGTH) throws Exception {
    super();
    this.FEE_PAYLOAD_LENGTH = FEE_PAYLOAD_LENGTH;
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();

    tx0PreviewService = whirlpoolWallet.getTx0PreviewService();
  }

  @Override
  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(ServerApi serverApi) {
    WhirlpoolWalletConfig config = super.computeWhirlpoolWalletConfig(serverApi);
    config.setTx0MaxOutputs(10);
    config.getFeeOpReturnImpl().setTestMode(true);
    return config;
  }

  protected byte[] encodeFeePayload(int feeIndice, short scodePayload, short partner) {
    return config.getFeeOpReturnImpl().computeFeePayload(feeIndice, scodePayload, partner);
  }

  protected Tx0 tx0(UnspentOutput[] spendFromUtxos, Tx0Config tx0Config, Tx0Preview tx0Preview)
      throws Exception {
    Tx0 tx0 =
        whirlpoolWallet
            .getTx0Service()
            .tx0(
                Arrays.asList(spendFromUtxos),
                whirlpoolWallet.getWalletSupplier(),
                tx0Config,
                tx0Preview,
                whirlpoolWallet.getUtxoSupplier());

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

  protected void check(Tx0Preview tx0Preview) {
    Assertions.assertEquals(FEE_PAYLOAD_LENGTH, tx0Preview.getTx0Data().getFeePayload().length);
  }
}
