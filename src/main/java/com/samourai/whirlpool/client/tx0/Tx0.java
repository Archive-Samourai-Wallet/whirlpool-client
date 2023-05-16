package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import java.util.Collection;
import java.util.List;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0 extends Tx0Preview {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);

  private Collection<UnspentOutput> spendFroms;
  private Tx0Config tx0Config;
  private Tx0Context tx0Context;
  private Transaction tx;
  private List<TransactionOutput> premixOutputs;
  private List<TransactionOutput> changeOutputs;
  private List<UnspentOutput> changeUtxos;
  private TransactionOutput opReturnOutput;
  private TransactionOutput samouraiFeeOutput;

  public Tx0(
      Tx0Preview tx0Preview,
      Collection<UnspentOutput> spendFroms,
      Tx0Config tx0Config,
      Tx0Context tx0Context,
      Transaction tx,
      List<TransactionOutput> premixOutputs,
      List<TransactionOutput> changeOutputs,
      List<UnspentOutput> changeUtxos,
      TransactionOutput opReturnOutput,
      TransactionOutput samouraiFeeOutput) {
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
  }

  public Collection<UnspentOutput> getSpendFroms() {
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

  public List<UnspentOutput> getChangeUtxos() {
    return changeUtxos;
  }

  public TransactionOutput getOpReturnOutput() {
    return opReturnOutput;
  }

  public TransactionOutput getSamouraiFeeOutput() {
    return samouraiFeeOutput;
  }
}
