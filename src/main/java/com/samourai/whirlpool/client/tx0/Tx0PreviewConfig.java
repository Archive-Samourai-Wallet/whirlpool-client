package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0PreviewConfig {
  private Tx0PreviewService tx0PreviewService;
  private Collection<Pool>
      pools; // list of pools being loaded by poolSupplier.computePools() (pool.tx0PreviewMinimal
  // not yet set)
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private boolean cascading;

  public Tx0PreviewConfig(
      Tx0PreviewService tx0PreviewService,
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget) {
    this.tx0PreviewService = tx0PreviewService;
    this.pools = pools;
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.cascading = false;
  }

  public Tx0Param getTx0Param(final String poolId) {
    // find pool
    Pool pool = pools.stream().filter(pool1 -> pool1.getPoolId().equals(poolId)).findFirst().get();
    return tx0PreviewService.getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
  }

  public Collection<Pool> getPools() {
    return pools;
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

  public boolean isCascading() {
    return cascading;
  }

  public void setCascading(boolean cascading) {
    this.cascading = cascading;
  }
}
