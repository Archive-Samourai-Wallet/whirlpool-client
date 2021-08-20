package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.listener.MixFail;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class MixFailEvent extends WhirlpoolEvent {
  private MixFail mixFail;

  public MixFailEvent(MixFail mixFail) {
    super();
    this.mixFail = mixFail;
  }

  public MixFail getMixFail() {
    return mixFail;
  }
}
