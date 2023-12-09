package com.samourai.whirlpool.client.exception;

import com.samourai.soroban.client.exception.SorobanPayloadException;
import com.samourai.whirlpool.protocol.soroban.FailMixNotification;

public class FailMixNotificationException extends SorobanPayloadException {

  public FailMixNotificationException(FailMixNotification failMixNotification) {
    super(failMixNotification);
  }

  public FailMixNotification getFailMixNotification() {
    return (FailMixNotification) getSorobanPayload();
  }
}
