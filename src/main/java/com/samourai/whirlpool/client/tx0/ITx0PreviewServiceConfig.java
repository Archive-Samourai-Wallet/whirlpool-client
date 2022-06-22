package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.whirlpool.ServerApi;
import org.bitcoinj.core.NetworkParameters;

public interface ITx0PreviewServiceConfig {
  NetworkParameters getNetworkParameters();

  Long getOverspend(String poolId);

  int getFeeMin();

  int getFeeMax();

  int getTx0MaxOutputs();

  String getScode();

  ServerApi getServerApi();

  String getPartner();

  boolean isOpReturnV0();
}
