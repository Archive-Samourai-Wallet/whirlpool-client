package com.samourai.whirlpool.client.wallet.beans;

import java.util.ArrayList;
import java.util.List;

public class WhirlpoolUtxoChanges {
  private boolean isFirstFetch;
  private List<WhirlpoolUtxo> utxosAdded;
  private List<WhirlpoolUtxo> utxosUpdated;
  private List<WhirlpoolUtxo> utxosRemoved;

  public WhirlpoolUtxoChanges(boolean isFirstFetch) {
    this.isFirstFetch = isFirstFetch;
    this.utxosAdded = new ArrayList<WhirlpoolUtxo>();
    this.utxosUpdated = new ArrayList<WhirlpoolUtxo>();
    this.utxosRemoved = new ArrayList<WhirlpoolUtxo>();
  }

  public boolean isEmpty() {
    return utxosAdded.isEmpty() && utxosUpdated.isEmpty() && utxosRemoved.isEmpty();
  }

  public boolean isFirstFetch() {
    return isFirstFetch;
  }

  public List<WhirlpoolUtxo> getUtxosAdded() {
    return utxosAdded;
  }

  public List<WhirlpoolUtxo> getUtxosUpdated() {
    return utxosUpdated;
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
        + utxosUpdated.size()
        + " updated, "
        + utxosRemoved.size()
        + " removed";
  }
}
