package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class MixSuccessEvent extends WhirlpoolEvent {
  private MixSuccess mixSuccess;

  public MixSuccessEvent(MixSuccess mixSuccess) {
    super();
    this.mixSuccess = mixSuccess;
  }

  public MixSuccess getMixSuccess() {
    return mixSuccess;
  }
}
