package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private WhirlpoolAccount changeWallet;
  private Collection<? extends BipUtxo> spendFromUtxos; // not NULL

  public Tx0Config(
      Collection<Pool> pools,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      WhirlpoolAccount changeWallet,
      Collection<? extends BipUtxo> spendFromUtxos) {
    super(pools, tx0FeeTarget, mixFeeTarget, spendFromUtxos);
    this.changeWallet = changeWallet;
    this.spendFromUtxos = spendFromUtxos;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config, Collection<? extends BipUtxo> spendFromUtxos) {
    super(tx0Config, spendFromUtxos);
    this.changeWallet = tx0Config.changeWallet;
    this.spendFromUtxos = spendFromUtxos;
    consistencyCheck();
  }

  protected void consistencyCheck() {
    if (spendFromUtxos == null) {
      throw new IllegalArgumentException("Tx0Config.spendFromUtxos cannot be NULL");
    }
  }

  public WhirlpoolAccount getChangeWallet() {
    return changeWallet;
  }

  public Tx0Config setChangeWallet(WhirlpoolAccount changeWallet) {
    this.changeWallet = changeWallet;
    return this;
  }

  public Collection<? extends BipUtxo> getSpendFromUtxos() {
    return spendFromUtxos;
  }

  @Override
  public String toString() {
    return super.toString() + ", changeWallet=" + changeWallet;
  }
}
