package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;

public class Tx0PreviewConfig {
  private Tx0PreviewService tx0PreviewService;
  private Collection<Pool>
      pools; // list of pools being loaded by poolSupplier.computePools() (pool.tx0PreviewMinimal
  // not yet set)
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;

  public Tx0PreviewConfig(
      Tx0PreviewService tx0PreviewService,
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget) {
    this.tx0PreviewService = tx0PreviewService;
    this.pools = pools;
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
  }

  public Tx0Param getTx0Param(final String poolId) {
    // find pool
    Pool pool =
        StreamSupport.stream(pools)
            .filter(
                new Predicate<Pool>() {
                  @Override
                  public boolean test(Pool pool) {
                    return pool.getPoolId().equals(poolId);
                  }
                })
            .findFirst()
            .get();
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
}
