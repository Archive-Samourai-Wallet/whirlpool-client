package com.samourai.whirlpool.client.tx0;

import com.google.common.collect.Lists;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.UnspentOutputComparator;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.rest.Tx0DataRequestV2;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;
import java.util.*;
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

  private Tx0Param getTx0Param(Tx0PreviewConfig tx0Config, final String poolId) {
    // find pool
    Pool pool =
        tx0Config.getPools().stream()
            .filter(pool1 -> pool1.getPoolId().equals(poolId))
            .findFirst()
            .get();
    return getTx0Param(pool, tx0Config.getTx0FeeTarget(), tx0Config.getMixFeeTarget());
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
      Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
      try {
        // minimal preview estimation (without SCODE calculation)
        Tx0Preview tx0Preview = tx0PreviewMinimal(tx0PreviewConfig, tx0Param);
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
    boolean useCascading = tx0PreviewConfig.getCascadingParent() != null;
    Collection<Tx0Data> tx0Datas = fetchTx0Data(config.getPartner(), useCascading);

    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Tx0Data tx0Data : tx0Datas) {
      final String poolId = tx0Data.getPoolId();
      Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
      try {
        // real preview for outputs (with SCODE and outputs calculation)
        Tx0Preview tx0Preview = tx0Preview(tx0PreviewConfig, tx0Param, tx0Data, spendFroms);
        tx0PreviewsByPoolId.put(poolId, tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0Previews(tx0PreviewsByPoolId);
  }

  public Tx0Preview tx0Preview(
      Tx0PreviewConfig tx0PreviewConfig, Collection<UnspentOutput> spendFroms, String poolId)
      throws Exception {
    // fetch fresh Tx0Data
    boolean useCascading = tx0PreviewConfig.getCascadingParent() != null;
    Collection<Tx0Data> tx0Datas = fetchTx0Data(config.getPartner(), useCascading);
    Tx0Data tx0Data =
        tx0Datas.stream().filter(td -> td.getPoolId().equals(poolId)).findFirst().get();

    // real preview for outputs (with SCODE and outputs calculation)
    Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
    return tx0Preview(tx0PreviewConfig, tx0Param, tx0Data, spendFroms);
  }

  protected Tx0Preview tx0PreviewMinimal(Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param)
      throws Exception {
    return doTx0Preview(tx0PreviewConfig, tx0Param, 1, null, null);
  }

  protected Tx0Preview tx0Preview(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      Collection<UnspentOutput> spendFroms)
      throws Exception {
    int nbPremix = computeNbPremixMax(tx0Param, tx0Data, spendFroms);
    return doTx0Preview(tx0PreviewConfig, tx0Param, nbPremix, spendFroms, tx0Data);
  }

  protected Tx0Preview doTx0Preview(
      Tx0PreviewConfig tx0PreviewConfig,
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
            nbPremix,
            Arrays.asList(changeValue));

    List<Long> changeAmounts =
        computeChangeAmounts(
            tx0PreviewConfig, tx0Preview, tx0Param, spendFromsOrNull, tx0DataOrNull);
    if (!changeAmounts.isEmpty()) {
      //  if decoy tx0x2 change outputs total > premix output value, remove last premix output
      if (tx0PreviewConfig.isDecoyTx0x2() && changeAmounts.size() == 2) {
        if (changeAmounts.get(0) + changeAmounts.get(1) > tx0Preview.getPremixValue()) {
          // should remove at most 1 premix output
          nbPremix--;
        }
      }
    }

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

  protected Collection<Tx0Data> fetchTx0Data(String partnerId, boolean cascading) throws Exception {
    Collection<Tx0Data> tx0Datas = new LinkedList<Tx0Data>();
    try {
      Tx0DataRequestV2 tx0DataRequest =
          new Tx0DataRequestV2(config.getScode(), partnerId, cascading);
      Tx0DataResponseV2 tx0DatasResponse =
          AsyncUtil.getInstance()
              .blockingGet(
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

  private List<Long> computeChangeAmounts(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Preview tx0Preview,
      Tx0Param tx0Param,
      Collection<UnspentOutput> sortedSpendFroms,
      Tx0Data tx0Data)
      throws Exception {
    long changeValueTotal = tx0Preview.getChangeValue();

    if (changeValueTotal < 0) {
      throw new Exception(
          "Negative change detected, please report this bug. tx0Preview=" + tx0Preview);
    }
    if (changeValueTotal == 0) {
      if (log.isDebugEnabled()) {
        log.debug("Tx0: spending whole utx0, no change");
      }
      return Lists.newLinkedList();
    }

    if (tx0PreviewConfig.isDecoyTx0x2()) {
      // attempt to decoy Tx0x2: split change between 2 change addresses with STONEWALL
      List<Long> changeAmountsStonewall =
          computeChangeAmountsStonewall(
              tx0PreviewConfig, tx0Preview, tx0Param, sortedSpendFroms, tx0Data);
      if (changeAmountsStonewall != null) {
        if (log.isDebugEnabled()) {
          log.debug("Tx0: decoy Tx0, 2 changes");
        }
        return changeAmountsStonewall;
      }
    }

    // normal Tx0
    if (log.isDebugEnabled()) {
      log.debug("Tx0: normal Tx0, 1 change");
    }
    return Arrays.asList(changeValueTotal);
  }

  List<Long> computeChangeAmountsStonewall(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Preview tx0Preview,
      Tx0Param tx0Param,
      Collection<UnspentOutput> sortedSpendFroms,
      Tx0Data tx0Data)
      throws Exception {
    Pair<Long, Long> spendFromAmountsStonewall =
        computeSpendFromAmountsStonewall(
            tx0PreviewConfig, sortedSpendFroms, tx0Param, tx0Data, tx0Preview.getTx0MinerFee());
    if (spendFromAmountsStonewall == null) {
      // stonewall is not possible
      return null;
    }

    // calculate changes
    long changeValueTotalA = spendFromAmountsStonewall.getLeft(); // spend value A
    long changeValueTotalB = spendFromAmountsStonewall.getRight(); // spend value B

    if (tx0PreviewConfig.getCascadingParent() == null) {
      // initial pool, split fee
      changeValueTotalA -= tx0Preview.getFeeValue() / 2L;
      changeValueTotalB -= tx0Preview.getFeeValue() / 2L;
    } else {
      // lower pools
      changeValueTotalA -= tx0Preview.getFeeChange();
    }

    // split miner fees
    changeValueTotalA -= tx0Preview.getTx0MinerFee() / 2L;
    changeValueTotalB -= tx0Preview.getTx0MinerFee() / 2L;

    int nbPremixA = 0;
    while (changeValueTotalA > tx0Preview.getPremixValue()) {
      changeValueTotalA -= tx0Preview.getPremixValue();
      nbPremixA++;
    }
    int nbPremixB = 0;
    while (changeValueTotalB > tx0Preview.getPremixValue()) {
      changeValueTotalB -= tx0Preview.getPremixValue();
      nbPremixB++;
    }

    long changeValueTotal = changeValueTotalA + changeValueTotalB;

    // if nbPremix < tx0Preview.nbPremix => remove 1 premixOutput. should be at most 1 less.
    int nbPremix = nbPremixA + nbPremixB;
    if (nbPremix < tx0Preview.getNbPremix()) {
      // TODO - Not 100% sure if should recalculate tx0MinerFee.
      //  Recalculating makes it exact as Tx0x2, but usually a small sat difference.
      NetworkParameters params = config.getNetworkParameters();

      // recalculate tx0MinerFee
      long tx0MinerFee =
          ClientUtils.computeTx0MinerFee(
              nbPremix, tx0Preview.getTx0MinerFeePrice(), sortedSpendFroms, params);
      if (tx0MinerFee % 2L != 0) {
        tx0MinerFee++;
      }

      // add tx0MinerFee difference back to change
      long diffTx0MinerFee = tx0Preview.getTx0MinerFee() - tx0MinerFee;
      changeValueTotalA += diffTx0MinerFee / 2L;
      changeValueTotalB += diffTx0MinerFee / 2L;
      tx0Preview.setTx0MinerFee(tx0MinerFee);

      // recalculate & confirm spendValue. difference should be premixValue + diffTx0MinerFee
      long feeValueOrFeeChange =
          tx0Preview.getFeeValue() != 0 ? tx0Preview.getFeeValue() : tx0Preview.getFeeChange();
      long spendValue =
          ClientUtils.computeTx0SpendValue(
              tx0Preview.getPremixValue(), nbPremix, feeValueOrFeeChange, tx0MinerFee);
      long diffSpendValue = tx0Preview.getSpendValue() - spendValue;
      if (diffSpendValue == tx0Preview.getPremixValue() + diffTx0MinerFee) {
        // update spend value
        tx0Preview.setSpendValue(spendValue);
      } else {
        String message =
            "Miscalculated Spend Value. diffSpendValue="
                + diffSpendValue
                + "; premixValue="
                + tx0Preview.getPremixValue()
                + "; diffTx0MinerFee="
                + diffTx0MinerFee;
        if (log.isDebugEnabled()) {
          log.debug(message);
        }
        throw new Exception(message);
      }

      // confirm recalculated total value
      changeValueTotal = changeValueTotalA + changeValueTotalB;
      if (tx0Preview.getTotalValue() != spendValue + changeValueTotal) {
        String message =
            "Error calculating change. totalValue="
                + tx0Preview.getTotalValue()
                + "; spendValue="
                + spendValue
                + "; changeValue="
                + changeValueTotal
                + "; feeChange="
                + tx0Preview.getFeeChange();
        if (log.isDebugEnabled()) {
          log.debug(message);
        }
        throw new Exception(message);
      }

      // recalculate tx0 size
      int tx0Size = ClientUtils.computeTx0Size(nbPremix, sortedSpendFroms.size(), params);
      tx0Preview.setTx0Size(tx0Size);

      // reduce 1 premix
      tx0Preview.decrementNbPremix();

      // reduce 1 premix miner fee
      tx0Preview.decrementMixMinerFee();
    }

    if (tx0Preview.getPool().getPoolId().equals("0.001btc")) {
      changeValueTotalA = changeValueTotal / 2L;
      changeValueTotalB = changeValueTotal / 2L;
      if (changeValueTotal % 2L == 1) {
        tx0Preview.incrementTx0MinerFee();
      }
    }
    tx0Preview.setChangeValue(changeValueTotalA + changeValueTotalB);
    tx0Preview.setChangeAmounts(Arrays.asList(changeValueTotalA, changeValueTotalB));

    return Arrays.asList(changeValueTotalA, changeValueTotalB);
  }

  // TODO: Continue to optimize...
  private Pair<Long, Long> computeSpendFromAmountsStonewall(
      Tx0PreviewConfig tx0PreviewConfig,
      Collection<UnspentOutput> spendFroms,
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      long tx0MinerFeeMax)
      throws NotifiableException {

    long feeParticipation;
    long minSpendFromA;
    long minSpendFromB;
    if (tx0PreviewConfig.getCascadingParent() == null) {
      // initial pool
      feeParticipation = (tx0MinerFeeMax + tx0Data.getFeeValue()) / 2L; // split fees
      long minSpendFrom = feeParticipation + tx0Param.getPremixValue(); // at least 1 must mix each
      // same minSpendFrom for A & B
      minSpendFromA = minSpendFrom;
      minSpendFromB = minSpendFrom;
    } else {
      // lower pools
      feeParticipation = tx0MinerFeeMax / 2L; // split miner fee
      // fee change applied to A
      minSpendFromA = feeParticipation + tx0Data.getFeeChange() + tx0Param.getPremixValue();
      minSpendFromB = feeParticipation + tx0Param.getPremixValue();
    }

    // sort utxos descending to help avoid misses [see
    // WhirlpoolWalletDeocyTx0x2.tx0x2_decoy_3utxos()]
    if (tx0PreviewConfig.getCascadingParent() == null) {
      List<UnspentOutput> sortedSpendFroms = new LinkedList<>();
      sortedSpendFroms.addAll(spendFroms);
      Collections.sort(sortedSpendFroms, new UnspentOutputComparator().reversed());
      spendFroms = sortedSpendFroms;
    }

    // check for min spend amount
    long spendFromA = 0L;
    long spendFromB = 0L;
    Map<String, UnspentOutput> spendFromsA = new HashMap<>();
    Map<String, UnspentOutput> spendFromsB = new HashMap<>();
    if (tx0PreviewConfig.getCascadingParent() == null) {
      // initial pool: check outpoints
      for (UnspentOutput spendFrom : spendFroms) {
        String hash = spendFrom.tx_hash;

        if (spendFromA < minSpendFromA) {
          spendFromsA.put(hash, spendFrom);
          spendFromA += spendFrom.value; // must reach minSpendFrom for A
        } else if (spendFromB < minSpendFromB && !spendFromsA.containsKey(hash)) {
          spendFromsB.put(hash, spendFrom);
          spendFromB += spendFrom.value; // must reach minSpendFrom for B
        } else {
          // random factor when minSpendFrom is reached
          if (RandomUtil.getInstance().random(0, 1) == 1) {
            if (!spendFromsB.containsKey(hash)) {
              spendFromsA.put(hash, spendFrom);
              spendFromA += spendFrom.value;
            } else {
              spendFromsB.put(hash, spendFrom);
              spendFromB += spendFrom.value;
            }
          } else {
            if (!spendFromsA.containsKey(hash)) {
              spendFromsB.put(hash, spendFrom);
              spendFromB += spendFrom.value;
            } else {
              spendFromsA.put(hash, spendFrom);
              spendFromA += spendFrom.value;
            }
          }
        }
      }
    } else {
      // lower pools: does not check outpoints
      for (UnspentOutput spendFrom : spendFroms) {
        String hash = spendFrom.tx_hash;
        if (spendFromA < minSpendFromA) {
          // must reach minSpendFrom for A
          spendFromA += spendFrom.value;
          spendFromsA.put(hash, spendFrom);
        } else if (spendFromB < minSpendFromB) {
          // must reach minSpendFrom for B
          spendFromB += spendFrom.value;
          spendFromsB.put(hash, spendFrom);
        } else {
          // random factor when minSpendFrom is reached
          if (RandomUtil.getInstance().random(0, 1) == 1) {
            spendFromA += spendFrom.value;
            spendFromsA.put(hash, spendFrom);
          } else {
            spendFromB += spendFrom.value;
            spendFromsB.put(hash, spendFrom);
          }
        }
      }
    }

    if (spendFromA < minSpendFromA || spendFromB < minSpendFromB) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Stonewall is not possible for TX0 decoy change: spendFromA="
                + spendFromA
                + ", spendFromB="
                + spendFromB
                + ", minSpendFromA="
                + minSpendFromA
                + ", minSpendFromB="
                + minSpendFromB);
      }

      // if both inputs (higher pool change outputs) are not large enough for decoy tx02, skip to
      // next lower pool
      if (tx0PreviewConfig.getCascadingParent() != null) {
        throw new NotifiableException(
            "Decoy Tx0x2 not possible for lower pool: " + tx0Data.getPoolId());
      }

      return null;
    }

    return Pair.of(spendFromA, spendFromB);
  }
}
