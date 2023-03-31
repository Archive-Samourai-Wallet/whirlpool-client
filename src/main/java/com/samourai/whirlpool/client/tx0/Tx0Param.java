package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Param {
  private static final Logger log = LoggerFactory.getLogger(Tx0Param.class);
  private static final FeeUtil feeUtil = FeeUtil.getInstance();

  private int tx0MinerFeePrice;
  private int mixMinerFeePrice;
  private Pool pool;

  // computed
  private long premixValue;

  public Tx0Param(
      int tx0MinerFeePrice, int mixMinerFeePrice, Pool pool, Long overspendValueOrNull) {
    this.tx0MinerFeePrice = tx0MinerFeePrice;
    this.mixMinerFeePrice = mixMinerFeePrice;
    this.pool = pool;
    this.premixValue = computePremixValue(overspendValueOrNull);
  }

  private long computePremixValue(Long overspendValueOrNull) {
    long premixValue = pool.getPremixValue();

    if (overspendValueOrNull != null && overspendValueOrNull > 0) {
      // use premixValue from local config
      long premixOverspend = overspendValueOrNull;

      // make sure premixValue is acceptable for pool
      long premixBalanceMin = pool.computePremixBalanceMin(false);
      long premixBalanceMax = pool.computePremixBalanceMax(false);
      premixValue = pool.getDenomination() + premixOverspend;
      premixValue = Math.min(premixValue, premixBalanceMax);
      premixValue = Math.max(premixValue, premixBalanceMin);
    }
    return premixValue;
  }

  public int getTx0MinerFeePrice() {
    return tx0MinerFeePrice;
  }

  public int getMixMinerFeePrice() {
    return mixMinerFeePrice;
  }

  public Pool getPool() {
    return pool;
  }

  public long getPremixValue() {
    return premixValue;
  }

  @Override
  public String toString() {
    return super.toString() + "pool=" + pool.getPoolId();
  }
}
