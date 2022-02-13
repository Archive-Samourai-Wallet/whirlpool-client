package com.samourai.whirlpool.client.event;

import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class PaynymChangeEvent extends WhirlpoolEvent {
  private PaynymState paynymState;

  public PaynymChangeEvent(PaynymState paynymState) {
    super();
    this.paynymState = paynymState;
  }

  public PaynymState getPaynymState() {
    return paynymState;
  }
}
