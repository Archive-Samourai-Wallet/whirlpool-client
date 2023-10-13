package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoOutPoint;

public class Tx0x2CahootsResult extends Tx0x2Preview {
  private BipUtxo senderChangeUtxo;
  private UtxoOutPoint counterpartyChangeOutPoint;

  public Tx0x2CahootsResult(
      Tx0x2Preview tx0x2Preview,
      BipUtxo senderChangeUtxo,
      UtxoOutPoint counterpartyChangeOutPoint) {
    super(tx0x2Preview);
    this.senderChangeUtxo = senderChangeUtxo;
    this.counterpartyChangeOutPoint = counterpartyChangeOutPoint;
  }

  public BipUtxo getSenderChangeUtxo() {
    return senderChangeUtxo;
  }

  public UtxoOutPoint getCounterpartyChangeOutPoint() {
    return counterpartyChangeOutPoint;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", senderChangeUtxo="
        + (senderChangeUtxo != null ? "yes" : "no")
        + ", counterpartyChangeOutPoint="
        + (counterpartyChangeOutPoint != null ? "yes" : "no");
  }
}
