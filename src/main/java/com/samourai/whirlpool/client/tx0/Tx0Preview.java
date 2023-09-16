package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Collection;

public class Tx0Preview {
  private Pool pool;
  private Tx0Data tx0Data; // may be null
  private int tx0Size;
  private long tx0MinerFee;
  private long mixMinerFee;
  private long premixMinerFee;
  private int tx0MinerFeePrice;
  private int mixMinerFeePrice;
  private long feeValue;
  private long feeChange;
  private int feeDiscountPercent;
  private long premixValue;
  private long changeValue;
  private int nbPremix;
  private long spendValue; // all except change
  private long totalValue; // with change
  private Collection<Long> changeAmounts;
  private boolean decoyTx0x2;

  public Tx0Preview(Tx0Preview tx0Preview) throws Exception {
    this(
        tx0Preview.pool,
        tx0Preview.tx0Data,
        tx0Preview.tx0Size,
        tx0Preview.tx0MinerFee,
        tx0Preview.mixMinerFee,
        tx0Preview.premixMinerFee,
        tx0Preview.tx0MinerFeePrice,
        tx0Preview.mixMinerFeePrice,
        tx0Preview.premixValue,
        tx0Preview.changeValue,
        tx0Preview.nbPremix,
        tx0Preview.changeAmounts,
        tx0Preview.decoyTx0x2);
  }

  public Tx0Preview(
      Pool pool,
      Tx0Data tx0Data,
      int tx0Size,
      long tx0MinerFee,
      long mixMinerFee,
      long premixMinerFee,
      int tx0MinerFeePrice,
      int mixMinerFeePrice,
      long premixValue,
      long changeValue,
      int nbPremix,
      Collection<Long> changeAmounts,
      boolean decoyTx0x2)
      throws Exception {
    this.pool = pool;
    this.tx0Data = tx0Data;
    this.tx0Size = tx0Size;
    this.tx0MinerFee = tx0MinerFee;
    this.mixMinerFee = mixMinerFee;
    this.premixMinerFee = premixMinerFee;
    this.tx0MinerFeePrice = tx0MinerFeePrice;
    this.mixMinerFeePrice = mixMinerFeePrice;
    this.feeValue = tx0Data != null ? tx0Data.getFeeValue() : pool.getFeeValue();
    this.feeChange = tx0Data != null ? tx0Data.getFeeChange() : 0;
    this.feeDiscountPercent = tx0Data != null ? tx0Data.getFeeDiscountPercent() : 0;
    this.premixValue = premixValue;
    this.changeValue = changeValue;
    this.nbPremix = nbPremix;
    long feeValueOrFeeChange =
        tx0Data != null ? tx0Data.computeFeeValueOrFeeChange() : pool.getFeeValue();
    this.spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    this.totalValue = spendValue + changeValue;
    this.changeAmounts = changeAmounts;
    this.decoyTx0x2 = decoyTx0x2;
    this.consistencyCheck();
  }

  private void consistencyCheck() throws Exception {
    if (changeValue < 0) {
      throw new Exception(
          "Negative change detected, please report this bug. tx0Preview=" + totalValue);
    }

    if (changeAmounts.stream().mapToLong(v -> v).sum() != changeValue) {
      throw new Exception(
          "Invalid changeAmounts=" + changeAmounts + " vs changeValue=" + changeValue);
    }
  }

  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  // used by Sparrow
  public Tx0Data getTx0Data() {
    return tx0Data;
  }

  public int getTx0Size() {
    return tx0Size;
  }

  public long getTx0MinerFee() {
    return tx0MinerFee;
  }

  public long getMixMinerFee() {
    return mixMinerFee;
  }

  public long getPremixMinerFee() {
    return premixMinerFee;
  }

  public int getTx0MinerFeePrice() {
    return tx0MinerFeePrice;
  }

  public int getMixMinerFeePrice() {
    return mixMinerFeePrice;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public long getFeeChange() {
    return feeChange;
  }

  public int getFeeDiscountPercent() {
    return feeDiscountPercent;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public long getChangeValue() {
    return changeValue;
  }

  public int getNbPremix() {
    return nbPremix;
  }

  public long getSpendValue() {
    return spendValue;
  }

  public long getTotalValue() {
    return totalValue;
  }

  public Collection<Long> getChangeAmounts() {
    return changeAmounts;
  }

  public boolean isDecoyTx0x2() {
    return decoyTx0x2;
  }

  @Override
  public String toString() {
    return "poolId="
        + pool.getPoolId()
        + ", tx0MinerFee="
        + tx0MinerFee
        + ", mixMinerFee="
        + mixMinerFee
        + ", premixMinerFee="
        + premixMinerFee
        + ", feeValue="
        + feeValue
        + ", feeChange="
        + feeChange
        + ", feeDiscountPercent="
        + feeDiscountPercent
        + ", premixValue="
        + premixValue
        + ", changeValue="
        + changeValue
        + ", nbPremix="
        + nbPremix
        + ", spendValue="
        + spendValue
        + ", totalValue="
        + totalValue
        + ", changeAmounts="
        + changeAmounts
        + ", decoyTx0x2="
        + decoyTx0x2;
  }
}
