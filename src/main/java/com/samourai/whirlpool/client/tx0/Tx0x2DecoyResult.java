package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;

public class Tx0x2DecoyResult extends Tx0x2Preview {
  private BipUtxo senderChangeUtxo;
  private BipUtxo counterpartyChangeUtxo;

  public Tx0x2DecoyResult(
      Tx0x2Preview tx0x2Preview, BipUtxo senderChangeUtxo, BipUtxo counterpartyChangeUtxo) {
    super(tx0x2Preview);
    this.senderChangeUtxo = senderChangeUtxo;
    this.counterpartyChangeUtxo = counterpartyChangeUtxo;
  }

  public BipUtxo getSenderChangeUtxo() {
    return senderChangeUtxo;
  }

  public BipUtxo getCounterpartyChangeUtxo() {
    return counterpartyChangeUtxo;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", senderChangeUtxo="
        + (senderChangeUtxo != null ? "yes" : "no")
        + ", counterpartyChangeUtxo="
        + (counterpartyChangeUtxo != null ? "yes" : "no");
  }
}
