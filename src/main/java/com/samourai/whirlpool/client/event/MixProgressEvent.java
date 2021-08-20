package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.listener.MixProgress;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class MixProgressEvent extends WhirlpoolEvent {
  private MixProgress mixProgress;

  public MixProgressEvent(MixProgress mixProgress) {
    this.mixProgress = mixProgress;
  }

  public MixProgress getMixProgress() {
    return mixProgress;
  }
}
