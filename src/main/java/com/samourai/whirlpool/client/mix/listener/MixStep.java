package com.samourai.whirlpool.client.mix.listener;

public enum MixStep {
  CONNECTING("connecting", 0, true),
  REGISTER_INPUT("waiting for a mix...", 20, true),

  CONFIRM_INPUT("joined a mix", 40, true),

  REGISTER_OUTPUT("registered output", 60, false),

  SIGN("signed", 80, false),

  SUCCESS("mix success", 100, true),
  FAIL("mix failed", 100, true);

  private String message;
  private int progressPercent;
  private boolean interruptable;

  MixStep(String message, int progressPercent, boolean interruptable) {
    this.message = message;
    this.progressPercent = progressPercent;
    this.interruptable = interruptable;
  }

  public String getMessage() {
    return message;
  }

  public int getProgressPercent() {
    return progressPercent;
  }

  public boolean isInterruptable() {
    return interruptable;
  }
}
