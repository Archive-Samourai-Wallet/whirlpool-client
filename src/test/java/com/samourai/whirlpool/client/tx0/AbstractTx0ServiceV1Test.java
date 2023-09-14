package com.samourai.whirlpool.client.tx0;

public abstract class AbstractTx0ServiceV1Test extends AbstractTx0ServiceTest {
  public AbstractTx0ServiceV1Test() throws Exception {
    super(46);
  }

  public void setup() throws Exception {
    super.setup(false); // V1
  }
}
