package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;

public class Tx0PreviewConfig {
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private Tx0 cascadingParent; // set when cascading

  public Tx0PreviewConfig(Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.cascadingParent = null;
  }

  public Tx0FeeTarget getTx0FeeTarget() {
    return tx0FeeTarget;
  }

  public void setTx0FeeTarget(Tx0FeeTarget tx0FeeTarget) {
    this.tx0FeeTarget = tx0FeeTarget;
  }

  public Tx0FeeTarget getMixFeeTarget() {
    return mixFeeTarget;
  }

  public void setMixFeeTarget(Tx0FeeTarget mixFeeTarget) {
    this.mixFeeTarget = mixFeeTarget;
  }

  public Tx0 getCascadingParent() {
    return cascadingParent;
  }

  public void setCascadingParent(Tx0 cascadingParent) {
    this.cascadingParent = cascadingParent;
  }
}
