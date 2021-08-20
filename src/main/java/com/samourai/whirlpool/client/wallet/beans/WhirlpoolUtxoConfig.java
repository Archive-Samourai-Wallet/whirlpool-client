package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

public abstract class WhirlpoolUtxoConfig {
  public WhirlpoolUtxoConfig() {}

  protected abstract UtxoConfigPersisted getUtxoConfigPersisted();

  protected abstract UtxoConfigSupplier getUtxoConfigSupplier();

  abstract UnspentOutput getUtxo();

  private void saveUtxoConfig() {
    getUtxoConfigSupplier()
        .saveUtxo(getUtxo().tx_hash, getUtxo().tx_output_n, getUtxoConfigPersisted());
  }

  public int getMixsDone() {
    return getUtxoConfigPersisted().getMixsDone();
  }

  public void setMixsDone(int mixsDone) {
    getUtxoConfigPersisted().setMixsDone(mixsDone);
    saveUtxoConfig();
  }

  @Override
  public String toString() {
    return getUtxoConfigPersisted().toString();
  }
}
