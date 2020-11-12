package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;

public abstract class WhirlpoolUtxoConfig {
  public WhirlpoolUtxoConfig() {}

  protected abstract UtxoConfigPersisted getUtxoConfigPersisted();

  protected abstract UtxoConfigSupplier getUtxoConfigSupplier();

  public String getPoolId() {
    return getUtxoConfigPersisted().getPoolId();
  }

  private void onChange() {
    getUtxoConfigSupplier().setLastChange();
  }

  public void setPoolId(String poolId) {
    getUtxoConfigPersisted().setPoolId(poolId);
    onChange();
  }

  public int getMixsDone() {
    return getUtxoConfigPersisted().getMixsDone();
  }

  public void setMixsDone(int mixsDone) {
    getUtxoConfigPersisted().setMixsDone(mixsDone);
    onChange();
  }

  public void incrementMixsDone() {
    getUtxoConfigPersisted().incrementMixsDone();
    onChange();
  }

  @Override
  public String toString() {
    return getUtxoConfigPersisted().toString();
  }
}
