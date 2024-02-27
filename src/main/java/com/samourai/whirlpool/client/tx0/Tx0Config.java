package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private WhirlpoolAccount changeWallet;

  public Tx0Config(
      Tx0PreviewService tx0PreviewService,
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      WhirlpoolAccount changeWallet) {
    super(tx0PreviewService, pools, tx0FeeTarget, mixFeeTarget);
    this.changeWallet = changeWallet;
  }

  public WhirlpoolAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(WhirlpoolAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }
}
