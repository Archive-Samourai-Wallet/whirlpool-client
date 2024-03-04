package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.SamouraiNetwork;

public class MockTx0PreviewServiceConfig implements ITx0PreviewServiceConfig {
  private SamouraiNetwork samouraiNetwork;

  public MockTx0PreviewServiceConfig(SamouraiNetwork samouraiNetwork) {
    this.samouraiNetwork = samouraiNetwork;
  }

  @Override
  public SamouraiNetwork getSamouraiNetwork() {
    return samouraiNetwork;
  }

  @Override
  public Long getOverspend(String poolId) {
    return null;
  }

  @Override
  public int getFeeMin() {
    return 1;
  }

  @Override
  public int getFeeMax() {
    return 9999;
  }

  @Override
  public int getTx0MaxOutputs() {
    return 70;
  }

  @Override
  public String getPartner() {
    return null;
  }

  @Override
  public String getScode() {
    return null;
  }
}
