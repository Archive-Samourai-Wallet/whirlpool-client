package com.samourai.whirlpool.client.event;

import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class MixSuccessEvent extends AbstractMixEvent {
  private UtxoDetail receiveUtxo;

  public MixSuccessEvent(
      WhirlpoolWallet whirlpoolWallet, MixParams mixParams, UtxoDetail receiveUtxo) {
    super(whirlpoolWallet, mixParams);
    this.receiveUtxo = receiveUtxo;
  }

  public UtxoDetail getReceiveUtxo() {
    return receiveUtxo;
  }
}
