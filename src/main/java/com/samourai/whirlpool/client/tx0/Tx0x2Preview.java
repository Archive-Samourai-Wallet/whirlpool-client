package com.samourai.whirlpool.client.tx0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Preview {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Preview.class);

  private boolean tx0x2Cahoots; // tx0x2 (2-party)
  private boolean tx0x2Decoy; // tx0x2 (decoy)
  private int nbPremixSender;
  private int nbPremixCounterparty;
  private long changeAmountSender;
  private long changeAmountCounterparty;
  private long tx0MinerFeeSender;
  private long tx0MinerFeeCounterparty;
  private long samouraiFeeSender;
  private long samouraiFeeCounterparty;
  private boolean splitChange;

  public Tx0x2Preview(Tx0x2Preview tx0Preview) {
    this(
        tx0Preview.tx0x2Cahoots,
        tx0Preview.tx0x2Decoy,
        tx0Preview.nbPremixSender,
        tx0Preview.nbPremixCounterparty,
        tx0Preview.changeAmountSender,
        tx0Preview.changeAmountCounterparty,
        tx0Preview.tx0MinerFeeSender,
        tx0Preview.tx0MinerFeeCounterparty,
        tx0Preview.samouraiFeeSender,
        tx0Preview.samouraiFeeCounterparty,
        tx0Preview.splitChange);
  }

  public Tx0x2Preview(
      boolean tx0x2Cahoots,
      boolean tx0x2Decoy,
      int nbPremixSender,
      int nbPremixCounterparty,
      long changeAmountSender,
      long changeAmountCounterparty,
      long tx0MinerFeeSender,
      long tx0MinerFeeCounterparty,
      long samouraiFeeSender,
      long samouraiFeeCounterparty,
      boolean splitChange) {
    this.tx0x2Cahoots = tx0x2Cahoots;
    this.tx0x2Decoy = tx0x2Decoy;
    this.nbPremixSender = nbPremixSender;
    this.nbPremixCounterparty = nbPremixCounterparty;
    this.changeAmountSender = changeAmountSender;
    this.changeAmountCounterparty = changeAmountCounterparty;
    this.tx0MinerFeeSender = tx0MinerFeeSender;
    this.tx0MinerFeeCounterparty = tx0MinerFeeCounterparty;
    this.samouraiFeeSender = samouraiFeeSender;
    this.samouraiFeeCounterparty = samouraiFeeCounterparty;
    this.splitChange = splitChange;
  }

  public boolean isTx0x2Cahoots() {
    return tx0x2Cahoots;
  }

  public boolean isTx0x2Decoy() {
    return tx0x2Decoy;
  }

  public int getNbPremixSender() {
    return nbPremixSender;
  }

  public int getNbPremixCounterparty() {
    return nbPremixCounterparty;
  }

  public long getChangeAmountSender() {
    return changeAmountSender;
  }

  public long getChangeAmountCounterparty() {
    return changeAmountCounterparty;
  }

  public long getTx0MinerFeeSender() {
    return tx0MinerFeeSender;
  }

  public long getTx0MinerFeeCounterparty() {
    return tx0MinerFeeCounterparty;
  }

  public long getSamouraiFeeSender() {
    return samouraiFeeSender;
  }

  public long getSamouraiFeeCounterparty() {
    return samouraiFeeCounterparty;
  }

  public boolean isSplitChange() {
    return splitChange;
  }

  @Override
  public String toString() {
    return "tx0x2Cahoots="
        + tx0x2Cahoots
        + ", decoyTx0x2="
        + tx0x2Decoy
        + ", nbPremixSender="
        + nbPremixSender
        + ", nbPremixCounterparty="
        + nbPremixCounterparty
        + ", changeAmountSender="
        + changeAmountSender
        + ", changeAmountCounterparty="
        + changeAmountCounterparty
        + ", tx0MinerFeeSender="
        + tx0MinerFeeSender
        + ", tx0MinerFeeCounterparty="
        + tx0MinerFeeCounterparty
        + ", samouraiFeeSender="
        + samouraiFeeSender
        + ", samouraiFeeCounterparty="
        + samouraiFeeCounterparty
        + ", splitChange="
        + splitChange;
  }
}
