package com.samourai.whirlpool.client.mix.listener;

public enum MixFailReason {
  PROTOCOL_MISMATCH("Protocol mismatch (check for updates!)", false),
  MIX_FAILED("Mix failed", false),
  DISCONNECTED("Disconnected", false),
  INPUT_REJECTED("Input rejected", false),
  INTERNAL_ERROR("Internal error", false),
  CANCEL("Cancelled", true), // silent stop
  STOP("Stopped", false);

  private String message;
  private boolean silent;

  MixFailReason(String message, boolean silent) {
    this.message = message;
    this.silent = silent;
  }

  public String getMessage() {
    return message;
  }

  public boolean isSilent() {
    return silent;
  }
}
