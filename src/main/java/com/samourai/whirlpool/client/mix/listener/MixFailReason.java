package com.samourai.whirlpool.client.mix.listener;

public enum MixFailReason {
  MIX_FAILED("Mix failed", false, true, true),
  NETWORK_ERROR("Network error", false, true, true),
  INPUT_REJECTED("Input rejected", false, true, true),
  INTERNAL_ERROR("Internal error", false, true, true),
  ROTATE("Rotate", true, true, false),
  STOP_MIXING("Mixing halted", true, true, false),
  STOP_UTXO("Mixing UTXO halted", false, false, false);

  private String message;
  private boolean silent;
  private boolean requeue;
  private boolean error;

  MixFailReason(String message, boolean silent, boolean requeue, boolean error) {
    this.message = message;
    this.silent = silent;
    this.requeue = requeue;
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public boolean isSilent() {
    return silent;
  }

  public boolean isRequeue() {
    return requeue;
  }

  public boolean isError() {
    return error;
  }
}
