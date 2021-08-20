package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.protocol.beans.Utxo;

public class MixProgressSuccess extends MixProgress {
  private MixDestination receiveDestination;
  private Utxo receiveUtxo;

  public MixProgressSuccess(MixDestination receiveDestination, Utxo receiveUtxo) {
    super(MixStep.SUCCESS);
    this.receiveDestination = receiveDestination;
    this.receiveUtxo = receiveUtxo;
  }

  public MixDestination getReceiveDestination() {
    return receiveDestination;
  }

  public Utxo getReceiveUtxo() {
    return receiveUtxo;
  }
}
