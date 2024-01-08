package com.samourai.whirlpool.client.exception;

public class PostmixIndexAlreadyUsedException extends NotifiableException {
  private int postmixIndex;

  public PostmixIndexAlreadyUsedException(int postmixIndex) {
    super("PostmixIndex already used: " + postmixIndex);
    this.postmixIndex = postmixIndex;
  }

  public int getPostmixIndex() {
    return postmixIndex;
  }
}
