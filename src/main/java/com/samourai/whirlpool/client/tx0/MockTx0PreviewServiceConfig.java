package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;

public class MockTx0PreviewServiceConfig implements ITx0PreviewServiceConfig {
  private WhirlpoolNetwork whirlpoolNetwork;

  public MockTx0PreviewServiceConfig(WhirlpoolNetwork whirlpoolNetwork) {
    this.whirlpoolNetwork = whirlpoolNetwork;
  }

  @Override
  public WhirlpoolNetwork getWhirlpoolNetwork() {
    return whirlpoolNetwork;
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
