package com.samourai.whirlpool.client.exception;

public class EmptyWalletException extends Exception {

  public EmptyWalletException(String message) {
    super(message);
  }

  public String getMessageDeposit(String depositAddress) {
    return "Insufficient balance to continue.\nPlease make a deposit to "
        + depositAddress
        + "\nCaused by: "
        + getMessage();
  }
}
