package com.samourai.whirlpool.client.exception;

import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.protocol.payload.SorobanErrorMessage;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifiableException extends Exception {
  private static final Logger log = LoggerFactory.getLogger(NotifiableException.class);
  private static final int STATUS_DEFAULT = 500;

  private int status;

  public NotifiableException(String message, Exception cause) {
    this(message, cause, STATUS_DEFAULT);
  }

  public NotifiableException(String message) {
    this(message, null, STATUS_DEFAULT);
  }

  public NotifiableException(String message, int status) {
    this(message, null, status);
  }

  public NotifiableException(String message, Exception cause, int status) {
    super(message, cause);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static NotifiableException computeNotifiableException(Exception e) {
    NotifiableException notifiableException = findNotifiableException(e);
    if (notifiableException == null && e.getCause() != null) {
      notifiableException = findNotifiableException(e.getCause());
    }
    if (notifiableException == null) {
      log.warn("Exception obfuscated to user", e);
      notifiableException = new NotifiableException("Technical error, check logs for details");
    }
    return notifiableException;
  }

  protected static NotifiableException findNotifiableException(Throwable e) {
    if (NotifiableException.class.isAssignableFrom(e.getClass())) {
      return (NotifiableException) e;
    }
    if (TimeoutException.class.isAssignableFrom(e.getClass())) {
      return new NotifiableException("Request timed out");
    }
    if (AsynchronousCloseException.class.isAssignableFrom(e.getClass())) {
      return new NotifiableException("Network error");
    }
    if (SorobanErrorMessageException.class.isAssignableFrom(e.getClass())) {
      SorobanErrorMessage sorobanErrorMessage =
          ((SorobanErrorMessageException) e).getSorobanErrorMessage();
      return new NotifiableException(
          "Error " + sorobanErrorMessage.errorCode + ": " + sorobanErrorMessage.message);
    }
    if (e.getCause() != null) {
      return findNotifiableException(e.getCause());
    }
    return null;
  }
}
