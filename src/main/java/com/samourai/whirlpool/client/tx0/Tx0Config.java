package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private Collection<? extends BipUtxo> spendFromUtxos; // not NULL
  private WhirlpoolAccount changeWallet;
  private Pool pool;

  public Tx0Config(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<? extends BipUtxo> spendFromUtxos,
      WhirlpoolAccount changeWallet,
      Pool pool) {
    super(tx0FeeTarget, mixFeeTarget, spendFromUtxos);
    this.spendFromUtxos = spendFromUtxos;
    this.changeWallet = changeWallet;
    this.pool = pool;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config) {
    super(tx0Config);
    this.changeWallet = tx0Config.changeWallet;
    this.spendFromUtxos = tx0Config.spendFromUtxos;
    this.pool = tx0Config.pool;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config, Collection<? extends BipUtxo> spendFromUtxos, Pool pool) {
    super(tx0Config, spendFromUtxos);
    this.changeWallet = tx0Config.changeWallet;
    this.spendFromUtxos = spendFromUtxos;
    this.pool = pool;
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

  public Pool getPool() {
    return pool;
  }

  @Override
  public String toString() {
    return super.toString() + ", changeWallet=" + changeWallet;
  }
}
