package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;

public class Tx0Config extends Tx0PreviewConfig {
  private SamouraiAccount changeWallet;
  private int tx0MaxRetry;

  public Tx0Config(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      SamouraiAccount changeWallet,
      int tx0MaxRetry) {
    super(tx0FeeTarget, mixFeeTarget);
    this.changeWallet = changeWallet;
    this.tx0MaxRetry = tx0MaxRetry;
  }

  public SamouraiAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(SamouraiAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }
}
