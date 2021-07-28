package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class MixFailEvent extends WhirlpoolEvent {
  private WhirlpoolUtxo whirlpoolUtxo;
  private MixFailReason mixFailReason;

  public MixFailEvent(WhirlpoolUtxo whirlpoolUtxo, MixFailReason mixFailReason) {
    super();
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.mixFailReason = mixFailReason;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public MixFailReason getMixFailReason() {
    return mixFailReason;
  }
}
