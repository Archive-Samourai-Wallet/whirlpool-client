package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UtxoConfigPersisted {
  private int mixsDone;
  private Long forwarding;

  public UtxoConfigPersisted() {
    this(0, null);
  }

  public UtxoConfigPersisted(int mixsDone, Long forwarding) {
    this.mixsDone = mixsDone;
    this.forwarding = forwarding;
  }

  public UtxoConfigPersisted copy() {
    UtxoConfigPersisted copy = new UtxoConfigPersisted(this.mixsDone, this.forwarding);
    return copy;
  }

  public int getMixsDone() {
    return mixsDone;
  }

  public void setMixsDone(int mixsDone) {
    this.mixsDone = mixsDone;
  }

  public void incrementMixsDone() {
    this.mixsDone++;
  }

  public Long getForwarding() {
    return forwarding;
  }

  public void setForwarding(Long forwarding) {
    this.forwarding = forwarding;
  }

  @JsonIgnore
  @Deprecated
  public void setMixsTarget(Integer mixsTarget) {
    // keep this for backward-compatibility
  }

  @JsonIgnore
  @Deprecated
  public void setPoolId(String poolId) {
    // keep this for backward-compatibility
  }

  @Override
  public String toString() {
    return "mixsDone=" + mixsDone + ", forwarding=" + (forwarding != null ? forwarding : "null");
  }
}
