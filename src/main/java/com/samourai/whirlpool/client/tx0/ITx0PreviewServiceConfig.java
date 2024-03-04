package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.SamouraiNetwork;

public interface ITx0PreviewServiceConfig {
  SamouraiNetwork getSamouraiNetwork();

  Long getOverspend(String poolId);

  int getFeeMin();

  int getFeeMax();

  int getTx0MaxOutputs();

  String getScode();

  String getPartner();
}
