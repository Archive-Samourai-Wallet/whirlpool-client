package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pool {
  private final Logger log = LoggerFactory.getLogger(Pool.class);

  private String poolId;
  private long denomination;
  private long feeValue;
  private long premixValue;
  private long premixValueMin;
  private long premixValueMax;
  private int tx0MaxOutputs;
  private int anonymitySet;
  private Tx0Preview tx0PreviewMin;

  public Pool() {}

  public boolean isPremix(long inputBalance, boolean liquidity) {
    long minBalance = computePremixBalanceMin(liquidity);
    long maxBalance = computePremixBalanceMax(liquidity);
    return inputBalance >= minBalance && inputBalance <= maxBalance;
  }

  public boolean isTx0Possible(long inputBalance) {
    return tx0PreviewMin != null && inputBalance >= tx0PreviewMin.getSpendValue();
  }

  public long getTx0PreviewMinSpendValue() {
    if (tx0PreviewMin == null) {
      log.error("getTx0MinSpendValue() failed: tx0PreviewMin is NULL!");
      return 0; // shouldn't happen
    }
    return tx0PreviewMin.getSpendValue();
  }

  public long computePremixBalanceMin(boolean liquidity) {
    return WhirlpoolProtocol.computePremixBalanceMin(denomination, premixValueMin, liquidity);
  }

  public long computePremixBalanceMax(boolean liquidity) {
    return WhirlpoolProtocol.computePremixBalanceMax(denomination, premixValueMax, liquidity);
  }

  public String getPoolId() {
    return poolId;
  }

  public void setPoolId(String poolId) {
    this.poolId = poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public void setDenomination(long denomination) {
    this.denomination = denomination;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public void setFeeValue(long feeValue) {
    this.feeValue = feeValue;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public void setPremixValue(long premixValue) {
    this.premixValue = premixValue;
  }

  public long getPremixValueMin() {
    return premixValueMin;
  }

  public void setPremixValueMin(long premixValueMin) {
    this.premixValueMin = premixValueMin;
  }

  public long getPremixValueMax() {
    return premixValueMax;
  }

  public void setPremixValueMax(long premixValueMax) {
    this.premixValueMax = premixValueMax;
  }

  public int getTx0MaxOutputs() {
    return tx0MaxOutputs;
  }

  public void setTx0MaxOutputs(int tx0MaxOutputs) {
    this.tx0MaxOutputs = tx0MaxOutputs;
  }

  public int getAnonymitySet() {
    return anonymitySet;
  }

  public void setAnonymitySet(int anonymitySet) {
    this.anonymitySet = anonymitySet;
  }

  /**
   * @return smallest possible Tx0Preview for pool (without taking SCODE into account)
   */
  public Tx0Preview getTx0PreviewMin() {
    return tx0PreviewMin;
  }

  public void setTx0PreviewMin(Tx0Preview tx0Min) {
    this.tx0PreviewMin = tx0Min;
  }
}
