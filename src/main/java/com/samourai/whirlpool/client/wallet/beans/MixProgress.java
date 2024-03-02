package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixProgress {
  private static final Logger log = LoggerFactory.getLogger(MixProgress.class);
  private MixParams mixParams;
  private MixStep mixStep;
  private String mixId;
  private long since;

  public MixProgress(MixParams mixParams, MixStep mixStep, String mixId) {
    this.mixParams = mixParams;
    this.mixStep = mixStep;
    this.mixId = mixId;
    this.since = System.currentTimeMillis();
  }

  public long getDenomination() {
    return mixParams.getDenomination();
  }

  public MixStep getMixStep() {
    return mixStep;
  }

  public String getMixId() {
    return mixId;
  }

  public long getSince() {
    return since;
  }

  public PaymentCode getSorobanSender() {
    return mixParams
        .getWhirlpoolApiClient()
        .getRpcSession()
        .getRpcWallet()
        .getBip47Account()
        .getPaymentCode();
  }

  @Override
  public String toString() {
    return mixStep.getProgressPercent() + "%: " + mixStep;
  }
}
