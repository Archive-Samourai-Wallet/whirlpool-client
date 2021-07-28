package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class Tx0Event extends WhirlpoolEvent {
  private Tx0 tx0;

  public Tx0Event(Tx0 tx0) {
    super();
    this.tx0 = tx0;
  }

  public Tx0 getTx0() {
    return tx0;
  }
}
