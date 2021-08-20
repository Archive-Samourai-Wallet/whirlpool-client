package com.samourai.whirlpool.client.mix.listener;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.wallet.beans.MixProgressDetail;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixProgress {
  private static final Logger log = LoggerFactory.getLogger(MixProgress.class);
  private String poolId;
  private long denomination;
  private MixProgressDetail mixProgressDetail;
  private WhirlpoolUtxo whirlpoolUtxo;
  private MixDestination destination;

  public MixProgress(MixParams mixParams, MixStep mixStep) {
    this.poolId = mixParams.getPoolId();
    this.denomination = mixParams.getDenomination();
    this.mixProgressDetail = new MixProgressDetail(mixStep);
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

  public MixProgressDetail getMixProgressDetail() {
    return mixProgressDetail;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public MixDestination getDestination() {
    return destination;
  }
}
