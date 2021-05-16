package com.samourai.whirlpool.client.wallet.beans;

import java.util.ArrayList;
import java.util.List;

public class WhirlpoolUtxoChanges {
  private boolean isFirstFetch;
  private List<WhirlpoolUtxo> utxosAdded;
  private List<WhirlpoolUtxo> utxosConfirmed;
  private List<WhirlpoolUtxo> utxosRemoved;

  public WhirlpoolUtxoChanges(boolean isFirstFetch) {
    this.isFirstFetch = isFirstFetch;
    this.utxosAdded = new ArrayList<WhirlpoolUtxo>();
    this.utxosConfirmed = new ArrayList<WhirlpoolUtxo>();
    this.utxosRemoved = new ArrayList<WhirlpoolUtxo>();
  }

  public boolean isEmpty() {
    return utxosAdded.isEmpty() && utxosConfirmed.isEmpty() && utxosRemoved.isEmpty();
  }

  public boolean isFirstFetch() {
    return isFirstFetch;
  }

  public List<WhirlpoolUtxo> getUtxosAdded() {
    return utxosAdded;
  }

  public List<WhirlpoolUtxo> getUtxosConfirmed() {
    return utxosConfirmed;
  }

  public List<WhirlpoolUtxo> getUtxosRemoved() {
    return utxosRemoved;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "unchanged";
    }
    return utxosAdded.size()
        + " added, "
        + utxosConfirmed.size()
        + " confirmed, "
        + utxosRemoved.size()
        + " removed";
  }
}
