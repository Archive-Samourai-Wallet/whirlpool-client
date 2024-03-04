package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.constants.SamouraiAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DestinationType {
  DEPOSIT,
  PREMIX,
  POSTMIX,
  BADBANK,
  XPUB;

  private static final Logger log = LoggerFactory.getLogger(DestinationType.class);

  public static DestinationType find(SamouraiAccount samouraiAccount) {
    switch (samouraiAccount) {
      case DEPOSIT:
        return DestinationType.DEPOSIT;
      case PREMIX:
        return DestinationType.PREMIX;
      case POSTMIX:
        return DestinationType.POSTMIX;
      case BADBANK:
        return DestinationType.BADBANK;
    }
    log.error("Unknown DestinationType for SamouraiAccount: " + samouraiAccount);
    return null;
  }
}
