package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public abstract class AbstractMixEvent extends WhirlpoolWalletEvent {
  private MixParams mixParams;

  public AbstractMixEvent(WhirlpoolWallet whirlpoolWallet, MixParams mixParams) {
    super(whirlpoolWallet);
    this.mixParams = mixParams;
  }

  public MixParams getMixParams() {
    return mixParams;
  }
}
