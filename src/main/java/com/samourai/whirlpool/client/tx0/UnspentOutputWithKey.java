package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;

public class UnspentOutputWithKey extends UnspentOutput {
  private byte[] key;

  public UnspentOutputWithKey(UnspentOutput uo, byte[] key) {
    super(uo);
    this.key = key;
  }

  byte[] getKey() {
    return key;
  }
}
