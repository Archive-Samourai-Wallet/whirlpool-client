package com.samourai.whirlpool.client.exception;

import com.samourai.soroban.client.exception.SorobanPayloadException;
import com.samourai.whirlpool.protocol.soroban.RevealOutputNotification;

public class RevealOutputNotificationException extends SorobanPayloadException {

  public RevealOutputNotificationException(RevealOutputNotification revealOutputNotification) {
    super(revealOutputNotification);
  }

  public RevealOutputNotification getRevealOutputNotification() {
    return (RevealOutputNotification) getSorobanPayload();
  }
}
