package com.samourai.whirlpool.client.wallet.data.utxo;

public class UtxoConfigPersisted {
  private String poolId;
  private Integer mixsTarget;
  private int mixsDone;

  public UtxoConfigPersisted() {
    this(null, null, 0);
  }

  public UtxoConfigPersisted(String poolId, Integer mixsTarget, int mixsDone) {
    this.poolId = poolId;
    this.mixsTarget = mixsTarget;
    this.mixsDone = mixsDone;
  }

  public UtxoConfigPersisted copy() {
    UtxoConfigPersisted copy = new UtxoConfigPersisted(this.poolId, this.mixsTarget, this.mixsDone);
    return copy;
  }

  public String getPoolId() {
    return poolId;
  }

  public void setPoolId(String poolId) {
    this.poolId = poolId;
  }

  public Integer getMixsTarget() {
    return mixsTarget;
  }

  public void setMixsTarget(Integer mixsTarget) {
    this.mixsTarget = mixsTarget;
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

  @Override
  public String toString() {
    return "poolId="
        + (poolId != null ? poolId : "null")
        + ", mixsTarget="
        + (mixsTarget != null ? mixsTarget : "null")
        + ", mixsDone="
        + mixsDone;
  }
}
