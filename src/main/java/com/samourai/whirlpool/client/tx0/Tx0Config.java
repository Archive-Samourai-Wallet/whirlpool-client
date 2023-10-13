package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public class Tx0Config extends Tx0PreviewConfig {
  private Collection<BipUtxo> ownSpendFromUtxos; // own inputs, not NULL
  private UtxoKeyProvider utxoKeyProvider;
  private BipWallet premixWallet;
  private BipWallet changeWallet;
  private BipWallet feeChangeWallet;
  private Pool pool;
  private Tx0x2CahootsConfig tx0x2CahootsConfig; // set for tx0x2Cahoots (2-party)

  public Tx0Config(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<BipUtxo> ownSpendFromUtxos,
      UtxoKeyProvider utxoKeyProvider,
      BipWallet premixWallet,
      BipWallet changeWallet,
      BipWallet feeChangeWallet,
      Pool pool) {
    super(tx0FeeTarget, mixFeeTarget, ownSpendFromUtxos);
    this.ownSpendFromUtxos = ownSpendFromUtxos;
    this.utxoKeyProvider = utxoKeyProvider;
    this.premixWallet = premixWallet;
    this.changeWallet = changeWallet;
    this.feeChangeWallet = feeChangeWallet;
    this.pool = pool;
    this.tx0x2CahootsConfig = null;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config) {
    super(tx0Config);
    this.ownSpendFromUtxos = tx0Config.ownSpendFromUtxos;
    this.utxoKeyProvider = tx0Config.utxoKeyProvider;
    this.premixWallet = tx0Config.premixWallet;
    this.changeWallet = tx0Config.changeWallet;
    this.feeChangeWallet = tx0Config.feeChangeWallet;
    this.pool = tx0Config.pool;
    this.tx0x2CahootsConfig = tx0Config.tx0x2CahootsConfig;
    consistencyCheck();
  }

  public Tx0Config(Tx0Config tx0Config, Collection<BipUtxo> ownSpendFromUtxos, Pool pool) {
    super(tx0Config, ownSpendFromUtxos);
    this.ownSpendFromUtxos = ownSpendFromUtxos;
    this.utxoKeyProvider = tx0Config.utxoKeyProvider;
    this.premixWallet = tx0Config.premixWallet;
    this.changeWallet = tx0Config.changeWallet;
    this.feeChangeWallet = tx0Config.feeChangeWallet;
    this.pool = pool;
    this.tx0x2CahootsConfig =
        tx0Config.tx0x2CahootsConfig != null
            ? new Tx0x2CahootsConfig(tx0Config.tx0x2CahootsConfig)
            : null;
    consistencyCheck();
  }

  protected void consistencyCheck() {
    if (ownSpendFromUtxos == null) {
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

  public Collection<BipUtxo> getOwnSpendFromUtxos() {
    return ownSpendFromUtxos;
  }

  public Pool getPool() {
    return pool;
  }

  public Tx0x2CahootsConfig getTx0x2CahootsConfig() {
    return tx0x2CahootsConfig;
  }

  public void setTx0x2CahootsConfig(Tx0x2CahootsConfig tx0x2CahootsConfig) {
    this.tx0x2CahootsConfig = tx0x2CahootsConfig;
    setTx0x2SpendFromsCounterparty(tx0x2CahootsConfig.getCounterpartyInputs());
  }

  @Override
  public String toString() {
    return super.toString()
        + ", tx0x2CahootsConfig="
        + (tx0x2CahootsConfig != null ? "{" + tx0x2CahootsConfig + "}" : "null")
        + ", poolId="
        + pool.getPoolId();
  }
}
