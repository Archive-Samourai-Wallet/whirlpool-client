package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;

public class Tx0Config extends Tx0PreviewConfig {
  private SamouraiAccount changeWallet;
  private int tx0AttemptsAddressReuse;
  private int tx0AttemptsSoroban;

  public Tx0Config(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      SamouraiAccount changeWallet,
      int tx0AttemptsAddressReuse,
      int tx0AttemptsSoroban) {
    super(tx0FeeTarget, mixFeeTarget);
    this.changeWallet = changeWallet;
    this.tx0AttemptsAddressReuse = tx0AttemptsAddressReuse;
    this.tx0AttemptsSoroban = tx0AttemptsSoroban;
  }

  public SamouraiAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(SamouraiAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }

  public int getTx0AttemptsAddressReuse() {
    return tx0AttemptsAddressReuse;
  }

  public int getTx0AttemptsSoroban() {
    return tx0AttemptsSoroban;
  }
}
