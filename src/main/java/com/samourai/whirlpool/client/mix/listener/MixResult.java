package com.samourai.whirlpool.client.mix.listener;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MixResult {
  private static final Logger log = LoggerFactory.getLogger(MixResult.class);
  private String poolId;
  private long denomination;
  private WhirlpoolUtxo whirlpoolUtxo;
  private MixDestination destination;

  public MixResult(MixParams mixParams) {
    this.poolId = mixParams.getPoolId();
    this.denomination = mixParams.getDenomination();
    this.whirlpoolUtxo = mixParams.getWhirlpoolUtxo();
    try {
      this.destination = mixParams.getPostmixHandler().getDestination();
    } catch (Exception e) {
      log.error("", e); // should never happen
    }
  }

  public String getPoolId() {
    return poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public MixDestination getDestination() {
    return destination;
  }
}
