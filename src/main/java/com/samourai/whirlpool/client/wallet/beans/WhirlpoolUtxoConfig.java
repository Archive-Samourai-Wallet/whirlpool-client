package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

public abstract class WhirlpoolUtxoConfig {
  public WhirlpoolUtxoConfig() {}

  protected abstract UtxoConfigPersisted getUtxoConfigPersisted();

  protected abstract UtxoConfigSupplier getUtxoConfigSupplier();

  private void onChange() {
    getUtxoConfigSupplier().saveUtxoConfig(getUtxoConfigPersisted());
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
