package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.*;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0ParamService {
  private Logger log = LoggerFactory.getLogger(Tx0ParamService.class);

  private MinerFeeSupplier minerFeeSupplier;
  private ITx0ParamServiceConfig config;

  public Tx0ParamService(MinerFeeSupplier minerFeeSupplier, ITx0ParamServiceConfig config) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.config = config;
  }

  public Tx0Param getTx0Param(Pool pool, Tx0FeeTarget tx0FeeTarget) {
    int feeTx0 = minerFeeSupplier.getFee(tx0FeeTarget);
    int feePremix = getFeePremix();
    Long overspendOrNull = config.getOverspend(pool.getPoolId());
    Tx0Param tx0Param =
        new Tx0Param(config.getNetworkParameters(), feeTx0, feePremix, pool, overspendOrNull);
    return tx0Param;
  }

  public Collection<Pool> findPools(Collection<Pool> poolsByPreference, long utxoValue) {
    List<Pool> eligiblePools = new LinkedList<Pool>();
    for (Pool pool : poolsByPreference) {
      Tx0Param tx0Param = getTx0Param(pool, Tx0FeeTarget.MIN);
      boolean eligible = tx0Param.isTx0Possible(utxoValue);
      if (eligible) {
        eligiblePools.add(pool);
      }
    }
    return eligiblePools;
  }

  private int getFeePremix() {
    return minerFeeSupplier.getFee(config.getFeeTargetPremix());
  }

  public boolean isTx0Possible(Pool pool, Tx0FeeTarget tx0FeeTarget, long utxoValue) {
    Tx0Param tx0Param = getTx0Param(pool, tx0FeeTarget);
    return tx0Param.isTx0Possible(utxoValue);
  }

  public boolean isPoolApplicable(Pool pool, WhirlpoolUtxo whirlpoolUtxo) {
    long utxoValue = whirlpoolUtxo.getUtxo().value;
    if (WhirlpoolAccount.DEPOSIT.equals(whirlpoolUtxo.getAccount())) {
      return isTx0Possible(pool, Tx0FeeTarget.MIN, utxoValue);
    }
    if (WhirlpoolAccount.PREMIX.equals(whirlpoolUtxo.getAccount())) {
      return pool.checkInputBalance(utxoValue, false);
    }
    if (WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount())) {
      return utxoValue == pool.getDenomination();
    }
    log.error("Unknown account for whirlpoolUtxo:" + whirlpoolUtxo);
    return false;
  }
}
