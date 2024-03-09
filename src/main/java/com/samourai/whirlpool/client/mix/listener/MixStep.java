package com.samourai.whirlpool.client.mix.listener;

public enum MixStep {
  REGISTER_INPUT(20, true),

  CONFIRM_INPUT(40, true),

  REGISTER_OUTPUT(60, false),

  SIGN(80, false),

  SUCCESS(100, true),
  FAIL(100, true);
  private int progressPercent;
  private boolean interruptable;

  MixStep(int progressPercent, boolean interruptable) {
    this.progressPercent = progressPercent;
    this.interruptable = interruptable;
  }

  public String getMessage() {
    return name() + " (" + progressPercent + "%)";
  }

  public int getProgressPercent() {
    return progressPercent;
  }

  public boolean isInterruptable() {
    return interruptable;
  }
}
