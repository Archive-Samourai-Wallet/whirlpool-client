package com.samourai.whirlpool.client.tx0;

import java.util.Collection;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

public class Tx0 extends Tx0Preview {
  private Transaction tx;
  private Collection<TransactionOutput> premixOutputs;
  private Collection<TransactionOutput> changeOutputs;

  public Tx0(
      Tx0Preview tx0Preview,
      Transaction tx,
      Collection<TransactionOutput> premixOutputs,
      Collection<TransactionOutput> changeOutputs) {
    super(tx0Preview);
    this.tx = tx;
    this.premixOutputs = premixOutputs;
    this.changeOutputs = changeOutputs;
  }

  public Transaction getTx() {
    return tx;
  }

  public Collection<TransactionOutput> getPremixOutputs() {
    return premixOutputs;
  }

  public Collection<TransactionOutput> getChangeOutputs() {
    return changeOutputs;
  }
}
