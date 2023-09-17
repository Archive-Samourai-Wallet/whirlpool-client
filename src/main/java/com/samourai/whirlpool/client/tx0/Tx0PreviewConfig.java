package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0PreviewConfig {
  // list of pools being loaded by poolSupplier.computePools() (pool.tx0PreviewMinimal not yet set)
  private Collection<Pool> pools;
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private boolean decoyTx0x2;
  private boolean _cascading; // internally set when cascading
  private Collection<? extends UtxoDetail> spendFroms; // may be NULL for general pools preview

  public Tx0PreviewConfig(
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<? extends UtxoDetail> spendFroms) {
    this.pools = pools;
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.decoyTx0x2 = true;
    this._cascading = false;
    this.spendFroms = spendFroms;
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

  public boolean isDecoyTx0x2() {
    return decoyTx0x2;
  }

  public void setDecoyTx0x2(boolean decoyTx0x2) {
    this.decoyTx0x2 = decoyTx0x2;
  }

  public boolean _isCascading() {
    return _cascading;
  }

  public void _setCascading(boolean _cascading) {
    this._cascading = _cascading;
  }

  public Collection<? extends UtxoDetail> getSpendFroms() {
    return spendFroms;
  }

  public void setSpendFroms(Collection<? extends UtxoDetail> spendFroms) {
    this.spendFroms = spendFroms;
  }

  @Override
  public String toString() {
    return "pools="
        + pools.stream().map(p -> p.getPoolId()).toArray()
        + ", tx0FeeTarget="
        + tx0FeeTarget
        + ", mixFeeTarget="
        + mixFeeTarget
        + ", _cascading="
        + _cascading
        + ", decoyTx0x2="
        + decoyTx0x2
        + ", spendFroms="
        + (spendFroms != null ? spendFroms.size() + " utxos" : "null");
  }
}
