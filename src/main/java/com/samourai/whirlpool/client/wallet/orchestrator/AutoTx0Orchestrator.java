package com.samourai.whirlpool.client.wallet.orchestrator;

import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.exception.AutoTx0InsufficientBalanceException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTx0Orchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(AutoTx0Orchestrator.class);
  private static final int START_DELAY = 10000;

  private WhirlpoolWallet whirlpoolWallet;
  private WhirlpoolWalletConfig config;

  public AutoTx0Orchestrator(WhirlpoolWallet whirlpoolWallet, WhirlpoolWalletConfig config) {
    super(config.getAutoTx0Delay() * 1000, START_DELAY, config.getAutoTx0Delay());
    this.whirlpoolWallet = whirlpoolWallet;
    this.config = config;
  }

  @Override
  protected void runOrchestrator() {
    // try tx0 with automatic selection of best available utxo
    try {
      if (log.isDebugEnabled()) {
        log.debug("AutoTx0: looking for Tx0...");
      }
      autoTx0(); // throws AutoMixInsufficientBalanceException
      setLastRun();
      log.info(" • AutoTx0: Tx0 SUCCESS");
    } catch (AutoTx0InsufficientBalanceException e) {
      // no tx0 possible yet
      onAutoTx0InsufficientBalance(e);
    } catch (Exception e) {
      log.error("", e);
    }
  }

  public Tx0Result autoTx0() throws Exception { // throws AutoMixInsufficientBalanceException
    String poolId = config.getAutoTx0PoolId();
    Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException(
          "No pool found for autoTx0 (autoTx0 = " + (poolId != null ? poolId : "null") + ")");
    }
    Tx0FeeTarget tx0FeeTarget = config.getAutoTx0FeeTarget();
    Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_12;
    Collection<WhirlpoolUtxo> spendFroms =
        findAutoTx0SpendFrom(
            pool, tx0FeeTarget, mixFeeTarget); // throws AutoMixInsufficientBalanceException

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(pool, spendFroms, tx0FeeTarget, mixFeeTarget);
    return whirlpoolWallet.tx0(tx0Config);
  }

  private Collection<WhirlpoolUtxo> findAutoTx0SpendFrom(
      Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget)
      throws Exception { // throws AutoMixInsufficientBalanceException

    // spend TX0 from all non-PREMIX accounts when --auto-tx0-aggregate
    WhirlpoolAccount[] accounts =
        config.isAutoTx0Aggregate()
            ? new WhirlpoolAccount[] {
              WhirlpoolAccount.DEPOSIT, WhirlpoolAccount.POSTMIX, WhirlpoolAccount.BADBANK
            }
            : new WhirlpoolAccount[] {WhirlpoolAccount.DEPOSIT};
    Collection<WhirlpoolUtxo> spendFroms = whirlpoolWallet.getUtxoSupplier().findUtxos(accounts);

    // find ready DEPOSITS
    Collection<WhirlpoolUtxo> readyUtxos =
        spendFroms.stream()
            .filter(
                whirlpoolUtxo -> {
                  WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
                  return utxoStatus != WhirlpoolUtxoStatus.TX0
                      && utxoStatus != WhirlpoolUtxoStatus.TX0_SUCCESS
                      && utxoStatus != WhirlpoolUtxoStatus.MIX_STARTED
                      && utxoStatus != WhirlpoolUtxoStatus.MIX_SUCCESS;
                })
            .collect(Collectors.toList());

    // check tx0 possible
    if (pool.isTx0Possible(BipUtxo.sumValue(readyUtxos))) {
      return readyUtxos;
    } else {
      throw new AutoTx0InsufficientBalanceException();
    }
  }

  private long computeTotalUnconfirmedDeposits() {
    return BipUtxo.sumValue(
        whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT).stream()
            .filter(whirlpoolUtxo -> !whirlpoolUtxo.getConfirmInfo().isConfirmed())
            .collect(Collectors.toList()));
  }

  private void onAutoTx0InsufficientBalance(AutoTx0InsufficientBalanceException e) {
    // wait tx0Delay before retry
    setLastRun();

    Pool autoMixPool = whirlpoolWallet.getPoolSupplier().findPoolById(config.getAutoTx0PoolId());
    long autoMixDenomination = autoMixPool.getDenomination();

    // do we have enough unconfirmed deposit for a tx0?
    long minUnconfirmedDeposit = 2 * autoMixDenomination;
    long totalUnconfirmedDeposit = computeTotalUnconfirmedDeposits();
    if (log.isDebugEnabled()) {
      log.debug(
          "totalUnconfirmedDeposit="
              + ClientUtils.satToBtc(totalUnconfirmedDeposit)
              + ", minUnconfirmedDeposit="
              + ClientUtils.satToBtc(minUnconfirmedDeposit));
    }
    if (totalUnconfirmedDeposit >= minUnconfirmedDeposit) {
      if (log.isDebugEnabled()) {
        log.debug(
            "AutoTx0: no tx0 possible yet, awaiting for "
                + ClientUtils.satToBtc(totalUnconfirmedDeposit)
                + "btc UNCONFIRMED DEPOSIT to confirm");
      }
      return;
    }

    // do we have enough premix to mix at full speed?
    int maxClients = Math.min(config.getMaxClients(), config.getMaxClientsPerPool());
    long minQueueBalance = autoMixDenomination * maxClients;
    long totalPremix = whirlpoolWallet.getUtxoSupplier().getBalance(WhirlpoolAccount.PREMIX);
    if (log.isDebugEnabled()) {
      log.debug(
          "totalPremix="
              + ClientUtils.satToBtc(totalPremix)
              + ", minQueueBalance="
              + ClientUtils.satToBtc(minQueueBalance));
    }
    if (totalPremix >= minQueueBalance) {
      if (log.isDebugEnabled()) {
        log.debug(
            "AutoTx0: no tx0 possible yet, awaiting for "
                + ClientUtils.satToBtc(totalPremix)
                + "btc PREMIX to mix");
      }
      return;
    }

    // notify
    if (config.isAutoTx0Aggregate()) {
      long minAggregateBalance = minQueueBalance * 4; // at least 4 mixs
      long totalBalance = whirlpoolWallet.getUtxoSupplier().getBalanceTotal();
      if (log.isDebugEnabled()) {
        log.debug(
            "totalBalance="
                + ClientUtils.satToBtc(totalBalance)
                + ", minAggregateBalance="
                + ClientUtils.satToBtc(minAggregateBalance));
      }
      if (totalBalance >= minAggregateBalance) {
        // aggregate wallet
        try {
          whirlpoolWallet.aggregate();
        } catch (Exception ee) {
          log.error("aggregate failed", ee);
        }
      } else {
        // not enough funds to continue
        String depositAddress = whirlpoolWallet.getDepositAddress(false);
        String message =
            "insufficient balance for AutoTx0. Please make a deposit to "
                + depositAddress
                + " of at least "
                + ClientUtils.satToBtc(minAggregateBalance)
                + "btc\n";
        whirlpoolWallet.notifyError(message);
      }
    }
  }

  public void onUtxoChanges(WhirlpoolUtxoChanges whirlpoolUtxoChanges) {
    if (!isStarted()) {
      return;
    }

    boolean notify = false;

    // DETECTED
    if (!whirlpoolUtxoChanges.getUtxosAdded().isEmpty()) {
      notify = true;
    }

    if (notify) {
      if (log.isDebugEnabled()) {
        log.debug(" o AutoTx0: checking for tx0...");
        log.debug(
            "Balances: deposit="
                + ClientUtils.satToBtc(
                    whirlpoolWallet.getUtxoSupplier().getBalance(WhirlpoolAccount.DEPOSIT))
                + ", premix="
                + ClientUtils.satToBtc(
                    whirlpoolWallet.getUtxoSupplier().getBalance(WhirlpoolAccount.PREMIX))
                + ", postmix="
                + ClientUtils.satToBtc(
                    whirlpoolWallet.getUtxoSupplier().getBalance(WhirlpoolAccount.POSTMIX))
                + ", total="
                + ClientUtils.satToBtc(whirlpoolWallet.getUtxoSupplier().getBalanceTotal()));
      }
      notifyOrchestrator();
    }
  }
}
