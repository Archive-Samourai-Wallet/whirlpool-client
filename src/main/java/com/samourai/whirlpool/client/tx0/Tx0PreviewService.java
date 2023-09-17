package com.samourai.whirlpool.client.tx0;

import com.google.common.collect.Lists;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Service;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.wallet.utxo.UtxoDetailComparator;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.PoolComparatorByDenominationDesc;
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
  private BipFormatSupplier bipFormatSupplier;
  private ITx0PreviewServiceConfig config;

  public Tx0PreviewService(
      MinerFeeSupplier minerFeeSupplier,
      BipFormatSupplier bipFormatSupplier,
      ITx0PreviewServiceConfig config) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.bipFormatSupplier = bipFormatSupplier;
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
      Collection<? extends UtxoDetail> spendFrom,
      boolean isDecoyTx0x2) {
    long premixValue = tx0Param.getPremixValue();
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    int feeTx0 = tx0Param.getFeeTx0();
    Pool pool = tx0Param.getPool();

    long spendFromBalance = UtxoDetail.sumValue(spendFrom);

    // compute nbPremix ignoring TX0 fee
    int nbPremixInitial = (int) Math.ceil(spendFromBalance / premixValue);

    // compute nbPremix with TX0 fee
    int nbPremix = capNbPremix(nbPremixInitial, pool, isDecoyTx0x2);
    while (true) {
      // estimate TX0 fee for nbPremix
      int tx0Size =
          ClientUtils.computeTx0Size(
              nbPremix, isDecoyTx0x2, spendFrom, bipFormatSupplier, config.getNetworkParameters());
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

  /** Minimal preview (without SCODE calculation) for each pool. */
  public Tx0Previews tx0PreviewsMinimal(Tx0PreviewConfig tx0PreviewConfig) {
    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Pool pool : tx0PreviewConfig.getPools()) {
      final String poolId = pool.getPoolId();
      Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
      try {
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

  protected Tx0Preview tx0PreviewMinimal(Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param)
      throws Exception {
    return doTx0Preview(tx0Param, 1, null, null, null, tx0PreviewConfig.isDecoyTx0x2());
  }

  /** Preview for each pool. */
  public Tx0Previews tx0Previews(Tx0PreviewConfig tx0PreviewConfig) throws Exception {
    // fetch fresh Tx0Data
    boolean useCascading = false;
    Collection<Tx0Data> tx0Datas = fetchTx0Data(config.getPartner(), useCascading);

    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Tx0Data tx0Data : tx0Datas) {
      final String poolId = tx0Data.getPoolId();
      Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
      try {
        // real preview for outputs (with SCODE and outputs calculation)
        Tx0Preview tx0Preview = tx0Preview(tx0PreviewConfig, tx0Param, tx0Data);
        tx0PreviewsByPoolId.put(poolId, tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0Previews(tx0PreviewsByPoolId);
  }

  /** Preview a single TX0 for a specific pool. */
  public Tx0Preview tx0Preview(Tx0PreviewConfig tx0PreviewConfig, String poolId) throws Exception {
    // fetch fresh Tx0Data
    boolean useCascading = tx0PreviewConfig._isCascading();
    Collection<Tx0Data> tx0Datas = fetchTx0Data(config.getPartner(), useCascading);
    Tx0Data tx0Data =
        tx0Datas.stream().filter(td -> td.getPoolId().equals(poolId)).findFirst().get();

    // real preview for outputs (with SCODE and outputs calculation)
    Tx0Param tx0Param = getTx0Param(tx0PreviewConfig, poolId);
    return tx0Preview(tx0PreviewConfig, tx0Param, tx0Data);
  }

  /** Preview a TX0 cascade for a specific pool. */
  public Tx0PreviewCascade tx0PreviewCascade(
      Tx0PreviewConfig tx0PreviewConfig, Collection<Pool> poolsChoice) throws Exception {
    List<Tx0Preview> tx0Previews = new ArrayList<>();

    // sort pools by denomination
    List<Pool> pools = new LinkedList<>(poolsChoice);
    Collections.sort(pools, new PoolComparatorByDenominationDesc());

    // initial Tx0 on highest pool
    Iterator<Pool> poolsIter = pools.iterator();
    Pool poolInitial = poolsIter.next();
    if (log.isDebugEnabled()) {
      log.debug(" +Tx0Preview cascading for poolId=" + poolInitial.getPoolId() + "... (1/x)");
    }
    Tx0Preview tx0Preview = tx0Preview(tx0PreviewConfig, poolInitial.getPoolId());
    tx0Previews.add(tx0Preview);

    Collection<UtxoDetail> changeUtxos = null; // TODO zl !!!!tx0Preview.getChangeUtxos();

    // Tx0 cascading for remaining pools
    while (poolsIter.hasNext()) {
      Pool pool = poolsIter.next();
      if (changeUtxos.isEmpty()) {
        break; // stop when no tx0 change
      }

      try {
        if (log.isDebugEnabled()) {
          log.debug(
              " +Tx0 cascading for poolId="
                  + pool.getPoolId()
                  + "... ("
                  + (tx0Previews.size() + 1)
                  + "/x)");
        }

        tx0PreviewConfig._setCascading(true);
        tx0PreviewConfig.setSpendFroms(changeUtxos);
        tx0Preview = tx0Preview(tx0PreviewConfig, pool.getPoolId());
        tx0Previews.add(tx0Preview);
        // TODO zl !!!! changeUtxos = tx0Preview.getChangeUtxos();
      } catch (Exception e) {
        // Tx0 is not possible for this pool, ignore it
        if (log.isDebugEnabled()) {
          log.debug(
              "Tx0 cascading skipped for poolId=" + pool.getPoolId() + ": " + e.getMessage(), e);
        }
      }
    }
    return new Tx0PreviewCascade(tx0Previews);
  }

  protected Tx0Preview tx0Preview(
      Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param, Tx0Data tx0Data) throws Exception {
    Collection<? extends UtxoDetail> spendFroms = tx0PreviewConfig.getSpendFroms();
    Integer nbPremix = null;
    Collection<Long> changeAmounts = null;
    boolean isDecoyTx0x2 = false;
    if (tx0PreviewConfig.isDecoyTx0x2()) {
      // attempt to decoy Tx0x2
      Pair<Integer, Collection<Long>> changeAmountsStonewall =
          computeChangeAmountsStonewall(tx0PreviewConfig, tx0Param, tx0Data);
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
      Collection<? extends UtxoDetail> spendFromsOrNull,
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

    int tx0Size =
        ClientUtils.computeTx0Size(
            nbPremix, isDecoyTx0x2, spendFromsOrNull, bipFormatSupplier, params);
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, feeTx0, isDecoyTx0x2);
    long premixMinerFee = tx0Param.getPremixValue() - tx0Param.getPool().getDenomination();
    long mixMinerFee = nbPremix * premixMinerFee;
    long spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    long spendFromBalance =
        spendFromsOrNull != null ? UtxoDetail.sumValue(spendFromsOrNull) : spendValue;
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
            changeAmountsOrNull,
            isDecoyTx0x2);

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
      Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param, Tx0Data tx0Data)
      throws NotifiableException {
    Pair<Collection<? extends UtxoDetail>, Collection<? extends UtxoDetail>> spendFromsStonewall =
        computeSpendFromsStonewall(tx0PreviewConfig, tx0Param, tx0Data);
    if (spendFromsStonewall == null) {
      // stonewall is not possible
      return null;
    }

    Collection<? extends UtxoDetail> spendFromsA = spendFromsStonewall.getLeft();
    Collection<? extends UtxoDetail> spendFromsB = spendFromsStonewall.getRight();
    long spendValueA = UtxoDetail.sumValue(spendFromsA);
    long spendValueB = UtxoDetail.sumValue(spendFromsB);

    Collection<? extends UtxoDetail> spendFroms = tx0PreviewConfig.getSpendFroms();
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
    if (!tx0PreviewConfig._isCascading()) {
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
    NetworkParameters params = config.getNetworkParameters();
    int tx0Size =
        ClientUtils.computeTx0Size(nbPremixTotal, true, spendFroms, bipFormatSupplier, params);
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

    if (changeValueA < Tx0x2Service.CHANGE_SPLIT_THRESHOLD
        && changeValueB < Tx0x2Service.CHANGE_SPLIT_THRESHOLD) {
      // split changes evenly when both changes < treshold
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

  private Pair<Collection<? extends UtxoDetail>, Collection<? extends UtxoDetail>>
      computeSpendFromsStonewall(
          Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param, Tx0Data tx0Data)
          throws NotifiableException {
    NetworkParameters params = config.getNetworkParameters();
    Collection<? extends UtxoDetail> spendFroms = tx0PreviewConfig.getSpendFroms();

    // estimate max tx0MinerFee as it would be for regular tx0
    // it will be recalculated later more precisely with real nbPremix (wich maybe lower)
    int nbPremixMax = computeNbPremixMax(tx0Param, tx0Data, spendFroms, false);
    int tx0SizeMax =
        ClientUtils.computeTx0Size(nbPremixMax, true, spendFroms, bipFormatSupplier, params);
    long tx0MinerFeeMax = ClientUtils.computeTx0MinerFee(tx0SizeMax, tx0Param.getFeeTx0(), true);

    // split minerFee + feeValue (feeValue will be zero for lower cascading pools)
    long feeParticipation = (tx0MinerFeeMax + tx0Data.computeFeeValueOrFeeChange()) / 2L;
    long minSpendFrom = feeParticipation + tx0Param.getPremixValue(); // at least 1 must mix each
    boolean initialPool = !tx0PreviewConfig._isCascading();

    // compute pairs
    return computeSpendFromsStonewallPairs(initialPool, spendFroms, minSpendFrom);
  }

  private Pair<Collection<? extends UtxoDetail>, Collection<? extends UtxoDetail>>
      computeSpendFromsStonewallPairs(
          boolean initialPool, Collection<? extends UtxoDetail> spendFroms, long minSpendFrom)
          throws NotifiableException {

    // sort utxos descending to help avoid misses [see
    // WhirlpoolWalletDeocyTx0x2.tx0x2_decoy_3utxos()]
    if (initialPool) {
      List<UtxoDetail> sortedSpendFroms = new LinkedList<>();
      sortedSpendFroms.addAll(spendFroms);
      Collections.sort(sortedSpendFroms, new UtxoDetailComparator().reversed());
      spendFroms = sortedSpendFroms;
    }

    // check for min spend amount
    long spendFromA = 0L;
    long spendFromB = 0L;
    Map<String, UtxoDetail> spendFromsA = new HashMap<>();
    Map<String, UtxoDetail> spendFromsB = new HashMap<>();

    for (UtxoDetail spendFrom : spendFroms) {
      String hash = spendFrom.getTxHash();

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
        spendFromA += spendFrom.getValue();
      } else {
        // choose B
        spendFromsB.put(hash, spendFrom);
        spendFromB += spendFrom.getValue();
      }
    }

    if (spendFromA < minSpendFrom || spendFromB < minSpendFrom) {
      if (log.isDebugEnabled()) {
        long[] spendFromValues = spendFroms.stream().mapToLong(s -> s.getValue()).toArray();
        log.debug(
            "Decoy Tx0x2 is not possible: spendFroms="
                + Arrays.toString(spendFromValues)
                + ", spendFromA="
                + spendFromA
                + ", spendFrtomB="
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
