package com.samourai.whirlpool.client.mix.listener;

public enum MixFailReason {
  MIX_FAILED("Mix failed", true, true),
  NETWORK_ERROR("Network error", true, true),
  INPUT_REJECTED("Input rejected", true, true),
  INTERNAL_ERROR("Internal error", true, true),
  ROTATE("Rotate", true, false),
  STOP_MIXING("Mixing halted", true, false),
  STOP_UTXO("Mixing UTXO halted", false, false);

  private String message;
  private boolean requeue;
  private boolean error;

  MixFailReason(String message, boolean requeue, boolean error) {
    this.message = message;
    this.requeue = requeue;
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public boolean isRequeue() {
    return requeue;
  }

  public boolean isError() {
    return error;
  }
}
