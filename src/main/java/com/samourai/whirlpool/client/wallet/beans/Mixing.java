package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.WhirlpoolClient;
import io.reactivex.Observable;

public class Mixing {
  private WhirlpoolUtxo utxo;
  private WhirlpoolClient whirlpoolClient;
  private Observable<MixProgress> observable;
  private long since;

  public Mixing(
      WhirlpoolUtxo utxo, WhirlpoolClient whirlpoolClient, Observable<MixProgress> observable) {
    this.utxo = utxo;
    this.whirlpoolClient = whirlpoolClient;
    this.observable = observable;
    this.since = System.currentTimeMillis();
  }

  public WhirlpoolUtxo getUtxo() {
    return utxo;
  }

  public WhirlpoolClient getWhirlpoolClient() {
    return whirlpoolClient;
  }

  public Observable<MixProgress> getObservable() {
    return observable;
  }

  public long getSince() {
    return since;
  }

  @Override
  public String toString() {
    return "utxo=[" + utxo + "]";
  }
}
