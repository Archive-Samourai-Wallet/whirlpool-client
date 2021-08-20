package com.samourai.whirlpool.client.mix.listener;

import com.samourai.whirlpool.client.mix.MixParams;

public class MixFail extends MixProgress {
  private MixFailReason mixFailReason;
  private String error; // may be null

  public MixFail(MixParams mixParams, MixFailReason mixFailReason, String error) {
    super(mixParams, MixStep.FAIL);
    this.mixFailReason = mixFailReason;
    this.error = error;
  }

  public MixFailReason getMixFailReason() {
    return mixFailReason;
  }

  public String getError() {
    return error;
  }
}
