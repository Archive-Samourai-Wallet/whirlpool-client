package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.rest.Tx0DataRequestV2;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0PreviewService {
  private Logger log = LoggerFactory.getLogger(Tx0PreviewService.class);

  private MinerFeeSupplier minerFeeSupplier;
  private ITx0PreviewServiceConfig config;

  public Tx0PreviewService(MinerFeeSupplier minerFeeSupplier, ITx0PreviewServiceConfig config) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.config = config;
  }

  public Tx0Param getTx0Param(Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    int feeTx0 = minerFeeSupplier.getFee(tx0FeeTarget.getFeeTarget());
    int feePremix = minerFeeSupplier.getFee(mixFeeTarget.getFeeTarget());
    return getTx0Param(pool, feeTx0, feePremix);
  }

  public Tx0Param getTx0Param(Pool pool, int tx0FeeTarget, int mixFeeTarget) {
    Long overspendOrNull = config.getOverspend(pool.getPoolId());
    Tx0Param tx0Param = new Tx0Param(tx0FeeTarget, mixFeeTarget, pool, overspendOrNull);
    return tx0Param;
  }

  protected int capNbPremix(int nbPremix, Pool pool) {
    int maxOutputs = config.getTx0MaxOutputs();
    if (maxOutputs > 0) {
      nbPremix = Math.min(maxOutputs, nbPremix); // cap with maxOutputs
    }
    nbPremix = Math.min(pool.getTx0MaxOutputs(), nbPremix); // cap with pool.tx0MaxOutputs
    return nbPremix;
  }

  private int computeNbPremixMax(
      Tx0Param tx0Param, Tx0Data tx0Data, Collection<UnspentOutput> spendFrom) {
    long premixValue = tx0Param.getPremixValue();
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    int feeTx0 = tx0Param.getFeeTx0();
    Pool pool = tx0Param.getPool();

    NetworkParameters params = config.getNetworkParameters();
    long spendFromBalance = UnspentOutput.sumValue(spendFrom);

    // compute nbPremix ignoring TX0 fee
    int nbPremixInitial = (int) Math.ceil(spendFromBalance / premixValue);

    // compute nbPremix with TX0 fee
    int nbPremix = nbPremixInitial;
    while (true) {
      // estimate TX0 fee for nbPremix
      long tx0MinerFee = ClientUtils.computeTx0MinerFee(nbPremix, feeTx0, spendFrom, params);
      long spendValue =
          ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
      if (log.isTraceEnabled()) {
        log.trace(
            "computeNbPremixMax: nbPremix="
                + nbPremix
                + " => spendValue="
                + spendValue
                + ", tx0MinerFee="
                + tx0MinerFee
                + ", spendFromBalance="
                + spendFromBalance
                + ", nbPremixInitial="
                + nbPremixInitial);
      }
      if (spendFromBalance < spendValue) {
        // if UTXO balance is insufficient, try with less nbPremix
        nbPremix--;
      } else {
        // nbPremix found
        break;
      }
    }
    // no negative value
    if (nbPremix < 0) {
      nbPremix = 0;
    }
    nbPremix = capNbPremix(nbPremix, pool);
    return nbPremix;
  }

  public Tx0Previews tx0PreviewsMinimal(Tx0PreviewConfig tx0PreviewConfig) {
    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Pool pool : tx0PreviewConfig.getPools()) {
      final String poolId = pool.getPoolId();
      Tx0Param tx0Param = tx0PreviewConfig.getTx0Param(poolId);
      try {
        // minimal preview estimation (without SCODE calculation)
        Tx0Preview tx0Preview = tx0PreviewMinimal(tx0Param);
        tx0PreviewsByPoolId.put(poolId, tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0Previews(tx0PreviewsByPoolId);
  }

  public Tx0Previews tx0Previews(
      Tx0PreviewConfig tx0PreviewConfig, Collection<UnspentOutput> spendFroms) throws Exception {
    // fetch fresh Tx0Data
    Collection<Tx0Data> tx0Datas = fetchTx0Data(config.getPartner());

    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Tx0Data tx0Data : tx0Datas) {
      final String poolId = tx0Data.getPoolId();
      Tx0Param tx0Param = tx0PreviewConfig.getTx0Param(poolId);
      try {
        // real preview for outputs (with SCODE and outputs calculation)
        Tx0Preview tx0Preview = tx0Preview(tx0Param, tx0Data, spendFroms);
        tx0PreviewsByPoolId.put(poolId, tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0Previews(tx0PreviewsByPoolId);
  }

  protected Tx0Preview tx0PreviewMinimal(Tx0Param tx0Param) throws Exception {
    return doTx0Preview(tx0Param, 1, null, null);
  }

  protected Tx0Preview tx0Preview(
      Tx0Param tx0Param, Tx0Data tx0Data, Collection<UnspentOutput> spendFroms) throws Exception {
    int nbPremix = computeNbPremixMax(tx0Param, tx0Data, spendFroms);
    return doTx0Preview(tx0Param, nbPremix, spendFroms, tx0Data);
  }

  protected Tx0Preview doTx0Preview(
      Tx0Param tx0Param,
      int nbPremix,
      Collection<UnspentOutput> spendFromsOrNull,
      Tx0Data tx0DataOrNull)
      throws Exception {

    // check fee (duplicate safety check)
    int feeTx0 = tx0Param.getFeeTx0();
    if (feeTx0 < config.getFeeMin()) {
      throw new NotifiableException("Invalid fee for Tx0: " + feeTx0 + " < " + config.getFeeMin());
    }
    if (feeTx0 > config.getFeeMax()) {
      throw new NotifiableException("Invalid fee for Tx0: " + feeTx0 + " > " + config.getFeeMax());
    }

    // check premixValue (duplicate safety check)
    long premixValue = tx0Param.getPremixValue();
    if (!tx0Param.getPool().isPremix(premixValue, false)) {
      throw new NotifiableException("Invalid premixValue for Tx0: " + premixValue);
    }

    NetworkParameters params = config.getNetworkParameters();

    Pool pool = tx0Param.getPool();
    long feeValueOrFeeChange =
        tx0DataOrNull != null ? tx0DataOrNull.computeFeeValueOrFeeChange() : pool.getFeeValue();

    int tx0Size = ClientUtils.computeTx0Size(nbPremix, spendFromsOrNull, params);
    long tx0MinerFee = FeeUtil.getInstance().calculateFee(tx0Size, feeTx0);
    long premixMinerFee = tx0Param.getPremixValue() - tx0Param.getPool().getDenomination();
    long mixMinerFee = nbPremix * premixMinerFee;
    long spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    long spendFromBalance =
        spendFromsOrNull != null ? UnspentOutput.sumValue(spendFromsOrNull) : spendValue;
    long changeValue = spendFromBalance - spendValue;

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool,
            tx0DataOrNull,
            tx0Size,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            tx0Param.getFeeTx0(),
            tx0Param.getFeePremix(),
            premixValue,
            changeValue,
            nbPremix);

    // verify outputsSum
    long totalValue = tx0Preview.getTotalValue();
    if (totalValue != spendFromBalance) {
      throw new Exception(
          "Invalid outputsSum for tx0: "
              + totalValue
              + " vs "
              + spendFromBalance
              + " for tx0Preview=["
              + tx0Preview
              + "]");
    }
    return tx0Preview;
  }

  protected Collection<Tx0Data> fetchTx0Data(String partnerId) throws Exception {
    Collection<Tx0Data> tx0Datas = new LinkedList<Tx0Data>();
    try {
      Tx0DataRequestV2 tx0DataRequest = new Tx0DataRequestV2(config.getScode(), partnerId);
      Tx0DataResponseV2 tx0DatasResponse =
          AsyncUtil.getInstance()
              .blockingSingle(
                  config.getServerApi().fetchTx0Data(tx0DataRequest, config.isOpReturnV0()))
              .get();
      for (Tx0DataResponseV2.Tx0Data tx0DataItem : tx0DatasResponse.tx0Datas) {
        Tx0Data tx0Data = new Tx0Data(tx0DataItem);
        tx0Datas.add(tx0Data);
      }
      return tx0Datas;
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }
}
