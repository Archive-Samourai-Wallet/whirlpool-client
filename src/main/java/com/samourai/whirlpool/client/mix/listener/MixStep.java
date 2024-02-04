package com.samourai.whirlpool.client.mix.listener;

public enum MixStep {
  REGISTERING_INPUT("waiting for a mix...", 10, true),

  CONFIRMING_INPUT("joining a mix...", 30, true),
  CONFIRMED_INPUT("joined a mix", 50, true),

  REGISTERING_OUTPUT("registering output", 60, false),
  REGISTERED_OUTPUT("registered output", 70, false),

  SIGNING("signing", 80, false),
  SIGNED("signed", 90, false),

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
