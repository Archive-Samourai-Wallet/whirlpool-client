package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;

public class WalletCloseEvent extends WhirlpoolEvent {
  private WhirlpoolWallet whirlpoolWallet;

  public WalletCloseEvent(WhirlpoolWallet whirlpoolWallet) {
    super();
    this.whirlpoolWallet = whirlpoolWallet;
  }

  public WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }
}
