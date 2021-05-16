package com.samourai.whirlpool.client.exception;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

public class UnconfirmedUtxoException extends NotifiableException {
  private WhirlpoolUtxo whirlpoolUtxo;

  public UnconfirmedUtxoException(WhirlpoolUtxo whirlpoolUtxo) {
    super("Utxo is unconfirmed: " + whirlpoolUtxo.getUtxo());
    this.whirlpoolUtxo = whirlpoolUtxo;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }
}
