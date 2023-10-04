package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private Collection<? extends BipUtxo> spendFromUtxos; // not NULL
  private UtxoKeyProvider utxoKeyProvider;
  private BipWallet premixWallet;
  private BipWallet changeWallet;
  private BipWallet feeChangeWallet;
  private Pool pool;

  public Tx0Config(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<? extends BipUtxo> spendFromUtxos,
      UtxoKeyProvider utxoKeyProvider,
      BipWallet premixWallet,
      BipWallet changeWallet,
      BipWallet feeChangeWallet,
      Pool pool) {
    super(tx0FeeTarget, mixFeeTarget, spendFromUtxos);
    this.spendFromUtxos = spendFromUtxos;
    this.utxoKeyProvider = utxoKeyProvider;
    this.premixWallet = premixWallet;
    this.changeWallet = changeWallet;
    this.feeChangeWallet = feeChangeWallet;
    this.pool = pool;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config) {
    super(tx0Config);
    this.spendFromUtxos = tx0Config.spendFromUtxos;
    this.utxoKeyProvider = tx0Config.utxoKeyProvider;
    this.premixWallet = tx0Config.premixWallet;
    this.changeWallet = tx0Config.changeWallet;
    this.feeChangeWallet = tx0Config.feeChangeWallet;
    this.pool = tx0Config.pool;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config, Collection<? extends BipUtxo> spendFromUtxos, Pool pool) {
    super(tx0Config, spendFromUtxos);
    this.spendFromUtxos = spendFromUtxos;
    this.utxoKeyProvider = tx0Config.utxoKeyProvider;
    this.premixWallet = tx0Config.premixWallet;
    this.changeWallet = tx0Config.changeWallet;
    this.feeChangeWallet = tx0Config.feeChangeWallet;
    this.pool = pool;
    consistencyCheck();
  }

  protected void consistencyCheck() {
    if (spendFromUtxos == null) {
      throw new IllegalArgumentException("Tx0Config.spendFromUtxos cannot be NULL");
    }
  }

  public UtxoKeyProvider getUtxoKeyProvider() {
    return utxoKeyProvider;
  }

  public BipWallet getPremixWallet() {
    return premixWallet;
  }

  public BipWallet getChangeWallet() {
    return changeWallet;
  }

  public void setChangeWallet(BipWallet changeWallet) {
    this.changeWallet = changeWallet;
  }

  public BipWallet getFeeChangeWallet() {
    return feeChangeWallet;
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
