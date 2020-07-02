package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentResponse.UnspentOutput;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.UnconfirmedUtxoException;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.orchestrator.AutoTx0Orchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.DataOrchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.PersistOrchestrator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.beans.Utxo;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java8.util.Lists;
import java8.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWallet.class);

  private WhirlpoolWalletConfig config;
  private WhirlpoolDataService dataService;
  private Tx0ParamService tx0ParamService;
  private Tx0Service tx0Service;

  private Bech32UtilGeneric bech32Util;

  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final UtxoSupplier utxoSupplier;

  private DataOrchestrator dataOrchestrator;
  private PersistOrchestrator persistOrchestrator;
  protected MixOrchestratorImpl mixOrchestrator;
  private Optional<AutoTx0Orchestrator> autoTx0Orchestrator;

  private MixingStateEditable mixingState;

  protected WhirlpoolWallet(WhirlpoolWallet whirlpoolWallet) {
    this(
        whirlpoolWallet.dataService,
        whirlpoolWallet.tx0ParamService,
        whirlpoolWallet.tx0Service,
        whirlpoolWallet.bech32Util,
        whirlpoolWallet.walletSupplier,
        whirlpoolWallet.utxoConfigSupplier,
        whirlpoolWallet.utxoSupplier);
  }

  public WhirlpoolWallet(
      WhirlpoolDataService dataService,
      Tx0ParamService tx0ParamService,
      Tx0Service tx0Service,
      Bech32UtilGeneric bech32Util,
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      UtxoSupplier utxoSupplier) {
    this.config = dataService.getConfig();
    this.dataService = dataService;
    this.tx0ParamService = tx0ParamService;
    this.tx0Service = tx0Service;

    this.bech32Util = bech32Util;

    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.utxoSupplier = utxoSupplier;

    this.mixingState = new MixingStateEditable(false);

    List<AbstractSupplier> suppliers = new LinkedList<AbstractSupplier>();
    suppliers.addAll(Lists.of(dataService.getSuppliers()));
    suppliers.add(walletSupplier);
    suppliers.add(utxoConfigSupplier);
    suppliers.add(utxoSupplier);

    int dataOrchestratorDelay =
        NumberUtils.min(
            config.getRefreshUtxoDelay(),
            config.getRefreshFeeDelay(),
            config.getRefreshPoolsDelay());
    this.dataOrchestrator = new DataOrchestrator(dataOrchestratorDelay * 1000, suppliers);

    int persistLoopDelay = 10; // persist check each 10s
    this.persistOrchestrator = new PersistOrchestrator(persistLoopDelay * 1000, suppliers);

    int loopDelay = config.getRefreshUtxoDelay() * 1000;
    this.mixOrchestrator = new MixOrchestratorImpl(mixingState, loopDelay, dataService, this);

    if (config.isAutoTx0()) {
      this.autoTx0Orchestrator = Optional.of(new AutoTx0Orchestrator(this, config));
    } else {
      this.autoTx0Orchestrator = Optional.empty();
    }
  }

  private WhirlpoolUtxo findTx0SpendFrom(Pool pool, Tx0FeeTarget tx0FeeTarget)
      throws Exception { // throws EmptyWalletException, UnconfirmedUtxoException
    // random utxo
    List<WhirlpoolUtxo> depositUtxosByPriority =
        new LinkedList<WhirlpoolUtxo>(utxoSupplier.findUtxos(WhirlpoolAccount.DEPOSIT));
    Collections.shuffle(depositUtxosByPriority);

    // find tx0 candidate
    WhirlpoolUtxo unconfirmedUtxo = null;
    for (WhirlpoolUtxo whirlpoolUtxo : depositUtxosByPriority) {
      // check pool
      if (tx0ParamService.isTx0Possible(pool, tx0FeeTarget, whirlpoolUtxo.getUtxo().value)) {
        // check confirmation
        if (whirlpoolUtxo.getUtxo().confirmations >= config.getTx0MinConfirmations()) {

          // set pool
          whirlpoolUtxo.setPoolId(pool.getPoolId());

          // utxo found
          return whirlpoolUtxo;
        } else {
          // found unconfirmed
          unconfirmedUtxo = whirlpoolUtxo;
        }
      }
    }

    // no confirmed utxo found, but we found unconfirmed utxo
    if (unconfirmedUtxo != null) {
      UnspentOutput utxo = unconfirmedUtxo.getUtxo();
      throw new UnconfirmedUtxoException(utxo);
    }

    // no eligible deposit UTXO found
    throw new EmptyWalletException("No UTXO found to spend TX0 from");
  }

  public long computeTx0SpendFromBalanceMin(Pool pool, Tx0FeeTarget tx0FeeTarget) {
    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget);
    return tx0Param.getSpendFromBalanceMin();
  }

  public Tx0 autoTx0() throws Exception { // throws UnconfirmedUtxoException, EmptyWalletException
    String poolId = config.getAutoTx0PoolId();
    Pool pool = getPoolSupplier().findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException(
          "No pool found for autoTx0 (autoTx0 = " + (poolId != null ? poolId : "null") + ")");
    }
    Tx0FeeTarget tx0FeeTarget = config.getAutoTx0FeeTarget();
    WhirlpoolUtxo spendFrom =
        findTx0SpendFrom(
            pool, tx0FeeTarget); // throws UnconfirmedUtxoException, EmptyWalletException

    Tx0Config tx0Config = getTx0Config();
    return tx0(Lists.of(spendFrom), pool, tx0FeeTarget, tx0Config);
  }

  public Tx0Preview tx0Preview(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0Config tx0Config,
      Tx0FeeTarget feeTarget)
      throws Exception {

    Collection<UnspentOutputWithKey> utxos = toUnspentOutputWithKeys(whirlpoolUtxos);
    return tx0Preview(pool, utxos, tx0Config, feeTarget);
  }

  public Tx0Preview tx0Preview(
      Pool pool,
      Collection<UnspentOutputWithKey> spendFroms,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget)
      throws Exception {

    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget);
    return tx0Service.tx0Preview(spendFroms, tx0Config, tx0Param);
  }

  public Tx0 tx0(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0FeeTarget feeTarget,
      Tx0Config tx0Config)
      throws Exception {

    Collection<UnspentOutputWithKey> spendFroms = toUnspentOutputWithKeys(whirlpoolUtxos);

    // verify utxos
    String poolId = pool.getPoolId();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      // check status
      WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
      if (!WhirlpoolUtxoStatus.READY.equals(utxoStatus)
          && !WhirlpoolUtxoStatus.STOP.equals(utxoStatus)
          && !WhirlpoolUtxoStatus.TX0_FAILED.equals(utxoStatus)) {
        throw new NotifiableException("Cannot Tx0: utxoStatus=" + utxoStatus);
      }
    }

    // set utxos
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      // set pool
      if (!poolId.equals(whirlpoolUtxo.getPoolId())) {
        whirlpoolUtxo.setPoolId(poolId);
      }
      // set status
      whirlpoolUtxo.getUtxoState().setStatus(WhirlpoolUtxoStatus.TX0, true);
    }
    try {
      // run
      Tx0 tx0 = tx0(spendFroms, pool, tx0Config, feeTarget);

      // success
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatus(WhirlpoolUtxoStatus.TX0_SUCCESS, true);
      }

      // preserve utxo config
      String tx0Txid = tx0.getTx().getHashAsString();
      WhirlpoolUtxo whirlpoolUtxoSource = whirlpoolUtxos.iterator().next();
      utxoConfigSupplier.forwardUtxoConfig(whirlpoolUtxoSource, tx0Txid);

      return tx0;
    } catch (Exception e) {
      // error
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        String error = NotifiableException.computeNotifiableException(e).getMessage();
        utxoState.setStatus(WhirlpoolUtxoStatus.TX0_FAILED, true, error);
      }
      throw e;
    }
  }

  public Tx0 tx0(
      Collection<UnspentOutputWithKey> spendFroms,
      Pool pool,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget)
      throws Exception {

    // check confirmations
    for (UnspentOutputWithKey spendFrom : spendFroms) {
      if (spendFrom.confirmations < config.getTx0MinConfirmations()) {
        log.error("Minimum confirmation(s) for tx0: " + config.getTx0MinConfirmations());
        throw new UnconfirmedUtxoException(spendFrom);
      }
    }

    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget);

    // run tx0
    int initialPremixIndex = getWalletPremix().getIndexHandler().get();
    try {
      Tx0 tx0 =
          tx0Service.tx0(
              spendFroms,
              getWalletDeposit(),
              getWalletPremix(),
              getWalletPostmix(),
              getWalletBadbank(),
              tx0Config,
              tx0Param);

      log.info(
          " • Tx0 result: txid="
              + tx0.getTx().getHashAsString()
              + ", nbPremixs="
              + tx0.getPremixOutputs().size());
      if (log.isDebugEnabled()) {
        log.debug(tx0.getTx().toString());
      }

      // pushTx
      try {
        config.getBackendApi().pushTx(ClientUtils.getTxHex(tx0.getTx()));
      } catch (Exception e) {
        // preserve pushTx message
        throw new NotifiableException(e.getMessage());
      }

      // refresh new utxos in background
      refreshUtxosDelay();
      return tx0;
    } catch (Exception e) {
      // revert index
      getWalletPremix().getIndexHandler().set(initialPremixIndex);
      throw e;
    }
  }

  private Collection<UnspentOutputWithKey> toUnspentOutputWithKeys(
      Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    Collection<UnspentOutputWithKey> spendFroms = new LinkedList<UnspentOutputWithKey>();

    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      UnspentOutput utxo = whirlpoolUtxo.getUtxo();
      byte[] utxoKey = getWalletDeposit().getAddressAt(utxo).getECKey().getPrivKeyBytes();
      UnspentOutputWithKey spendFrom = new UnspentOutputWithKey(utxo, utxoKey);
      spendFroms.add(spendFrom);
    }
    return spendFroms;
  }

  public Tx0Config getTx0Config() {
    Tx0Config tx0Config = new Tx0Config();
    return tx0Config;
  }

  public boolean isStarted() {
    return mixingState.isStarted();
  }

  public void open() throws Exception {
    // backup on startup
    persistOrchestrator.backup();

    // load initial data (or fail)
    dataOrchestrator.loadInitialData();

    // persist initial data (or fail)
    persistOrchestrator.persistInitialData();

    // keep these orchestrators running (even when mix stopped)
    dataOrchestrator.start(true);
    persistOrchestrator.start(true);
  }

  public void close() {
    persistOrchestrator.stop();
    dataOrchestrator.stop();
  }

  public synchronized void start() {
    if (isStarted()) {
      log.warn("NOT starting WhirlpoolWallet: already started");
      return;
    }
    log.info(" • Starting WhirlpoolWallet");

    this.mixOrchestrator.start(true);
    if (this.autoTx0Orchestrator.isPresent()) {
      this.autoTx0Orchestrator.get().start(true);
    }
    mixingState.setStarted(true);

    // load initial utxos
    WhirlpoolUtxoChanges utxoChanges = new WhirlpoolUtxoChanges(true);
    utxoChanges.getUtxosAdded().addAll(utxoSupplier.getUtxos());
    this._onUtxoChanges(utxoChanges);
  }

  public synchronized void stop() {
    if (!isStarted()) {
      log.warn("NOT stopping WhirlpoolWallet: not started");
      return;
    }
    log.info(" • Stopping WhirlpoolWallet");
    this.mixOrchestrator.stop();
    if (this.autoTx0Orchestrator.isPresent()) {
      this.autoTx0Orchestrator.get().stop();
    }
    // keep other orchestrators running

    mixingState.setStarted(false);
  }

  public void setPool(WhirlpoolUtxo whirlpoolUtxo, String poolId) throws Exception {
    // check pool
    Pool pool = null;
    if (poolId != null) {
      // check pool exists
      pool = getPoolSupplier().findPoolById(poolId);
      if (pool == null) {
        throw new NotifiableException("Pool not found: " + poolId);
      }

      // check pool applicable
      if (!tx0ParamService.isPoolApplicable(pool, whirlpoolUtxo)) {
        throw new NotifiableException("Pool not applicable for utxo: " + poolId);
      }
      poolId = pool.getPoolId();
    }
    // set pool
    whirlpoolUtxo.setPoolId(poolId);
  }

  public void setMixsTarget(WhirlpoolUtxo whirlpoolUtxo, Integer mixsTarget)
      throws NotifiableException {
    if (mixsTarget != null && mixsTarget < 0) {
      throw new NotifiableException("Invalid mixsTarget: " + mixsTarget);
    }
    whirlpoolUtxo.setMixsTarget(mixsTarget);
  }

  public void mixQueue(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixQueue(whirlpoolUtxo);
  }

  public void mixStop(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixStop(whirlpoolUtxo, true, false);
  }

  protected Bip84Wallet getWalletDeposit() {
    return walletSupplier.getWallet(WhirlpoolAccount.DEPOSIT);
  }

  protected Bip84Wallet getWalletPremix() {
    return walletSupplier.getWallet(WhirlpoolAccount.PREMIX);
  }

  protected Bip84Wallet getWalletPostmix() {
    return walletSupplier.getWallet(WhirlpoolAccount.POSTMIX);
  }

  protected Bip84Wallet getWalletBadbank() {
    return walletSupplier.getWallet(WhirlpoolAccount.BADBANK);
  }

  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  public Observable<MixProgress> mix(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    return mixOrchestrator.mixNow(whirlpoolUtxo);
  }

  public void onMixSuccess(WhirlpoolUtxo whirlpoolUtxo, MixSuccess mixSuccess) {
    // preserve utxo config
    Utxo receiveUtxo = mixSuccess.getReceiveUtxo();
    utxoConfigSupplier.forwardUtxoConfig(
        whirlpoolUtxo, receiveUtxo.getHash(), (int) receiveUtxo.getIndex());

    // refresh new utxos in background
    refreshUtxosDelay();
  }

  public void onMixFail(WhirlpoolUtxo whirlpoolUtxo, MixFailReason reason, String notifiableError) {
    switch (reason) {
      case PROTOCOL_MISMATCH:
        // stop mixing on protocol mismatch
        log.error("onMixFail(" + reason + "): stopping mixing");
        stop();
        break;

      case DISCONNECTED:
      case MIX_FAILED:
        // is utxo still mixable?
        if (whirlpoolUtxo.getPoolId() == null) {
          // utxo was spent in the meantime
          log.warn(
              "onMixFail(" + reason + "): not retrying because UTXO was spent: " + whirlpoolUtxo);
          return;
        }

        // retry later
        log.info("onMixFail(" + reason + "): will retry later");
        try {
          mixQueue(whirlpoolUtxo);
        } catch (Exception e) {
          log.error("", e);
        }
        break;

      case INPUT_REJECTED:
      case INTERNAL_ERROR:
      case STOP:
      case CANCEL:
        // not retrying
        log.warn("onMixFail(" + reason + "): won't retry");
        break;

      default:
        // not retrying
        log.warn("onMixFail(" + reason + "): unknown reason");
        break;
    }
  }

  /** Refresh utxos after utxosDelay (in a new thread). */
  public Observable<Optional<Void>> refreshUtxosDelay() {
    return ClientUtils.sleepUtxosDelay(
        config.getNetworkParameters(),
        new Runnable() {
          @Override
          public void run() {
            refreshUtxos(true);
          }
        });
  }

  /** Refresh utxos now. */
  public void refreshUtxos(boolean waitComplete) {
    utxoSupplier.expire();
    dataOrchestrator.notifyOrchestrator();
    if (waitComplete) {
      // TODO wait for orchestrator to complete
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
      }
    }
  }

  public MixingState getMixingState() {
    return mixingState;
  }

  public String getDepositAddress(boolean increment) {
    return bech32Util.toBech32(
        getWalletDeposit().getNextAddress(increment), config.getNetworkParameters());
  }

  public void _onUtxoChanges(WhirlpoolUtxoChanges whirlpoolUtxoChanges) {
    // notify
    mixOrchestrator.onUtxoChanges(whirlpoolUtxoChanges);
    if (autoTx0Orchestrator.isPresent()) {
      autoTx0Orchestrator.get().onUtxoChanges(whirlpoolUtxoChanges);
    }
  }

  public void onEmptyWalletException(EmptyWalletException e) {
    String depositAddress = getDepositAddress(false);
    String message = e.getMessageDeposit(depositAddress);
    notifyError(message);
  }

  public void notifyError(String message) {
    log.error(message);
  }

  public boolean hasMoreMixableOrUnconfirmed() {
    return mixOrchestrator.hasMoreMixableOrUnconfirmed();
  }

  public boolean hasMoreMixingThreadAvailable(String poolId) {
    return mixOrchestrator.hasMoreMixingThreadAvailable(poolId);
  }

  public String getZpubDeposit() {
    return getWalletDeposit().getZpub();
  }

  public String getZpubPremix() {
    return getWalletPremix().getZpub();
  }

  public String getZpubPostmix() {
    return getWalletPostmix().getZpub();
  }

  public String getZpubBadBank() {
    return getWalletBadbank().getZpub();
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return dataService.getMinerFeeSupplier();
  }

  public PoolSupplier getPoolSupplier() {
    return dataService.getPoolSupplier();
  }

  public WhirlpoolWalletConfig getConfig() {
    return config;
  }
}
