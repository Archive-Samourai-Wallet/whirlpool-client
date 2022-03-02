package com.samourai.whirlpool.client.exception;

import com.samourai.whirlpool.protocol.rest.PushTxErrorResponse;

public class PushTxErrorResponseException extends NotifiableException {
  private PushTxErrorResponse pushTxErrorResponse;

  public PushTxErrorResponseException(PushTxErrorResponse pushTxErrorResponse) {
    super(pushTxErrorResponse.message);
    this.pushTxErrorResponse = pushTxErrorResponse;
  }

  public PushTxErrorResponse getPushTxErrorResponse() {
    return pushTxErrorResponse;
  }
}
