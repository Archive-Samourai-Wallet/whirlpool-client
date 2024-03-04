package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private SamouraiAccount changeWallet;

  public Tx0Config(
      Tx0PreviewService tx0PreviewService,
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      SamouraiAccount changeWallet) {
    super(tx0PreviewService, pools, tx0FeeTarget, mixFeeTarget);
    this.changeWallet = changeWallet;
  }

  public SamouraiAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(SamouraiAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }
}
