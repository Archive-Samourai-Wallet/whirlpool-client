package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixingState;

public class MixStateChangeEvent extends WhirlpoolWalletEvent {
  private MixingState mixingState;

  public MixStateChangeEvent(WhirlpoolWallet whirlpoolWallet, MixingState mixingState) {
    super(whirlpoolWallet);
    this.mixingState = mixingState;
  }

  public MixingState getMixingState() {
    return mixingState;
  }
}
