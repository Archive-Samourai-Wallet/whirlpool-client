package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0InfoConfig {
  private Tx0PreviewService tx0PreviewService;
  private Collection<Pool>
      pools; // list of pools being loaded by poolSupplier.computePools() (pool.tx0PreviewMinimal
  // not yet set)
  private int tx0AttemptsAddressReuse;
  private int tx0AttemptsSoroban;

  private Tx0 cascadingParent; // set when cascading

  public Tx0InfoConfig(
      Tx0PreviewService tx0PreviewService,
      Collection<Pool> pools,
      int tx0AttemptsAddressReuse,
      int tx0AttemptsSoroban) {
    this.tx0PreviewService = tx0PreviewService;
    this.pools = pools;
    this.tx0AttemptsAddressReuse = tx0AttemptsAddressReuse;
    this.tx0AttemptsSoroban = tx0AttemptsSoroban;
  }

  public Pool findPool(final String poolId) {
    // find pool
    return pools.stream().filter(pool1 -> pool1.getPoolId().equals(poolId)).findFirst().get();
  }

  public Collection<Pool> getPools() {
    return pools;
  }

  public Tx0 getCascadingParent() {
    return cascadingParent;
  }

  protected Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
  }

  public void setCascadingParent(Tx0 cascadingParent) {
    this.cascadingParent = cascadingParent;
  }

  public int getTx0AttemptsAddressReuse() {
    return tx0AttemptsAddressReuse;
  }

  public void setTx0AttemptsAddressReuse(int tx0AttemptsAddressReuse) {
    this.tx0AttemptsAddressReuse = tx0AttemptsAddressReuse;
  }

  public int getTx0AttemptsSoroban() {
    return tx0AttemptsSoroban;
  }

  public void setTx0AttemptsSoroban(int tx0AttemptsSoroban) {
    this.tx0AttemptsSoroban = tx0AttemptsSoroban;
  }
}
