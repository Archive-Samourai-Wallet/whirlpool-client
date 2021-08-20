package com.samourai.whirlpool.client.mix.listener;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.protocol.beans.Utxo;

public class MixSuccess extends MixResult {
  private Utxo receiveUtxo;

  public MixSuccess(MixParams mixParams, Utxo receiveUtxo) {
    super(mixParams);
    this.receiveUtxo = receiveUtxo;
  }

  public Utxo getReceiveUtxo() {
    return receiveUtxo;
  }
}
