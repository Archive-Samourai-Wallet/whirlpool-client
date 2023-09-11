package com.samourai.whirlpool.client.tx0;

import com.google.common.collect.Lists;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.UnspentOutputComparator;
import com.samourai.wallet.util.AsyncUtil;
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
  private final static long DECOY_CHANGE_SPLIT_THRESHOLD = 100000L; // 0.001btc pool denomination

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

  protected int capNbPremix(int nbPremix, Pool pool, boolean isDecoyTx0x2) {
    int maxOutputs = config.getTx0MaxOutputs();
    if (maxOutputs > 0) {
      nbPremix = Math.min(maxOutputs, nbPremix); // cap with maxOutputs
    }
    int poolMaxOutputs = pool.getTx0MaxOutputs();
    if (isDecoyTx0x2) {
      poolMaxOutputs /= 2; // cap each tx0x2 participant to half
    }
    nbPremix = Math.min(poolMaxOutputs, nbPremix); // cap with pool.tx0MaxOutputs
    return nbPremix;
  }

  private int computeNbPremixMax(
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      Collection<UnspentOutput> spendFrom,
      boolean isDecoyTx0x2) {
    long premixValue = tx0Param.getPremixValue();
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    int feeTx0 = tx0Param.getFeeTx0();
    Pool pool = tx0Param.getPool();

    NetworkParameters params = config.getNetworkParameters();
    long spendFromBalance = UnspentOutput.sumValue(spendFrom);

    // compute nbPremix ignoring TX0 fee
    int nbPremixInitial = (int) Math.ceil(spendFromBalance / premixValue);

    // compute nbPremix with TX0 fee
    int nbPremix = capNbPremix(nbPremixInitial, pool, isDecoyTx0x2);
    while (true) {
      // estimate TX0 fee for nbPremix
      int tx0Size = ClientUtils.computeTx0Size(nbPremix, spendFrom, params);
      long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, feeTx0, isDecoyTx0x2);
      if (isDecoyTx0x2) {
        tx0MinerFee /= 2; // split minerFee
      }
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
                + ", isDecoyTx0x2="
                + isDecoyTx0x2
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
    return doTx0Preview(tx0Param, 1, null, null, null, tx0PreviewConfig.isDecoyTx0x2());
  }

  protected Tx0Preview tx0Preview(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      Collection<UnspentOutput> spendFroms)
      throws Exception {
    Integer nbPremix = null;
    Collection<Long> changeAmounts = null;
    boolean isDecoyTx0x2 = false;
    if (tx0PreviewConfig.isDecoyTx0x2()) {
      // attempt to decoy Tx0x2
      Pair<Integer, Collection<Long>> changeAmountsStonewall =
          computeChangeAmountsStonewall(tx0PreviewConfig, tx0Param, spendFroms, tx0Data);
      if (changeAmountsStonewall != null) {
        // use tx0x2 decoy
        nbPremix = changeAmountsStonewall.getLeft();
        changeAmounts = changeAmountsStonewall.getRight();
        isDecoyTx0x2 = true;
        if (log.isDebugEnabled()) {
          log.debug("Tx0: decoy Tx0, " + changeAmounts.size() + " changes");
        }
      }
    }
    if (nbPremix == null) {
      // use regular Tx0 (no decoy)
      nbPremix = computeNbPremixMax(tx0Param, tx0Data, spendFroms, false);
      changeAmounts = null; // regular changes
      isDecoyTx0x2 = false;
    }
    return doTx0Preview(tx0Param, nbPremix, spendFroms, tx0Data, changeAmounts, isDecoyTx0x2);
  }

  protected Tx0Preview doTx0Preview(
      Tx0Param tx0Param,
      int nbPremix,
      Collection<UnspentOutput> spendFromsOrNull,
      Tx0Data tx0DataOrNull,
      Collection<Long> changeAmountsOrNull,
      boolean isDecoyTx0x2)
      throws Exception {
    if (nbPremix < 1) {
      log.debug(
          "Tx0 not possible for poolId="
              + tx0Param.getPool().getPoolId()
              + ": nbPremix="
              + nbPremix);
      return null;
    }

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
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, feeTx0, isDecoyTx0x2);
    long premixMinerFee = tx0Param.getPremixValue() - tx0Param.getPool().getDenomination();
    long mixMinerFee = nbPremix * premixMinerFee;
    long spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    long spendFromBalance =
        spendFromsOrNull != null ? UnspentOutput.sumValue(spendFromsOrNull) : spendValue;
    long changeValue = spendFromBalance - spendValue;
    if (changeAmountsOrNull == null) {
      // regular tx0 change: O or 1 change
      changeAmountsOrNull = (changeValue == 0 ? Lists.newLinkedList() : Arrays.asList(changeValue));
    } else {
      // decoy tx0x2: split changeValue across multiple changes
      long changeAmountsSum = changeAmountsOrNull.stream().mapToLong(v -> v).sum();
      if (changeValue != changeAmountsSum) {
        throw new Exception(
            "Invalid changeValue=" + changeValue + " vs changeAmountsSum=" + changeAmountsSum);
      }
      changeValue = changeAmountsSum;
    }

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
            changeAmountsOrNull);

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

  private Pair<Integer, Collection<Long>> computeChangeAmountsStonewall(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Param tx0Param,
      Collection<UnspentOutput> spendFroms,
      Tx0Data tx0Data)
      throws NotifiableException {
    Pair<Collection<UnspentOutput>, Collection<UnspentOutput>> spendFromsStonewall =
        computeSpendFromsStonewall(tx0PreviewConfig, spendFroms, tx0Param, tx0Data);
    if (spendFromsStonewall == null) {
      // stonewall is not possible
      return null;
    }

    Collection<UnspentOutput> spendFromsA = spendFromsStonewall.getLeft();
    Collection<UnspentOutput> spendFromsB = spendFromsStonewall.getRight();
    long spendValueA = UnspentOutput.sumValue(spendFromsA);
    long spendValueB = UnspentOutput.sumValue(spendFromsB);

    if (log.isDebugEnabled()) {
      log.debug(
          "computeTx0PreviewDecoy: spendFroms = "
              + spendFromsA
              + " + "
              + spendFromsB
              + " = "
              + (spendFroms.size() + " utxos"));
      log.debug(
          "computeTx0PreviewDecoy: spendValue = "
              + spendValueA
              + " ("
              + spendFromsA.size()
              + " utxos)"
              + " + "
              + spendValueB
              + " ("
              + spendFromsB.size()
              + " utxos)"
              + " = "
              + (spendValueA + spendValueB)
              + " ("
              + spendFroms.size()
              + " utxos)");
    }

    // recalculate nbPremix (which may be lower than initial tx0Preview due to stonewall overhead)
    int nbPremixA = computeNbPremixMax(tx0Param, tx0Data, spendFromsA, true);
    int nbPremixB = computeNbPremixMax(tx0Param, tx0Data, spendFromsB, true);
    int nbPremixTotal = nbPremixA + nbPremixB;

    // calculate changes
    long changeValueA = spendValueA - nbPremixA * tx0Param.getPremixValue();
    long changeValueB = spendValueB - nbPremixB * tx0Param.getPremixValue();

    // deduce feeValue
    if (tx0PreviewConfig.getCascadingParent() == null) {
      // initial pool, split fee
      long feeValueA = (long) Math.ceil(tx0Data.getFeeValue() / 2);
      long feeValueB = tx0Data.getFeeValue() - feeValueA;
      changeValueA -= feeValueA;
      changeValueB -= feeValueB;
      log.debug(
          "computeTx0PreviewDecoy: feeValue = "
              + feeValueA
              + " + "
              + feeValueB
              + " = "
              + tx0Data.getFeeValue());
    } else {
      // lower pools
      changeValueA -= tx0Data.getFeeChange();
    }

    // calculate tx0MinerFee for new nbPremix
    int tx0Size =
        ClientUtils.computeTx0Size(nbPremixTotal, spendFroms, config.getNetworkParameters());
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, tx0Param.getFeeTx0(), true);

    // split miner fees
    long minerFeeA = tx0MinerFee / 2;
    long minerFeeB = minerFeeA;
    changeValueA -= minerFeeA;
    changeValueB -= minerFeeB;

    long changeValueTotal = changeValueA + changeValueB;
    if (log.isDebugEnabled()) {
      log.debug(
          "computeTx0PreviewDecoy: nbPremix = "
              + nbPremixA
              + " + "
              + nbPremixB
              + " = "
              + nbPremixTotal);
      log.debug(
          "computeTx0PreviewDecoy: minerFee = "
              + minerFeeA
              + " + "
              + minerFeeB
              + " = "
              + tx0MinerFee);
      log.debug(
          "computeTx0PreviewDecoy: changeValue = "
              + changeValueA
              + " + "
              + changeValueB
              + " = "
              + changeValueTotal);
    }

    if (changeValueA < DECOY_CHANGE_SPLIT_THRESHOLD && changeValueB< DECOY_CHANGE_SPLIT_THRESHOLD) {
      // split changes evenly
      changeValueA = changeValueTotal / 2L;
      changeValueB = changeValueTotal - changeValueA;
      if (log.isDebugEnabled()) {
        log.debug("computeTx0PreviewDecoy: changeValue2 = " + changeValueA + " + " + changeValueB);
      }
    }

    // recalculate preview with final changeValues
    Collection<Long> changeValues = Arrays.asList(changeValueA, changeValueB);
    return Pair.of(nbPremixTotal, changeValues);
  }

  private Pair<Collection<UnspentOutput>, Collection<UnspentOutput>> computeSpendFromsStonewall(
      Tx0PreviewConfig tx0PreviewConfig,
      Collection<UnspentOutput> spendFroms,
      Tx0Param tx0Param,
      Tx0Data tx0Data)
      throws NotifiableException {

    // estimate max tx0MinerFee as it would be for regular tx0
    // it will be recalculated later more precisely with real nbPremix (wich maybe lower)
    NetworkParameters params = config.getNetworkParameters();
    int nbPremixMax = computeNbPremixMax(tx0Param, tx0Data, spendFroms, false);
    int tx0SizeMax = ClientUtils.computeTx0Size(nbPremixMax, spendFroms, params);
    long tx0MinerFeeMax = ClientUtils.computeTx0MinerFee(tx0SizeMax, tx0Param.getFeeTx0(), true);

    // split minerFee + feeValue (feeValue will be zero for lower cascading pools)
    long feeParticipation = (tx0MinerFeeMax + tx0Data.computeFeeValueOrFeeChange()) / 2L;
    long minSpendFrom = feeParticipation + tx0Param.getPremixValue(); // at least 1 must mix each
    boolean initialPool = (tx0PreviewConfig.getCascadingParent() == null);

    // compute pairs
    return computeSpendFromsStonewallPairs(initialPool, spendFroms, minSpendFrom);
  }

  private Pair<Collection<UnspentOutput>, Collection<UnspentOutput>>
      computeSpendFromsStonewallPairs(
          boolean initialPool, Collection<UnspentOutput> spendFroms, long minSpendFrom)
          throws NotifiableException {

    // sort utxos descending to help avoid misses [see
    // WhirlpoolWalletDeocyTx0x2.tx0x2_decoy_3utxos()]
    if (initialPool) {
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

    for (UnspentOutput spendFrom : spendFroms) {
      String hash = spendFrom.tx_hash;

      // initial pool: check outpoints to avoid spending same prev-tx from both A & B
      // lower pools: does not check outpoints as we are cascading from same prev-tx0
      boolean allowA = !initialPool || !spendFromsB.containsKey(hash);
      boolean allowB = !initialPool || !spendFromsA.containsKey(hash);

      boolean choice; // true=A, false=B
      if (allowA && spendFromA < minSpendFrom) {
        // must reach minSpendFrom for A
        choice = true; // choose A
      } else if (allowB && spendFromB < minSpendFrom) {
        // must reach minSpendFrom for B
        choice = false;
      } else {
        // random factor when minSpendFrom is reached
        choice = (RandomUtil.getInstance().random(0, 1) == 0);
      }

      if (choice) {
        // choose A
        spendFromsA.put(hash, spendFrom);
        spendFromA += spendFrom.value;
      } else {
        // choose B
        spendFromsB.put(hash, spendFrom);
        spendFromB += spendFrom.value;
      }
    }

    if (spendFromA < minSpendFrom || spendFromB < minSpendFrom) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Stonewall is not possible for TX0 decoy change: spendFromA="
                + spendFromA
                + ", spendFromB="
                + spendFromB
                + ", minSpendFrom="
                + minSpendFrom);
      }

      // if both inputs (higher pool change outputs) are not large enough for decoy tx02, skip to
      // next lower pool
      if (!initialPool) {
        throw new NotifiableException("Decoy Tx0x2 not possible for lower pool");
      }

      return null;
    }
    return Pair.of(spendFromsA.values(), spendFromsB.values());
  }
}
