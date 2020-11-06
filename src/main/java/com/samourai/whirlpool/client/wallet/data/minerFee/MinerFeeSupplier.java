package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.BasicSupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinerFeeSupplier extends BasicSupplier<MinerFee> {
  private static final Logger log = LoggerFactory.getLogger(MinerFeeSupplier.class);

  protected int feeMin;
  protected int feeMax;

  public MinerFeeSupplier(int feeMin, int feeMax, int feeFallback) {
    super(log, mockMinerFee(feeFallback));
    this.feeMin = feeMin;
    this.feeMax = feeMax;
  }

  protected void _setValue(WalletResponse walletResponse) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }

    // check each fee value
    if (walletResponse == null || walletResponse.info == null || walletResponse.info.fees == null) {
      throw new Exception("Invalid walletResponse.info.fees");
    }
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      if (walletResponse.info.fees.get(minerFeeTarget.getValue()) == null) {
        throw new Exception("Invalid walletResponse.info.fees[" + minerFeeTarget.getValue() + "]");
      }
    }

    MinerFee minerFee = new MinerFee(walletResponse.info.fees);
    super.setValue(minerFee);
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
