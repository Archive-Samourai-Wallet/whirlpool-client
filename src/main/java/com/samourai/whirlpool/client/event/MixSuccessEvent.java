package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.protocol.beans.Utxo;

public class MixSuccessEvent extends AbstractMixEvent {
  private Utxo receiveUtxo;
  private MixDestination receiveDestination;

  public MixSuccessEvent(
      WhirlpoolWallet whirlpoolWallet,
      MixParams mixParams,
      Utxo receiveUtxo,
      MixDestination receiveDestination) {
    super(whirlpoolWallet, mixParams);
    this.receiveUtxo = receiveUtxo;
    this.receiveDestination = receiveDestination;
  }

  public Utxo getReceiveUtxo() {
    return receiveUtxo;
  }

  public MixDestination getReceiveDestination() {
    return receiveDestination;
  }
}
