package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import java.util.Collection;

public class Tx0PreviewConfig {
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private boolean tx0x2Decoy;
  private boolean tx0x2DecoyForced; // true=force tx0x2Decoy or fail, false=fallback to regular tx0
  private boolean _cascading; // internally set when cascading
  // own inputs for initial pool, may be NULL for general pools preview
  private Collection<? extends UtxoDetail> ownSpendFroms;
  // counterparty inputs for initial pool with tx0x2Cahoots (2-party)
  private Collection<? extends UtxoDetail> tx0x2SpendFromsCounterparty;
  private boolean cascade;

  public Tx0PreviewConfig(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<? extends UtxoDetail> ownSpendFroms) {
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.tx0x2Decoy = true;
    this.tx0x2DecoyForced = false;
    this._cascading = false;
    this.ownSpendFroms = ownSpendFroms;
    this.tx0x2SpendFromsCounterparty = null;
    this.cascade = false;
  }

  public Tx0PreviewConfig(Tx0PreviewConfig tx0PreviewConfig) {
    this.tx0FeeTarget = tx0PreviewConfig.tx0FeeTarget;
    this.mixFeeTarget = tx0PreviewConfig.mixFeeTarget;
    this.tx0x2Decoy = tx0PreviewConfig.tx0x2Decoy;
    this.tx0x2DecoyForced = tx0PreviewConfig.tx0x2DecoyForced;
    this._cascading = tx0PreviewConfig._cascading;
    this.ownSpendFroms = tx0PreviewConfig.ownSpendFroms;
    this.tx0x2SpendFromsCounterparty = tx0PreviewConfig.tx0x2SpendFromsCounterparty;
    this.cascade = tx0PreviewConfig.cascade;
  }

  public Tx0PreviewConfig(
      Tx0PreviewConfig tx0PreviewConfig, Collection<? extends UtxoDetail> ownSpendFroms) {
    this(tx0PreviewConfig);
    this.ownSpendFroms = ownSpendFroms;
  }

  public Tx0FeeTarget getTx0FeeTarget() {
    return tx0FeeTarget;
  }

  public void setTx0FeeTarget(Tx0FeeTarget tx0FeeTarget) {
    this.tx0FeeTarget = tx0FeeTarget;
  }

  public Tx0FeeTarget getMixFeeTarget() {
    return mixFeeTarget;
  }

  public void setMixFeeTarget(Tx0FeeTarget mixFeeTarget) {
    this.mixFeeTarget = mixFeeTarget;
  }

  public boolean isTx0x2Decoy() {
    return tx0x2Decoy;
  }

  public boolean isTx0x2Cahoots() {
    return tx0x2SpendFromsCounterparty != null;
  }

  public boolean isTx0x2Any() {
    return isTx0x2Cahoots() || tx0x2Decoy;
  }

  public void setTx0x2Decoy(boolean tx0x2Decoy) {
    this.tx0x2Decoy = tx0x2Decoy;
  }

  public boolean isTx0x2DecoyForced() {
    return tx0x2DecoyForced;
  }

  public void setTx0x2DecoyForced(boolean tx0x2DecoyForced) {
    this.tx0x2DecoyForced = tx0x2DecoyForced;
  }

  public boolean _isCascading() {
    return _cascading;
  }

  public void _setCascading(boolean _cascading) {
    this._cascading = _cascading;
  }

  public Collection<? extends UtxoDetail> getOwnSpendFroms() {
    return ownSpendFroms;
  }

  public Collection<? extends UtxoDetail> getTx0x2SpendFromsCounterparty() {
    return tx0x2SpendFromsCounterparty;
  }

  public void setTx0x2SpendFromsCounterparty(
      Collection<? extends UtxoDetail> tx0x2SpendFromsCounterparty) {
    this.tx0x2SpendFromsCounterparty = tx0x2SpendFromsCounterparty;
  }

  public boolean isCascade() {
    return cascade;
  }

  public void setCascade(boolean cascade) {
    this.cascade = cascade;
  }

  @Override
  public String toString() {
    return "tx0FeeTarget="
        + tx0FeeTarget
        + ", mixFeeTarget="
        + mixFeeTarget
        + ", cascade="
        + cascade
        + ", decoyTx0x2="
        + tx0x2Decoy
        + ", spendFroms="
        + (ownSpendFroms != null ? ownSpendFroms.size() + " utxos" : "null")
        + ", cascade="
        + cascade;
  }
}
