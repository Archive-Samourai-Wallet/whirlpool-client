package com.samourai.whirlpool.client.tx0;

public abstract class AbstractTx0ServiceV0Test extends AbstractTx0ServiceTest {
  public AbstractTx0ServiceV0Test() throws Exception {
    super(64);
  }

  public void setup() throws Exception {
    super.setup(true); // V0
  }
}
