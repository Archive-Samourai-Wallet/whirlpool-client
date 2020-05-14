package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

public class Tx0Config {
  private WhirlpoolAccount changeWallet;

  public Tx0Config() {
    this.changeWallet = WhirlpoolAccount.DEPOSIT;
  }

  public WhirlpoolAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(WhirlpoolAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }
}
