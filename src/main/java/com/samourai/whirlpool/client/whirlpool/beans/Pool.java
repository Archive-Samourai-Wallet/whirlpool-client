package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pool {
  private final Logger log = LoggerFactory.getLogger(Pool.class);

  private String poolId;
  private long denomination;
  private long feeValue;
  private long mustMixBalanceMin;
  private long mustMixBalanceCap;
  private long mustMixBalanceMax;
  private int minAnonymitySet;
  private int minMustMix;
  private int tx0MaxOutputs;
  private int nbRegistered;

  private int mixAnonymitySet;
  private MixStatus mixStatus;
  private long elapsedTime;
  private int nbConfirmed;
  private Tx0Preview tx0PreviewMin;
  private Tx0Preview tx0PreviewMax;
  private Long tx0PreviewMaxSpendValueCascading;

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
      // shouldn't happen
      throw new RuntimeException("pool.tx0PreviewMin is NULL!");
    }
    return tx0PreviewMin.getSpendValue();
  }

  public long getTx0PreviewMaxSpendValue() {
    if (tx0PreviewMax == null) {
      // shouldn't happen
      throw new RuntimeException("pool.tx0PreviewMax is NULL!");
    }
    return tx0PreviewMax.getSpendValue();
  }

  public long getTx0PreviewMaxSpendValueCascading() {
    if (tx0PreviewMaxSpendValueCascading == null) {
      // shouldn't happen
      throw new RuntimeException("pool.tx0PreviewMaxSpendValueCascading is NULL!");
    }
    return tx0PreviewMaxSpendValueCascading;
  }

  public long computePremixBalanceMin(boolean liquidity) {
    return WhirlpoolProtocol.computePremixBalanceMin(denomination, mustMixBalanceMin, liquidity);
  }

  public long computePremixBalanceMax(boolean liquidity) {
    return WhirlpoolProtocol.computePremixBalanceMax(denomination, mustMixBalanceMax, liquidity);
  }

  public long computePremixBalanceCap(boolean liquidity) {
    return WhirlpoolProtocol.computePremixBalanceMax(denomination, mustMixBalanceCap, liquidity);
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

  public long getMustMixBalanceMin() {
    return mustMixBalanceMin;
  }

  public void setMustMixBalanceMin(long mustMixBalanceMin) {
    this.mustMixBalanceMin = mustMixBalanceMin;
  }

  public long getMustMixBalanceCap() {
    return mustMixBalanceCap;
  }

  public void setMustMixBalanceCap(long mustMixBalanceCap) {
    this.mustMixBalanceCap = mustMixBalanceCap;
  }

  public long getMustMixBalanceMax() {
    return mustMixBalanceMax;
  }

  public void setMustMixBalanceMax(long mustMixBalanceMax) {
    this.mustMixBalanceMax = mustMixBalanceMax;
  }

  public int getMinAnonymitySet() {
    return minAnonymitySet;
  }

  public void setMinAnonymitySet(int minAnonymitySet) {
    this.minAnonymitySet = minAnonymitySet;
  }

  public int getMinMustMix() {
    return minMustMix;
  }

  public void setMinMustMix(int minMustMix) {
    this.minMustMix = minMustMix;
  }

  public int getTx0MaxOutputs() {
    return tx0MaxOutputs;
  }

  public void setTx0MaxOutputs(int tx0MaxOutputs) {
    this.tx0MaxOutputs = tx0MaxOutputs;
  }

  public int getNbRegistered() {
    return nbRegistered;
  }

  public void setNbRegistered(int nbRegistered) {
    this.nbRegistered = nbRegistered;
  }

  public int getMixAnonymitySet() {
    return mixAnonymitySet;
  }

  public void setMixAnonymitySet(int mixAnonymitySet) {
    this.mixAnonymitySet = mixAnonymitySet;
  }

  public MixStatus getMixStatus() {
    return mixStatus;
  }

  public void setMixStatus(MixStatus mixStatus) {
    this.mixStatus = mixStatus;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public int getNbConfirmed() {
    return nbConfirmed;
  }

  public void setNbConfirmed(int nbConfirmed) {
    this.nbConfirmed = nbConfirmed;
  }

  /** @return smallest possible Tx0Preview for pool (without taking SCODE into account) */
  public Tx0Preview getTx0PreviewMin() {
    return tx0PreviewMin;
  }

  public void setTx0PreviewMin(Tx0Preview tx0Min) {
    this.tx0PreviewMin = tx0Min;
  }

  /** @return Tx0Preview for pool with maxOutputs (without taking SCODE into account) */
  public Tx0Preview getTx0PreviewMax() {
    return tx0PreviewMax;
  }

  public void setTx0PreviewMax(Tx0Preview tx0PreviewMax) {
    this.tx0PreviewMax = tx0PreviewMax;
  }

  public void setTx0PreviewMaxSpendValueCascading(long tx0PreviewMaxSpendValueCascading) {
    this.tx0PreviewMaxSpendValueCascading = tx0PreviewMaxSpendValueCascading;
  }
}
