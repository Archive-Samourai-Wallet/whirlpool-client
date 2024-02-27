package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.WhirlpoolNetwork;

public interface ITx0PreviewServiceConfig {
  WhirlpoolNetwork getWhirlpoolNetwork();

  Long getOverspend(String poolId);

  int getFeeMin();

  int getFeeMax();

  int getTx0MaxOutputs();

  String getScode();

  String getPartner();
}
