package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.utils.ClientUtils;

public class ExternalDestination {
  private String xpub;
  private int chain;
  private int startIndex;
  private int mixs;
  private int mixsRandomFactor;

  public ExternalDestination(
      String xpub, int chain, int startIndex, int mixs, int mixsRandomFactor) {
    this.xpub = xpub;
    this.chain = chain;
    this.startIndex = startIndex;
    this.mixs = mixs;
    this.mixsRandomFactor = mixsRandomFactor;
  }

  public String getXpub() {
    return xpub;
  }

  public int getChain() {
    return chain;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public int getMixs() {
    return mixs;
  }

  public boolean useRandomDelay() {
    // 0 => never
    if (mixsRandomFactor == 0) {
      return false;
    }
    // random
    return ClientUtils.random(1, mixsRandomFactor) == 1;
  }

  @Override
  public String toString() {
    return "xpub="
        + ClientUtils.maskString(xpub)
        + ", chain="
        + chain
        + ", startIndex="
        + startIndex
        + ", mixs="
        + mixs
        + ", mixsRandomFactor="
        + mixsRandomFactor;
  }
}
