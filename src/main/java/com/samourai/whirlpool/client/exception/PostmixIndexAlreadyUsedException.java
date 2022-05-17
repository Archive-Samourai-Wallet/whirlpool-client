package com.samourai.whirlpool.client.exception;

import com.samourai.whirlpool.client.wallet.PostmixIndexService;

public class PostmixIndexAlreadyUsedException extends NotifiableException {
  private int postmixIndex;

  public PostmixIndexAlreadyUsedException(int postmixIndex) {
    super(PostmixIndexService.CHECKOUTPUT_ERROR_OUTPUT_ALREADY_REGISTERED);
    this.postmixIndex = postmixIndex;
  }

  public int getPostmixIndex() {
    return postmixIndex;
  }
}
