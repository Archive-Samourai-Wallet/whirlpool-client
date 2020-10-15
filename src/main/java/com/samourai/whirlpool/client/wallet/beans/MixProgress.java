package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.mix.listener.MixStep;

public class MixProgress {
  private MixStep mixStep;
  private int progressPercent;
  private long since;

  public MixProgress(MixStep mixStep) {
    this.mixStep = mixStep;
    this.progressPercent = mixStep.getProgress();
    this.since = System.currentTimeMillis();
  }

  public MixStep getMixStep() {
    return mixStep;
  }

  public int getProgressPercent() {
    return progressPercent;
  }

  public long getSince() {
    return since;
  }

  @Override
  public String toString() {
    return progressPercent + "%: " + mixStep;
  }
}
