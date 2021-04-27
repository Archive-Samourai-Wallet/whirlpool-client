package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;

public class WalletStartEvent extends WhirlpoolEvent {
  private UtxoData utxoData;

  public WalletStartEvent(UtxoData utxoData) {
    super();
    this.utxoData = utxoData;
  }

  public UtxoData getUtxoData() {
    return utxoData;
  }
}
