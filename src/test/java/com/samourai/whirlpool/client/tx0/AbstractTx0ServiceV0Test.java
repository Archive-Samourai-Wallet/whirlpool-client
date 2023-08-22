package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.whirlpool.ServerApi;

public abstract class AbstractTx0ServiceV0Test extends AbstractTx0ServiceTest {
  public AbstractTx0ServiceV0Test() throws Exception {
    super(64);
  }

  @Override
  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(ServerApi serverApi) {
    WhirlpoolWalletConfig config = super.computeWhirlpoolWalletConfig(serverApi);
    config.setFeeOpReturnImplV0();
    return config;
  }
}
