package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Service;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.wallet.utxo.UtxoDetailComparator;
import com.samourai.wallet.utxo.UtxoDetailImpl;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.PoolComparatorByDenominationDesc;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.rest.Tx0DataRequestV2;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;
import java.util.*;
import java.util.stream.Collectors;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0PreviewService {
  private Logger log = LoggerFactory.getLogger(Tx0PreviewService.class);
  private static final UtxoUtil utxoUtil = UtxoUtil.getInstance();

  private MinerFeeSupplier minerFeeSupplier;
  private BipFormatSupplier bipFormatSupplier;
  private ITx0PreviewServiceConfig config;
  private PoolSupplier poolSupplier;

  public Tx0PreviewService(
      MinerFeeSupplier minerFeeSupplier,
      BipFormatSupplier bipFormatSupplier,
      ITx0PreviewServiceConfig config) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.bipFormatSupplier = bipFormatSupplier;
    this.config = config;
    this.poolSupplier = null; // will be set later
  }

  public void _setPoolSupplier(PoolSupplier poolSupplier) {
    this.poolSupplier = poolSupplier;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  private Tx0Param getTx0Param(Pool pool, Tx0PreviewConfig tx0Config) {
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
    int poolMaxOutputs = pool.getTx0MaxOutputs();
    if (isDecoyTx0x2) {
      // cap each tx0x2 participant to half
      maxOutputs /= 2;
      poolMaxOutputs /= 2;
    }
    if (maxOutputs > 0) {
      nbPremix = Math.min(maxOutputs, nbPremix); // cap with maxOutputs
    }
    nbPremix = Math.min(poolMaxOutputs, nbPremix); // cap with pool.tx0MaxOutputs
    return nbPremix;
  }

  private int computeNbPremixMax(
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      Collection<? extends UtxoDetail> spendFrom,
      boolean tx0x2) {
    long premixValue = tx0Param.getPremixValue();
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    int feeTx0 = tx0Param.getFeeTx0();
    Pool pool = tx0Param.getPool();

    long spendFromBalance = UtxoDetail.sumValue(spendFrom);

    // compute nbPremix ignoring TX0 fee
    int nbPremixInitial = (int) Math.ceil(spendFromBalance / premixValue);
    int nbPremixCap = capNbPremix(nbPremixInitial, pool, tx0x2);

    // compute nbPremix with TX0 fee
    long spendValue;
    int nbPremix = nbPremixCap;
    while (true) {
      // estimate TX0 fee for nbPremix
      int tx0Size =
          ClientUtils.computeTx0Size(
              nbPremix, tx0x2, spendFrom, bipFormatSupplier, config.getNetworkParameters());
      long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, feeTx0, tx0x2);
      if (tx0x2) {
        tx0MinerFee /= 2; // split minerFee
      }
      spendValue =
          ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
      if (log.isTraceEnabled()) {
        log.trace(
            "computeNbPremixMax: nbPremix="
                + nbPremix
                + " => spendValue="
                + spendValue
                + ", tx0MinerFee="
                + tx0MinerFee
                + ", decoyTx0x2="
                + tx0x2
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
    if (nbPremix == 0) {
      if (log.isDebugEnabled()) {
        log.debug(
            "TX0 is not possible: poolId="
                + pool.getPoolId()
                + ", nbPremixCap="
                + nbPremixCap
                + ", spendFromBalance="
                + spendFromBalance
                + " < spendValue="
                + spendValue);
      }
    }
    return nbPremix;
  }

  /** Minimal preview (without SCODE calculation) for each pool. */
  public Tx0PreviewResult tx0PreviewsMinimal(
      Tx0PreviewConfig tx0PreviewConfig, Collection<Pool> pools) {
    List<Tx0Preview> tx0Previews = new LinkedList<>();
    for (Pool pool : pools) {
      final String poolId = pool.getPoolId();
      Tx0Param tx0Param = getTx0Param(pool, tx0PreviewConfig);
      try {
        Tx0Preview tx0Preview = tx0PreviewMinimal(tx0PreviewConfig, tx0Param);
        tx0Previews.add(tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0PreviewResult(tx0Previews);
  }

  /** Maximal preview (without SCODE calculation) for each pool. */
  public Tx0PreviewResult tx0PreviewsMaximal(
      Tx0PreviewConfig tx0PreviewConfig, Collection<Pool> pools) {
    List<Tx0Preview> tx0Previews = new LinkedList<>();
    for (Pool pool : pools) {
      final String poolId = pool.getPoolId();
      Tx0Param tx0Param = getTx0Param(pool, tx0PreviewConfig);
      try {
        Tx0Preview tx0Preview = tx0PreviewMaximal(tx0PreviewConfig, tx0Param);
        tx0Previews.add(tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0PreviewResult(tx0Previews);
  }

  protected Tx0Preview tx0PreviewMinimal(Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param)
      throws Exception {
    tx0PreviewConfig.setTx0x2Decoy(false); // no decoy for minimal preview
    return doTx0Preview(tx0Param, 1, null, null, null).get();
  }

  protected Tx0Preview tx0PreviewMaximal(Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param)
      throws Exception {
    tx0PreviewConfig.setTx0x2Decoy(false); // no decoy for minimal preview
    int tx0MaxOutputs = tx0Param.getPool().getTx0MaxOutputs();
    return doTx0Preview(tx0Param, tx0MaxOutputs, null, null, null).get();
  }

  public Optional<Tx0Preview> tx0PreviewSingle(Tx0Config tx0ConfigOrig) throws Exception {
    Tx0Config tx0Config = new Tx0Config(tx0ConfigOrig);
    tx0Config.setCascade(false); // single TX0 preview
    Pool pool = tx0Config.getPool();
    Optional<Tx0PreviewResult> tx0PreviewResult = tx0Preview(tx0Config, pool);
    if (tx0PreviewResult.isPresent()) {
      return tx0PreviewResult.get().getByPoolId(pool.getPoolId());
    }
    return Optional.empty();
  }

  public Optional<Tx0PreviewResult> tx0Preview(Tx0PreviewConfig tx0PreviewConfig, Pool pool)
      throws Exception {
    Map<String, Tx0PreviewResult> tx0Previews = tx0Previews(tx0PreviewConfig, Arrays.asList(pool));
    Tx0PreviewResult tx0PreviewResult = tx0Previews.get(pool.getPoolId());
    return Optional.ofNullable(tx0PreviewResult);
  }

  /** Preview single TX0 for each pool */
  public Map<String, Tx0PreviewResult> tx0Previews(
      Tx0PreviewConfig tx0PreviewConfig, Collection<Pool> pools) throws Exception {
    // fetch fresh Tx0Data
    boolean useCascading = tx0PreviewConfig._isCascading();
    Map<String, Tx0Data> tx0Datas = fetchTx0Data(useCascading);

    // preview single Tx0 for each pool
    Map<String, Tx0PreviewResult> tx0Previews = new LinkedHashMap<>();
    for (Pool pool : pools) {
      final String poolId = pool.getPoolId();
      Tx0Data tx0Data = tx0Datas.get(poolId);
      if (tx0Data != null) {
        // real preview for outputs (with SCODE and outputs calculation)
        Tx0PreviewResult tx0PreviewResult = null;
        if (tx0PreviewConfig.isCascade()) {
          // cascading TX0
          tx0PreviewResult = tx0PreviewCascade(tx0PreviewConfig, tx0Data, pool).orElse(null);
        } else {
          // single TX0
          Tx0Preview tx0Preview = tx0PreviewSingle(tx0PreviewConfig, tx0Data, pool).orElse(null);
          if (tx0Preview != null) {
            tx0PreviewResult = new Tx0PreviewResult(Arrays.asList(tx0Preview));
          }
        }
        if (tx0PreviewResult != null) {
          tx0Previews.put(poolId, tx0PreviewResult);
        }
      }
    }
    return tx0Previews;
  }

  /** Preview a TX0 cascade. */
  protected Optional<Tx0PreviewResult> tx0PreviewCascade(
      Tx0PreviewConfig tx0PreviewConfigCascade, Tx0Data tx0DataInitial, Pool pool)
      throws Exception {
    List<Tx0Preview> tx0Previews = new ArrayList<>();

    // initial Tx0 on highest pool
    String poolIdInitial = tx0DataInitial.getPoolId();
    if (log.isDebugEnabled()) {
      log.debug(" +Tx0Preview cascading (1/x): trying poolId=" + poolIdInitial);
    }
    Tx0Preview tx0PreviewInitial =
        tx0PreviewSingle(tx0PreviewConfigCascade, tx0DataInitial, pool).orElse(null);
    if (tx0PreviewInitial == null) {
      return Optional.empty(); // TX0 is not possible
    }
    tx0Previews.add(tx0PreviewInitial);

    // sort cascading pools by denomination
    List<Pool> cascadingPools = findCascadingPools(poolIdInitial);
    Collections.sort(cascadingPools, new PoolComparatorByDenominationDesc());

    // cascading datas
    Map<String, Tx0Data> tx0DataCascadings = fetchTx0Data(true);

    // Tx0 cascading for remaining pools
    Collection<UtxoDetail> changeUtxos = mockChangeUtxos(tx0PreviewInitial.getChangeAmountsAll());
    for (Pool cascadingPool : cascadingPools) {
      if (changeUtxos.isEmpty()) {
        break; // stop when no tx0 change
      }

      String poolId = cascadingPool.getPoolId();
      if (log.isDebugEnabled()) {
        log.debug(
            " +Tx0Preview cascading (" + (tx0Previews.size() + 1) + "/x): trying poolId=" + poolId);
      }
      Tx0Data tx0DataCascading = tx0DataCascadings.get(poolId);
      if (tx0DataCascading != null) {
        Tx0PreviewConfig tx0PreviewConfigLower =
            new Tx0PreviewConfig(tx0PreviewConfigCascade, changeUtxos);
        tx0PreviewConfigLower._setCascading(true);
        tx0PreviewConfigLower.setTx0x2DecoyForced(
            true); // skip to next lower pool when decoy is not possible
        if (tx0PreviewConfigCascade.isTx0x2Cahoots()) {
          Collection<UtxoDetail> changeCounterparty =
              mockChangeUtxos(
                  Arrays.asList(tx0PreviewInitial.getTx0x2Preview().getChangeAmountCounterparty()));
          tx0PreviewConfigLower.setTx0x2SpendFromsCounterparty(changeCounterparty);
        }
        Tx0Preview tx0PreviewLower =
            tx0PreviewSingle(tx0PreviewConfigLower, tx0DataCascading, cascadingPool).orElse(null);
        if (tx0PreviewLower != null) {
          tx0Previews.add(tx0PreviewLower);
          changeUtxos = mockChangeUtxos(tx0PreviewLower.getChangeAmountsAll());
        }
      }
    }
    return Optional.of(new Tx0PreviewResult(tx0Previews));
  }

  protected Collection<UtxoDetail> mockChangeUtxos(Collection<Long> changeAmounts) {
    String mockAddressBech32 =
        new SegwitAddress(new ECKey(), config.getNetworkParameters()).getBech32AsString();
    return changeAmounts.stream()
        .map(value -> new UtxoDetailImpl("previewhash", 0, value, mockAddressBech32, null))
        .collect(Collectors.toList());
  }

  protected Optional<Tx0Preview> tx0PreviewSingle(
      Tx0PreviewConfig tx0PreviewConfig, Tx0Data tx0Data, Pool pool) throws Exception {
    Tx0Param tx0Param = getTx0Param(pool, tx0PreviewConfig);
    return tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param);
  }

  protected Optional<Tx0Preview> tx0PreviewSingle(
      Tx0PreviewConfig tx0PreviewConfig, Tx0Data tx0Data, Tx0Param tx0Param) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0Preview["
              + tx0Param.getPool().getPoolId()
              + "] tx0PreviewSingle: config={"
              + tx0PreviewConfig
              + "}");
    }
    Collection<UtxoDetail> allSpendFroms = null;
    Integer nbPremix = null;
    Tx0x2Preview tx0x2Preview = null;

    if (tx0PreviewConfig.isTx0x2Any()) {
      Collection<UtxoDetail> spendFromsA = null;
      Collection<UtxoDetail> spendFromsB = null;
      if (tx0PreviewConfig.isTx0x2Cahoots()) {
        // 2-party TX0X2
        spendFromsA = (Collection<UtxoDetail>) tx0PreviewConfig.getOwnSpendFroms();
        spendFromsB = (Collection<UtxoDetail>) tx0PreviewConfig.getTx0x2SpendFromsCounterparty();
      } else {
        // decoy Tx0x2
        Pair<Collection<UtxoDetail>, Collection<UtxoDetail>> spendFromsEach =
            computeSpendFromsStonewall(tx0PreviewConfig, tx0Param, tx0Data);
        if (spendFromsEach != null) {
          spendFromsA = spendFromsEach.getLeft();
          spendFromsB = spendFromsEach.getRight();
        }
      }
      if (spendFromsA != null && spendFromsB != null) {
        // attempt tx0x2 (2-party or decoy)
        tx0x2Preview =
            computeTx0x2Preview(tx0PreviewConfig, tx0Param, tx0Data, spendFromsA, spendFromsB);
      }
      if (tx0x2Preview != null) {
        // tx0x2 success (2-party or decoy)
        nbPremix = tx0x2Preview.getNbPremixSender() + tx0x2Preview.getNbPremixCounterparty();
        allSpendFroms = new LinkedList<>(spendFromsA);
        allSpendFroms.addAll(spendFromsB);
        if (log.isDebugEnabled()) {
          log.debug(
              "Tx0Preview[" + tx0Data.getPoolId() + "]: tx0x2Preview={" + tx0x2Preview + " }");
        }
      } else {
        // tx0x2 decoy not possible
        if (tx0PreviewConfig.isTx0x2DecoyForced() || tx0PreviewConfig.isTx0x2Cahoots()) {
          if (log.isDebugEnabled()) {
            log.debug("Tx0[" + tx0Data.getPoolId() + "]: decoy Tx0 is not possible => skipping");
          }
          return Optional.empty(); // skip to next lower pool instead of regular tx0 fallback
        } else {
          if (log.isDebugEnabled()) {
            log.debug(
                "Tx0["
                    + tx0Param.getPool().getPoolId()
                    + "]: decoy Tx0 is not possible => trying regular Tx0");
          }
        }
      }
    }
    if (nbPremix == null) {
      // regular Tx0 (no decoy)
      allSpendFroms = (Collection<UtxoDetail>) tx0PreviewConfig.getOwnSpendFroms();
      nbPremix = computeNbPremixMax(tx0Param, tx0Data, allSpendFroms, false);
    }
    return doTx0Preview(tx0Param, nbPremix, allSpendFroms, tx0Data, tx0x2Preview);
  }

  protected Optional<Tx0Preview> doTx0Preview(
      Tx0Param tx0Param,
      int nbPremix,
      Collection<? extends UtxoDetail> allSpendFromsOrNull,
      Tx0Data tx0DataOrNull,
      Tx0x2Preview tx0x2Preview)
      throws Exception {
    String poolId = tx0Param.getPool().getPoolId();
    if (nbPremix < 1) {
      log.debug(
          "Tx0["
              + poolId
              + "] not possible: nbPremix="
              + nbPremix
              + ", allSpendFroms="
              + (allSpendFromsOrNull != null ? UtxoDetail.sumValue(allSpendFromsOrNull) : "null"));
      return Optional.empty();
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

    boolean tx0x2Any = tx0x2Preview != null;
    int tx0Size =
        ClientUtils.computeTx0Size(
            nbPremix, tx0x2Any, allSpendFromsOrNull, bipFormatSupplier, params);
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, feeTx0, tx0x2Any);
    if (log.isDebugEnabled()) {
      String allSPendFromsStr =
          allSpendFromsOrNull != null ? allSpendFromsOrNull.size() + " utxos" : "null";
      log.debug(
          "Tx0Preview["
              + poolId
              + "] tx0Size="
              + tx0Size
              + ", tx0MinerFee="
              + tx0MinerFee
              + ", nbPremix="
              + nbPremix
              + ", tx0x2Any="
              + tx0x2Any
              + ", allSpendFroms="
              + allSPendFromsStr);
    }
    long premixMinerFee = tx0Param.getPremixValue() - tx0Param.getPool().getDenomination();
    long mixMinerFee = nbPremix * premixMinerFee;
    long spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    long spendFromValue =
        allSpendFromsOrNull != null ? UtxoDetail.sumValue(allSpendFromsOrNull) : spendValue;
    long changeValue = spendFromValue - spendValue;
    if (log.isTraceEnabled()) {
      log.trace(
          "spendFromBalance="
              + spendFromValue
              + ", changeValue="
              + spendFromValue
              + "(spendFromValue) - "
              + spendValue
              + "(spendValue) = "
              + changeValue);
    }

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool,
            tx0DataOrNull,
            spendFromValue,
            tx0Size,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            tx0Param.getFeeTx0(),
            tx0Param.getFeePremix(),
            premixValue,
            changeValue,
            nbPremix,
            tx0x2Preview);
    return Optional.of(tx0Preview);
  }

  public Map<String, Tx0Data> fetchTx0Data(boolean cascading) throws Exception {
    Map<String, Tx0Data> tx0Datas = new LinkedHashMap<>();
    try {
      Tx0DataRequestV2 tx0DataRequest =
          new Tx0DataRequestV2(config.getScode(), config.getPartner(), cascading);
      Tx0DataResponseV2 tx0DatasResponse =
          AsyncUtil.getInstance()
              .blockingGet(
                  config.getServerApi().fetchTx0Data(tx0DataRequest, config.isOpReturnV0()))
              .get();
      for (Tx0DataResponseV2.Tx0Data tx0DataItem : tx0DatasResponse.tx0Datas) {
        Tx0Data tx0Data = new Tx0Data(tx0DataItem);
        tx0Datas.put(tx0Data.getPoolId(), tx0Data);
      }
      return tx0Datas;
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }

  private Tx0x2Preview computeTx0x2Preview(
      Tx0PreviewConfig tx0PreviewConfig,
      Tx0Param tx0Param,
      Tx0Data tx0Data,
      Collection<UtxoDetail> spendFromsA,
      Collection<UtxoDetail> spendFromsB) {
    long spendValueA = UtxoDetail.sumValue(spendFromsA);
    long spendValueB = UtxoDetail.sumValue(spendFromsB);

    Collection<UtxoDetail> spendFromsAll = new LinkedList<>();
    spendFromsAll.addAll(spendFromsA);
    spendFromsAll.addAll(spendFromsB);
    if (log.isDebugEnabled()) {
      log.debug(
          "computeTx0x2Changes: spendFromsA="
              + debugUtxos(spendFromsA)
              + ", \nspendFromsB="
              + debugUtxos(spendFromsB)
              + "\nspendValue = "
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
              + spendFromsAll.size()
              + " utxos)");
    }

    // recalculate nbPremix (which may be lower than initial tx0Preview due to stonewall overhead)
    int nbPremixA = computeNbPremixMax(tx0Param, tx0Data, spendFromsA, true);
    int nbPremixB = computeNbPremixMax(tx0Param, tx0Data, spendFromsB, true);
    if (nbPremixA == 0 || nbPremixB == 0) {
      // tx0x2Decoy is not possible
      return null;
    }
    int nbPremixTotal = nbPremixA + nbPremixB;

    // calculate changes
    long changeValueA = spendValueA - nbPremixA * tx0Param.getPremixValue();
    long changeValueB = spendValueB - nbPremixB * tx0Param.getPremixValue();

    // deduce feeValue (initial pool: split feeValue, lower pools: split feeChange received back via
    // samouraiFeeOutput)
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    long feeValueA = (long) Math.ceil(feeValueOrFeeChange / 2);
    long feeValueB = feeValueOrFeeChange - feeValueA;
    changeValueA -= feeValueA;
    changeValueB -= feeValueB;

    // calculate tx0MinerFee for new nbPremix
    NetworkParameters params = config.getNetworkParameters();
    int tx0Size =
        ClientUtils.computeTx0Size(nbPremixTotal, true, spendFromsAll, bipFormatSupplier, params);
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(tx0Size, tx0Param.getFeeTx0(), true);

    // split miner fees
    long minerFeeA = tx0MinerFee / 2;
    long minerFeeB = minerFeeA;
    changeValueA -= minerFeeA;
    changeValueB -= minerFeeB;

    long changeValueTotal = changeValueA + changeValueB;
    if (log.isDebugEnabled()) {
      log.debug(
          "computeTx0x2DecoyChanges: feeValueOrFeeChange = "
              + feeValueA
              + " + "
              + feeValueB
              + " = "
              + tx0Data.getFeeValue()
              + "\nnbPremix = "
              + nbPremixA
              + " + "
              + nbPremixB
              + " = "
              + nbPremixTotal
              + "\nminerFee = "
              + minerFeeA
              + " + "
              + minerFeeB
              + " = "
              + tx0MinerFee
              + "\nchangeValue = "
              + changeValueA
              + " + "
              + changeValueB
              + " = "
              + changeValueTotal);
    }

    boolean splitChange = false;
    if (changeValueA < Tx0x2Service.CHANGE_SPLIT_THRESHOLD
        && changeValueB < Tx0x2Service.CHANGE_SPLIT_THRESHOLD) {
      // split changes evenly when both changes < treshold
      changeValueA = changeValueTotal / 2L;
      changeValueB = changeValueTotal - changeValueA;
      splitChange = true;
      if (log.isDebugEnabled()) {
        log.debug(
            "computeTx0x2DecoyChanges: changeValueSplit = " + changeValueA + " + " + changeValueB);
      }
    }

    // recalculate preview with final changeValues
    return new Tx0x2Preview(
        tx0PreviewConfig.isTx0x2Cahoots(),
        tx0PreviewConfig.isTx0x2Decoy(),
        nbPremixA,
        nbPremixB,
        changeValueA,
        changeValueB,
        minerFeeA,
        minerFeeB,
        feeValueA,
        feeValueB,
        splitChange);
  }

  private Pair<Collection<UtxoDetail>, Collection<UtxoDetail>> computeSpendFromsStonewall(
      Tx0PreviewConfig tx0PreviewConfig, Tx0Param tx0Param, Tx0Data tx0Data)
      throws NotifiableException {
    NetworkParameters params = config.getNetworkParameters();
    Collection<? extends UtxoDetail> ownSpendFroms = tx0PreviewConfig.getOwnSpendFroms();
    if (ownSpendFroms.size() < 2) {
      if (log.isDebugEnabled()) {
        log.debug("Decoy Tx0x2 is not possible: spendFroms=" + ownSpendFroms.size() + " utxos < 2");
      }
      return null; // 2 utxos min required for stonewall
    }

    // estimate max tx0MinerFee as it would be for regular tx0
    // it will be recalculated later more precisely with real nbPremix (wich maybe lower)
    int nbPremixMax = computeNbPremixMax(tx0Param, tx0Data, ownSpendFroms, false);
    int tx0SizeMax =
        ClientUtils.computeTx0Size(nbPremixMax, true, ownSpendFroms, bipFormatSupplier, params);
    long tx0MinerFeeMax = ClientUtils.computeTx0MinerFee(tx0SizeMax, tx0Param.getFeeTx0(), true);

    // split minerFee + feeValue (feeValue will be zero for lower cascading pools)
    long feeParticipation = (tx0MinerFeeMax + tx0Data.computeFeeValueOrFeeChange()) / 2L;
    long minSpendFrom = feeParticipation + tx0Param.getPremixValue(); // at least 1 must mix each
    boolean initialPool = !tx0PreviewConfig._isCascading();

    // compute pairs
    return computeSpendFromsStonewallPairs(initialPool, ownSpendFroms, minSpendFrom);
  }

  private Pair<Collection<UtxoDetail>, Collection<UtxoDetail>> computeSpendFromsStonewallPairs(
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
    Set<String> utxosHashA = new LinkedHashSet<>();
    Set<String> utxosHashB = new LinkedHashSet<>();
    List<UtxoDetail> utxosA = new LinkedList<>();
    List<UtxoDetail> utxosB = new LinkedList<>();

    for (UtxoDetail spendFrom : spendFroms) {
      String hash = spendFrom.getTxHash();

      // initial pool: check outpoints to avoid spending same prev-tx from both A & B
      // lower pools: does not check outpoints as we are cascading from same prev-tx0
      boolean allowA = !initialPool || !utxosHashB.contains(hash);
      boolean allowB = !initialPool || !utxosHashA.contains(hash);

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
        utxosA.add(spendFrom);
        utxosHashA.add(hash);
        spendFromA += spendFrom.getValueLong();
      } else {
        // choose B
        utxosB.add(spendFrom);
        utxosHashB.add(hash);
        spendFromB += spendFrom.getValueLong();
      }
    }

    if (spendFromA < minSpendFrom || spendFromB < minSpendFrom) {
      if (log.isDebugEnabled()) {
        long[] spendFromValues = spendFroms.stream().mapToLong(s -> s.getValueLong()).toArray();
        log.debug(
            "Decoy Tx0x2 is not possible: spendFroms="
                + Arrays.toString(spendFromValues)
                + ", minSpendFrom="
                + minSpendFrom
                + ", \nutxosA="
                + debugUtxos(utxosA)
                + ", \nutxosB="
                + debugUtxos(utxosB));
      }
      return null;
    }
    return Pair.of(utxosA, utxosB);
  }

  protected String debugUtxos(Collection<? extends UtxoDetail> utxos) {
    return Arrays.toString(
        utxos.stream().map(u -> utxoUtil.utxoToKey(u) + "(" + u.getValueLong() + "sat)").toArray());
  }

  public List<Pool> findCascadingPools(String maxPoolId) {
    Pool highestPool = poolSupplier.findPoolById(maxPoolId);
    return poolSupplier.getPools().stream()
        .filter(pool -> pool.getDenomination() < highestPool.getDenomination())
        .collect(Collectors.toList());
  }

  public void initPools(List<Pool> poolsOrdered) { // sorted by balanceMin desc
    // compute & set tx0PreviewMin
    Tx0PreviewConfig tx0PreviewMinConfig =
        new Tx0PreviewConfig(Tx0FeeTarget.MIN, Tx0FeeTarget.MIN, null);
    final Tx0PreviewResult tx0PreviewsMin = tx0PreviewsMinimal(tx0PreviewMinConfig, poolsOrdered);
    for (Pool pool : poolsOrdered) {
      Tx0Preview tx0PreviewMin = tx0PreviewsMin.getByPoolId(pool.getPoolId()).orElse(null);
      if (tx0PreviewMin != null) {
        pool.setTx0PreviewMin(tx0PreviewMin);
      }
    }

    // compute & set tx0PreviewMax
    Tx0PreviewConfig tx0PreviewMaxConfig =
        new Tx0PreviewConfig(Tx0FeeTarget.BLOCKS_4, Tx0FeeTarget.BLOCKS_4, null);
    final Tx0PreviewResult tx0PreviewsMax = tx0PreviewsMaximal(tx0PreviewMaxConfig, poolsOrdered);
    for (Pool pool : poolsOrdered) {
      Tx0Preview tx0PreviewMax = tx0PreviewsMax.getByPoolId(pool.getPoolId()).orElse(null);
      if (tx0PreviewMax != null) {
        pool.setTx0PreviewMax(tx0PreviewMax);
      }
    }

    // compute & set tx0PreviewMaxSpendValueCascading
    Pool childPool = null;
    List<Pool> poolsAsc = new LinkedList<>(poolsOrdered);
    Collections.reverse(poolsAsc);
    for (Pool pool : poolsAsc) {
      long maxSpendValueCascading = pool.getTx0PreviewMaxSpendValue();
      if (childPool != null) {
        maxSpendValueCascading += childPool.getTx0PreviewMaxSpendValueCascading();
      }
      pool.setTx0PreviewMaxSpendValueCascading(maxSpendValueCascading);
      childPool = pool;
    }
  }
}
