package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Param {
  private static final Logger log = LoggerFactory.getLogger(Tx0Param.class);
  private static final FeeUtil feeUtil = FeeUtil.getInstance();

  private int feeTx0;
  private int feePremix;
  private Pool pool;
  private Long overspendValueOrNull;

  // computed
  private Long premixValue;

  public Tx0Param(int feeTx0, int feePremix, Pool pool, Long overspendValueOrNull) {
    this.feeTx0 = feeTx0;
    this.feePremix = feePremix;
    this.pool = pool;
    this.overspendValueOrNull = overspendValueOrNull;
    this.premixValue = null;
  }

  private long computePremixValue() {
    long premixOverspend;
    if (overspendValueOrNull != null && overspendValueOrNull > 0) {
      premixOverspend = overspendValueOrNull;
    } else {
      // compute premixOverspend
      long mixFeesEstimate =
          feeUtil.estimatedFeeSegwit(
              0, 0, pool.getMixAnonymitySet(), pool.getMixAnonymitySet(), 0, feePremix);
      premixOverspend = mixFeesEstimate / pool.getMinMustMix();
      if (log.isTraceEnabled()) {
        log.trace(
            "mixFeesEstimate="
                + mixFeesEstimate
                + " => premixOverspend="
                + overspendValueOrNull
                + " for poolId="
                + pool.getPoolId());
      }
    }
    long premixValue = pool.getDenomination() + premixOverspend;

    // make sure destinationValue is acceptable for pool
    long premixBalanceMin = pool.computePremixBalanceMin(false);
    long premixBalanceCap = pool.computePremixBalanceCap(false);
    long premixBalanceMax = pool.computePremixBalanceMax(false);

    long premixValueFinal = premixValue;
    premixValueFinal = Math.min(premixValueFinal, premixBalanceMax);
    premixValueFinal = Math.min(premixValueFinal, premixBalanceCap);
    premixValueFinal = Math.max(premixValueFinal, premixBalanceMin);

    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0Param["
              + pool.getPoolId()
              + "]: premixValueFinal="
              + premixValueFinal
              + ", premixValue="
              + premixValue
              + ", premixOverspend="
              + premixOverspend);
    }
    return premixValueFinal;
  }

  public int getFeeTx0() {
    return feeTx0;
  }

  public int getFeePremix() {
    return feePremix;
  }

  public Pool getPool() {
    return pool;
  }

  public long getPremixValue() {
    if (premixValue == null) {
      premixValue = computePremixValue();
    }
    return premixValue;
  }

  @Override
  public String toString() {
    return super.toString() + "pool=" + pool.getPoolId();
  }
}
