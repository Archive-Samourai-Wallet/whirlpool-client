package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;
import java.util.Collection;
import java.util.List;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

public class Tx0 extends Tx0Preview {
  private static final int TX_SIZE_PRECISION = 30; // allow 30 sats approx
  private Collection<BipUtxo> spendFroms;
  private Tx0Config tx0Config;
  private Tx0Context tx0Context;
  private Transaction tx;
  private List<TransactionOutput> premixOutputs;
  private List<TransactionOutput> changeOutputs;
  private List<? extends BipUtxo> changeUtxos;
  private TransactionOutput opReturnOutput;
  private TransactionOutput samouraiFeeOutput;

  public Tx0(
      Tx0Preview tx0Preview,
      Collection<BipUtxo> spendFroms,
      Tx0Config tx0Config,
      Tx0Context tx0Context,
      Transaction tx,
      List<TransactionOutput> premixOutputs,
      List<TransactionOutput> changeOutputs,
      List<? extends BipUtxo> changeUtxos,
      TransactionOutput opReturnOutput,
      TransactionOutput samouraiFeeOutput)
      throws Exception {
    super(tx0Preview);
    this.spendFroms = spendFroms;
    this.tx0Config = tx0Config;
    this.tx0Context = tx0Context;
    this.tx = tx;
    this.premixOutputs = premixOutputs;
    this.changeOutputs = changeOutputs;
    this.changeUtxos = changeUtxos;
    this.opReturnOutput = opReturnOutput;
    this.samouraiFeeOutput = samouraiFeeOutput;

    // consistency check
    long spendFromsSum = BipUtxo.sumValue(spendFroms);
    if (tx0Preview.getSpendFromValue() != spendFromsSum) {
      throw new Exception(
          "Invalid Tx0Preview.spendFromValue="
              + tx0Preview.getSpendFromValue()
              + " vs Tx0.spendFromsSum="
              + spendFromsSum);
    }
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

  public Collection<? extends BipUtxo> getSpendFroms() {
    return spendFroms;
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

  public List<TransactionOutput> getPremixOutputs() {
    return premixOutputs;
  }

  public List<TransactionOutput> getChangeOutputs() {
    return changeOutputs;
  }

  public List<? extends BipUtxo> getChangeUtxos() {
    return changeUtxos;
  }

  public TransactionOutput getOpReturnOutput() {
    return opReturnOutput;
  }

  public TransactionOutput getSamouraiFeeOutput() {
    return samouraiFeeOutput;
  }

  @Override
  public String toString() {
    return super.toString() + ", spendFroms=" + spendFroms + ", tx0Config=" + tx0Config;
  }
}
