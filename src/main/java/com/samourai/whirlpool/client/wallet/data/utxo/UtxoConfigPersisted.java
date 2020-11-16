package com.samourai.whirlpool.client.wallet.data.utxo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UtxoConfigPersisted {
  private String poolId;
  private int mixsDone;
  private Long forwarding;

  public UtxoConfigPersisted() {
    this(null, 0, null);
  }

  public UtxoConfigPersisted(String poolId, int mixsDone, Long forwarding) {
    this.poolId = poolId;
    this.mixsDone = mixsDone;
    this.forwarding = forwarding;
  }

  public UtxoConfigPersisted copy() {
    UtxoConfigPersisted copy = new UtxoConfigPersisted(this.poolId, this.mixsDone, this.forwarding);
    return copy;
  }

  public String getPoolId() {
    return poolId;
  }

  public void setPoolId(String poolId) {
    this.poolId = poolId;
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
  public void setMixsTarget(Integer mixsTarget) {
    // keep this for backward-compatibility
  }

  @Override
  public String toString() {
    return "poolId="
        + (poolId != null ? poolId : "null")
        + ", mixsDone="
        + mixsDone
        + ", forwarding="
        + (forwarding != null ? forwarding : "null");
  }
}
