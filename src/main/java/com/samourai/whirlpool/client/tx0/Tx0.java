package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bipWallet.KeyBag;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoOutPoint;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0 extends Tx0Preview {
  private static final Logger log = LoggerFactory.getLogger(Tx0.class);
  private static final int TX_SIZE_PRECISION = 30; // allow 30 sats approx
  private Collection<UtxoOutPoint> spendFroms;
  private Collection<BipUtxo> ownSpendFroms;
  private Tx0Config tx0Config;
  private Tx0Context tx0Context;
  private Transaction tx;
  private List<TransactionOutput>
      ownPremixOutputs; // own premix outputs (not including counterparty)
  private List<TransactionOutput>
      changeOutputs; // all change outputs (including counterparty if any)
  private TransactionOutput opReturnOutput;
  private TransactionOutput samouraiFeeOutput;
  private Tx0x2CahootsResult tx0x2CahootsResult; // set for tx0x2 cahoots (2-party)
  private Tx0x2DecoyResult tx0x2DecoyResult; // set for tx0x2 decoy
  private KeyBag keyBag;
  private boolean signed;
  private Collection<BipUtxo> cascadingChangeUtxos; // change utxos for next lower pool TX0

  public Tx0(
      Tx0Preview tx0Preview,
      Collection<UtxoOutPoint> spendFroms,
      Collection<BipUtxo> ownSpendFroms,
      Tx0Config tx0Config,
      Tx0Context tx0Context,
      Transaction tx,
      List<TransactionOutput> ownPremixOutputs,
      List<TransactionOutput> changeOutputs,
      TransactionOutput opReturnOutput,
      TransactionOutput samouraiFeeOutput,
      KeyBag keyBag,
      boolean signed)
      throws Exception {
    super(tx0Preview);
    this.spendFroms = spendFroms;
    this.ownSpendFroms = ownSpendFroms;
    this.tx0Config = tx0Config;
    this.tx0Context = tx0Context;
    this.tx = tx;
    this.ownPremixOutputs = ownPremixOutputs;
    this.changeOutputs = changeOutputs;
    this.opReturnOutput = opReturnOutput;
    this.samouraiFeeOutput = samouraiFeeOutput;
    this.tx0x2CahootsResult = null;
    this.tx0x2DecoyResult = null;
    this.keyBag = keyBag;
    this.signed = signed;
    this.cascadingChangeUtxos = null;
    try {
      this.consistencyCheck();
    } catch (Exception e) {
      log.error("consistency check failed for tx0={" + this + "}\n" + tx.toString(), e);
      throw e;
    }
  }

  private void consistencyCheck() throws Exception {
    // consistency check
    long spendFromsSum = UtxoOutPoint.sumValue(spendFroms);
    if (getSpendFromValue() != spendFromsSum) {
      throw new Exception(
          "Invalid Tx0Preview.spendFromValue="
              + getSpendFromValue()
              + " vs Tx0.spendFromsSum="
              + spendFromsSum);
    }
    if (signed) {
      // only check tx size when signed
      if (Math.abs(tx.getVirtualTransactionSize() - getTx0Size()) > TX_SIZE_PRECISION) {
        throw new Exception(
            "Invalid Tx0Preview.tx0Size="
                + getTx0Size()
                + " vs Tx0.vSize="
                + tx.getVirtualTransactionSize());
      }
      long minerFeePrecision = TX_SIZE_PRECISION * getTx0MinerFeePrice();
      if (Math.abs(tx.getFee().getValue() - getTx0MinerFee()) > minerFeePrecision) {
        throw new Exception(
            "Invalid Tx0Preview.tx0MinerFee="
                + getTx0MinerFee()
                + " vs Tx0.fee="
                + tx.getFee().getValue());
      }
    }
  }

  public void _finalizeTx0x2Result(Transaction tx) {
    this.tx = tx;
    this.signed = true;
  }

  public Collection<UtxoOutPoint> getSpendFroms() {
    return spendFroms;
  }

  public Collection<BipUtxo> getOwnSpendFroms() {
    return ownSpendFroms;
  }

  public Tx0Config getTx0Config() {
    return tx0Config;
  }

  public Tx0Context getTx0Context() {
    return tx0Context;
  }

  public Transaction getTx() {
    return tx;
  }

  public List<TransactionOutput> getOwnPremixOutputs() {
    return ownPremixOutputs;
  }

  public List<TransactionOutput> getChangeOutputs() {
    return changeOutputs;
  }

  public TransactionOutput getOpReturnOutput() {
    return opReturnOutput;
  }

  public TransactionOutput getSamouraiFeeOutput() {
    return samouraiFeeOutput;
  }

  public Tx0x2CahootsResult getTx0x2CahootsResult() {
    return tx0x2CahootsResult;
  }

  protected void _setTx0x2CahootsResult(Tx0x2CahootsResult tx0x2CahootsResult) {
    this.tx0x2CahootsResult = tx0x2CahootsResult;
  }

  public Tx0x2DecoyResult getTx0x2DecoyResult() {
    return tx0x2DecoyResult;
  }

  protected void _setTx0x2DecoyResult(Tx0x2DecoyResult tx0x2DecoyResult) {
    this.tx0x2DecoyResult = tx0x2DecoyResult;
  }

  public Collection<BipUtxo> getCascadingChangeUtxos() {
    return cascadingChangeUtxos;
  }

  protected void _setCascadingChangeUtxos(Collection<BipUtxo> cascadingChangeUtxos) {
    this.cascadingChangeUtxos = cascadingChangeUtxos;
  }

  public KeyBag getKeyBag() {
    return keyBag;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", spendFroms="
        + spendFroms.stream()
            .map(u -> UtxoUtil.getInstance().utxoToKey(u))
            .collect(Collectors.toList())
        + ", \nownSpendFroms="
        + ownSpendFroms.stream()
            .map(u -> UtxoUtil.getInstance().utxoToKey(u))
            .collect(Collectors.toList())
        + ", \nkeyBag="
        + keyBag
        + ",\ntx0Config="
        + tx0Config
        + ",\ntx0x2CahootsResult="
        + (tx0x2CahootsResult != null ? tx0x2CahootsResult : "null")
        + ",\ntx0x2DecoyResult="
        + (tx0x2DecoyResult != null ? tx0x2DecoyResult : "null")
        + ",\nsigned="
        + signed;
  }
}
