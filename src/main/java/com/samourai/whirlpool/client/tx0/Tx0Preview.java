package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Preview {
  private static final Logger log = LoggerFactory.getLogger(Tx0Preview.class);

  private Pool pool;
  private Tx0Data tx0Data; // may be null
  private long spendFromValue;
  private int tx0Size;
  private long tx0MinerFee;
  private long mixMinerFee;
  private long premixMinerFee;
  private int tx0MinerFeePrice;
  private int mixMinerFeePrice;
  private long feeValue;
  private long feeChange;
  private int feeDiscountPercent;
  private long premixValue;
  private long changeValue;
  private int nbPremix; // nbPremix total
  private long spendValue; // all except change
  private long totalValue; // with change
  private Tx0x2Preview tx0x2Preview; // only set when tx0x2 (2-party or decoy)

  public Tx0Preview(Tx0Preview tx0Preview) throws Exception {
    this(
        tx0Preview.pool,
        tx0Preview.tx0Data,
        tx0Preview.spendFromValue,
        tx0Preview.tx0Size,
        tx0Preview.tx0MinerFee,
        tx0Preview.mixMinerFee,
        tx0Preview.premixMinerFee,
        tx0Preview.tx0MinerFeePrice,
        tx0Preview.mixMinerFeePrice,
        tx0Preview.premixValue,
        tx0Preview.changeValue,
        tx0Preview.nbPremix,
        tx0Preview.tx0x2Preview);
  }

  public Tx0Preview(
      Pool pool,
      Tx0Data tx0Data,
      long spendFromValue,
      int tx0Size,
      long tx0MinerFee,
      long mixMinerFee,
      long premixMinerFee,
      int tx0MinerFeePrice,
      int mixMinerFeePrice,
      long premixValue,
      long changeValue,
      int nbPremix,
      Tx0x2Preview tx0x2Preview)
      throws Exception {
    this.pool = pool;
    this.tx0Data = tx0Data;
    this.spendFromValue = spendFromValue;
    this.tx0Size = tx0Size;
    this.tx0MinerFee = tx0MinerFee;
    this.mixMinerFee = mixMinerFee;
    this.premixMinerFee = premixMinerFee;
    this.tx0MinerFeePrice = tx0MinerFeePrice;
    this.mixMinerFeePrice = mixMinerFeePrice;
    this.feeValue = tx0Data != null ? tx0Data.getFeeValue() : pool.getFeeValue();
    this.feeChange = tx0Data != null ? tx0Data.getFeeChange() : 0;
    this.feeDiscountPercent = tx0Data != null ? tx0Data.getFeeDiscountPercent() : 0;
    this.premixValue = premixValue;
    this.changeValue = changeValue;
    this.nbPremix = nbPremix;
    long feeValueOrFeeChange =
        tx0Data != null ? tx0Data.computeFeeValueOrFeeChange() : pool.getFeeValue();
    this.spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    this.totalValue = spendValue + changeValue;
    this.tx0x2Preview = tx0x2Preview;
    try {
      this.consistencyCheck();
    } catch (Exception e) {
      log.error("consistency check failed for tx0preview={" + this + "}", e);
      throw e;
    }
  }

  private void consistencyCheck() throws Exception {
    if (spendFromValue <= 0) {
      throw new Exception("Invalid spendFromValue: " + spendFromValue);
    }
    if (changeValue < 0) {
      throw new Exception("Negative change detected, please report this bug.");
    }
    if (totalValue != spendFromValue) {
      throw new Exception(
          "Invalid totalValue=" + totalValue + " vs spendFromValue=" + spendFromValue);
    }
    if (tx0x2Preview != null) {
      if ((tx0x2Preview.getNbPremixSender() + tx0x2Preview.getNbPremixCounterparty()) != nbPremix) {
        throw new Exception(
            "Invalid nbPremixList="
                + tx0x2Preview.getNbPremixSender()
                + ";"
                + tx0x2Preview.getNbPremixCounterparty()
                + " vs nbPremix="
                + nbPremix);
      }
      if ((tx0x2Preview.getChangeAmountSender() + tx0x2Preview.getChangeAmountCounterparty())
          != changeValue) {
        throw new Exception(
            "Invalid changeAmountList="
                + tx0x2Preview.getChangeAmountSender()
                + ";"
                + tx0x2Preview.getChangeAmountCounterparty()
                + " vs changeValue="
                + changeValue);
      }
      if (tx0x2Preview.getTx0MinerFeeSender() + tx0x2Preview.getTx0MinerFeeCounterparty()
          != getTx0MinerFee()) {
        throw new Exception(
            "Invalid tx0MinerFeeSender="
                + tx0x2Preview.getTx0MinerFeeSender()
                + "+tx0MinerFeeCounterparty="
                + tx0x2Preview.getTx0MinerFeeCounterparty()
                + " vs tx0MinerFee="
                + tx0MinerFee);
      }
      if (!tx0x2Preview.isTx0x2Cahoots() && !tx0x2Preview.isTx0x2Decoy()) {
        throw new Exception("Invalid isTx0x2Any");
      }
    }
  }

  public Pool getPool() {
    return pool;
  }

  public void setPool(Pool pool) {
    this.pool = pool;
  }

  // used by Sparrow
  public Tx0Data getTx0Data() {
    return tx0Data;
  }

  public long getSpendFromValue() {
    return spendFromValue;
  }

  public int getTx0Size() {
    return tx0Size;
  }

  public long getTx0MinerFee() {
    return tx0MinerFee;
  }

  public long getMixMinerFee() {
    return mixMinerFee;
  }

  public long getPremixMinerFee() {
    return premixMinerFee;
  }

  public int getTx0MinerFeePrice() {
    return tx0MinerFeePrice;
  }

  public int getMixMinerFeePrice() {
    return mixMinerFeePrice;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public long getFeeChange() {
    return feeChange;
  }

  public int getFeeDiscountPercent() {
    return feeDiscountPercent;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public long getChangeValue() {
    return changeValue;
  }

  public int getNbPremix() {
    return nbPremix;
  }

  public long getSpendValue() {
    return spendValue;
  }

  public long getTotalValue() {
    return totalValue;
  }

  public boolean isTx0x2Any() {
    return tx0x2Preview != null;
  }

  public boolean isTx0x2Decoy() {
    return tx0x2Preview != null && tx0x2Preview.isTx0x2Decoy();
  }

  public boolean isTx0x2Cahoots() {
    return tx0x2Preview != null && tx0x2Preview.isTx0x2Cahoots();
  }

  public Tx0x2Preview getTx0x2Preview() {
    return tx0x2Preview;
  }

  // all change outputs (including eventual counterparty)
  public Collection<Long> getChangeAmountsAll() {
    List<Long> changeAmountsAll = new LinkedList<>();
    if (tx0x2Preview != null) {
      // tx0x2 (cahoots or decoy)
      if (tx0x2Preview.getChangeAmountSender() > 0) {
        changeAmountsAll.add(tx0x2Preview.getChangeAmountSender());
      }
      if (tx0x2Preview.getChangeAmountCounterparty() > 0) {
        changeAmountsAll.add(tx0x2Preview.getChangeAmountCounterparty());
      }
    } else {
      // regular tx0
      if (changeValue > 0) {
        changeAmountsAll.add(changeValue);
      }
    }
    return changeAmountsAll;
  }

  @Override
  public String toString() {
    return "poolId="
        + pool.getPoolId()
        + ", tx0MinerFee="
        + tx0MinerFee
        + ", mixMinerFee="
        + mixMinerFee
        + ", premixMinerFee="
        + premixMinerFee
        + ", feeValue="
        + feeValue
        + ", feeChange="
        + feeChange
        + ", feeDiscountPercent="
        + feeDiscountPercent
        + ", premixValue="
        + premixValue
        + ", nbPremix="
        + nbPremix
        + ", spendValue="
        + spendValue
        + ", totalValue="
        + totalValue
        + ", changeValue="
        + changeValue
        + ", \ntx0x2Preview={"
        + tx0x2Preview
        + "}";
  }
}
