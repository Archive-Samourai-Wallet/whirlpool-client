package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class WalletOpenEvent extends WhirlpoolEvent {
  private WhirlpoolWallet whirlpoolWallet;

  public WalletOpenEvent(WhirlpoolWallet whirlpoolWallet) {
    super();
    this.whirlpoolWallet = whirlpoolWallet;
  }

  public WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
