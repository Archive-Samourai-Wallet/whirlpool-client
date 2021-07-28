package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class MixSuccessEvent extends WhirlpoolEvent {
  private WhirlpoolUtxo whirlpoolUtxo;
  private MixSuccess mixSuccess;

  public MixSuccessEvent(WhirlpoolUtxo whirlpoolUtxo, MixSuccess mixSuccess) {
    super();
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.mixSuccess = mixSuccess;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public MixSuccess getMixSuccess() {
    return mixSuccess;
  }
}
