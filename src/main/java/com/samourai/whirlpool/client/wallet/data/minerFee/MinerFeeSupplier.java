package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MinerFeeSupplier extends AbstractSupplier<MinerFee> {
  private static final Logger log = LoggerFactory.getLogger(MinerFeeSupplier.class);

  protected int feeMin;
  protected int feeMax;

  public MinerFeeSupplier(int feeMin, int feeMax, int feeFallback) {
    super(null, mockMinerFee(feeFallback), log);
    this.feeMin = feeMin;
    this.feeMax = feeMax;
  }

  protected void _setValue(WalletResponse walletResponse) {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }
    MinerFee minerFee = new MinerFee(walletResponse.info.fees);
    super._setValue(minerFee);
  }

  protected static MinerFee mockMinerFee(int feeValue) {
    Map<String, Integer> feeResponse = new LinkedHashMap<String, Integer>();
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      feeResponse.put(minerFeeTarget.getValue(), feeValue);
    }
    return new MinerFee(feeResponse);
  }

  public int getFee(MinerFeeTarget feeTarget) {
    // get fee or fallback
    int fee = getValue().get(feeTarget);

    // check min
    if (fee < feeMin) {
      log.error("Fee/b too low (" + feeTarget + "): " + fee + " => " + feeMin);
      fee = feeMin;
    }

    // check max
    if (fee > feeMax) {
      log.error("Fee/b too high (" + feeTarget + "): " + fee + " => " + feeMax);
      fee = feeMax;
    }
    return fee;
  }

  public int getFee(Tx0FeeTarget feeTarget) {
    return getFee(feeTarget.getFeeTarget());
  }
}
