package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;

public interface ITx0PreviewServiceConfig {
  WhirlpoolNetwork getWhirlpoolNetwork();

  Long getOverspend(String poolId);

  int getFeeMin();

  int getFeeMax();

  int getTx0MaxOutputs();

  String getScode();

  String getPartner();

  boolean isOpReturnV0();
}
