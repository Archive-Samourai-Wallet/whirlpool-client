package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import java.util.Collection;

public class Tx0PreviewConfig {
  private Tx0FeeTarget tx0FeeTarget;
  private Tx0FeeTarget mixFeeTarget;
  private boolean decoyTx0x2;
  private boolean decoyTx0x2Forced; // true=force tx0x2Decoy or fail, false=fallback to regular tx0
  private boolean _cascading; // internally set when cascading
  private Collection<? extends UtxoDetail> spendFroms; // may be NULL for general pools preview
  private boolean cascade;

  public Tx0PreviewConfig(
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Collection<? extends UtxoDetail> spendFroms) {
    this.tx0FeeTarget = tx0FeeTarget;
    this.mixFeeTarget = mixFeeTarget;
    this.decoyTx0x2 = true;
    this.decoyTx0x2Forced = false;
    this._cascading = false;
    this.spendFroms = spendFroms;
    this.cascade = false;
  }

  public Tx0PreviewConfig(Tx0PreviewConfig tx0PreviewConfig) {
    this.tx0FeeTarget = tx0PreviewConfig.tx0FeeTarget;
    this.mixFeeTarget = tx0PreviewConfig.mixFeeTarget;
    this.decoyTx0x2 = tx0PreviewConfig.decoyTx0x2;
    this.decoyTx0x2Forced = tx0PreviewConfig.decoyTx0x2Forced;
    this._cascading = tx0PreviewConfig._cascading;
    this.spendFroms = tx0PreviewConfig.spendFroms;
    this.cascade = tx0PreviewConfig.cascade;
  }

  public Tx0PreviewConfig(
      Tx0PreviewConfig tx0PreviewConfig, Collection<? extends UtxoDetail> spendFroms) {
    this(tx0PreviewConfig);
    this.spendFroms = spendFroms;
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

  public boolean isDecoyTx0x2() {
    return decoyTx0x2;
  }

  public void setDecoyTx0x2(boolean decoyTx0x2) {
    this.decoyTx0x2 = decoyTx0x2;
  }

  public boolean isDecoyTx0x2Forced() {
    return decoyTx0x2Forced;
  }

  public void setDecoyTx0x2Forced(boolean decoyTx0x2Forced) {
    this.decoyTx0x2Forced = decoyTx0x2Forced;
  }

  public boolean _isCascading() {
    return _cascading;
  }

  public void _setCascading(boolean _cascading) {
    this._cascading = _cascading;
  }

  public Collection<? extends UtxoDetail> getSpendFroms() {
    return spendFroms;
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
        + ", _cascading="
        + _cascading
        + ", decoyTx0x2="
        + decoyTx0x2
        + ", spendFroms="
        + (spendFroms != null ? spendFroms.size() + " utxos" : "null")
        + ", cascade="
        + cascade;
  }
}
