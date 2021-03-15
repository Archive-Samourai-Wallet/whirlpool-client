package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.HD_Address;
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
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.wallet.orchestrator.AutoTx0Orchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.DataOrchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.PersistOrchestrator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.rest.CheckOutputRequest;
import com.samourai.whirlpool.protocol.rest.Tx0NotifyRequest;
import io.reactivex.Observable;
import java.util.*;
import java8.util.Lists;
import java8.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWallet.class);
  private static final int FETCH_TXS_PER_PAGE = 300;
  private static final int CHECK_POSTMIX_INDEX_MAX = 30;

  private WhirlpoolWalletConfig config;
  private Tx0ParamService tx0ParamService;
  private Tx0Service tx0Service;

  private Bech32UtilGeneric bech32Util;

  private final WalletSupplier walletSupplier;
  private final PoolSupplier poolSupplier;
  private final WalletDataSupplier walletDataSupplier;

  private DataOrchestrator dataOrchestrator;
  private PersistOrchestrator persistOrchestrator;
  protected MixOrchestratorImpl mixOrchestrator;
  private Optional<AutoTx0Orchestrator> autoTx0Orchestrator;

  private MixingStateEditable mixingState;

  protected WhirlpoolWallet(WhirlpoolWallet whirlpoolWallet) {
    this(
        whirlpoolWallet.config,
        whirlpoolWallet.tx0ParamService,
        whirlpoolWallet.tx0Service,
        whirlpoolWallet.bech32Util,
        whirlpoolWallet.walletSupplier,
        whirlpoolWallet.poolSupplier,
        whirlpoolWallet.walletDataSupplier);
  }

  public WhirlpoolWallet(
      WhirlpoolWalletConfig config,
      Tx0ParamService tx0ParamService,
      Tx0Service tx0Service,
      Bech32UtilGeneric bech32Util,
      WalletSupplier walletSupplier,
      PoolSupplier poolSupplier,
      WalletDataSupplier walletDataSupplier) {
    this.config = config;
    this.tx0ParamService = tx0ParamService;
    this.tx0Service = tx0Service;

    this.bech32Util = bech32Util;

    this.walletSupplier = walletSupplier;
    this.poolSupplier = poolSupplier;
    this.walletDataSupplier = walletDataSupplier;

    this.mixingState = new MixingStateEditable(false);

    List<AbstractSupplier> suppliers = new LinkedList<AbstractSupplier>();
    suppliers.add(poolSupplier);
    suppliers.add(walletSupplier.getWalletStateSupplier());
    suppliers.add(getUtxoConfigSupplier());
    suppliers.add(walletDataSupplier);

    int dataOrchestratorDelay =
        NumberUtils.min(config.getRefreshUtxoDelay(), config.getRefreshPoolsDelay());
    this.dataOrchestrator = new DataOrchestrator(dataOrchestratorDelay * 1000, suppliers);

    int persistLoopDelay = 10; // persist check each 10s
    this.persistOrchestrator = new PersistOrchestrator(persistLoopDelay * 1000, suppliers);

    int loopDelay = config.getRefreshUtxoDelay() * 1000;
    this.mixOrchestrator =
        new MixOrchestratorImpl(mixingState, loopDelay, config, getPoolSupplier(), this);

    if (config.isAutoTx0()) {
      this.autoTx0Orchestrator = Optional.of(new AutoTx0Orchestrator(this, config));
    } else {
      this.autoTx0Orchestrator = Optional.empty();
    }
  }

  private WhirlpoolUtxo findTx0SpendFrom(
      Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget)
      throws Exception { // throws EmptyWalletException, UnconfirmedUtxoException
    // random utxo
    List<WhirlpoolUtxo> depositUtxosByPriority =
        new LinkedList<WhirlpoolUtxo>(getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT));
    Collections.shuffle(depositUtxosByPriority);

    // find tx0 candidate
    WhirlpoolUtxo unconfirmedUtxo = null;
    for (WhirlpoolUtxo whirlpoolUtxo : depositUtxosByPriority) {
      // check pool
      if (tx0ParamService.isTx0Possible(
          pool, tx0FeeTarget, mixFeeTarget, whirlpoolUtxo.getUtxo().value)) {
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

  public long computeTx0SpendFromBalanceMin(
      Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Param.getSpendFromBalanceMin();
  }

  public Tx0 autoTx0() throws Exception { // throws UnconfirmedUtxoException, EmptyWalletException
    String poolId = config.getAutoTx0PoolId();
    Pool pool = poolSupplier.findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException(
          "No pool found for autoTx0 (autoTx0 = " + (poolId != null ? poolId : "null") + ")");
    }
    Tx0FeeTarget tx0FeeTarget = config.getAutoTx0FeeTarget();
    Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_12;
    WhirlpoolUtxo spendFrom =
        findTx0SpendFrom(
            pool,
            tx0FeeTarget,
            mixFeeTarget); // throws UnconfirmedUtxoException, EmptyWalletException

    Tx0Config tx0Config = getTx0Config();
    return tx0(Lists.of(spendFrom), pool, tx0FeeTarget, mixFeeTarget, tx0Config);
  }

  public Tx0Preview tx0Preview(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {

    Collection<UnspentOutputWithKey> utxos = toUnspentOutputWithKeys(whirlpoolUtxos);
    return tx0Preview(pool, utxos, tx0Config, tx0FeeTarget, mixFeeTarget);
  }

  public Tx0Preview tx0Preview(
      Pool pool,
      Collection<UnspentOutputWithKey> spendFroms,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {

    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Service.tx0Preview(spendFroms, tx0Config, tx0Param);
  }

  public Tx0 tx0(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
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
      Tx0 tx0 = tx0(spendFroms, pool, tx0Config, tx0FeeTarget, mixFeeTarget);

      // success
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatus(WhirlpoolUtxoStatus.TX0_SUCCESS, true);
      }

      // preserve utxo config
      String tx0Txid = tx0.getTx().getHashAsString();
      WhirlpoolUtxo whirlpoolUtxoSource = whirlpoolUtxos.iterator().next();
      getUtxoConfigSupplier().forwardUtxoConfig(whirlpoolUtxoSource, tx0Txid);

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
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {

    // check confirmations
    for (UnspentOutputWithKey spendFrom : spendFroms) {
      if (spendFrom.confirmations < config.getTx0MinConfirmations()) {
        log.error("Minimum confirmation(s) for tx0: " + config.getTx0MinConfirmations());
        throw new UnconfirmedUtxoException(spendFrom);
      }
    }

    Tx0Param tx0Param = tx0ParamService.getTx0Param(pool, tx0FeeTarget, mixFeeTarget);

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

      // notify coordinator (not mandatory)
      notifyTx0(tx0.getTx().getHashAsString(), pool.getPoolId());

      // refresh new utxos in background
      refreshUtxosDelay();
      return tx0;
    } catch (Exception e) {
      // revert index
      getWalletPremix().getIndexHandler().set(initialPremixIndex);
      throw e;
    }
  }

  private void notifyTx0(final String txid, final String poolId) {
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Tx0NotifyRequest tx0NotifyRequest = new Tx0NotifyRequest(txid, poolId);
                  config.getServerApi().tx0Notify(tx0NotifyRequest).blockingSingle().get();
                } catch (Exception e) {
                  log.warn("notifyTx0 failed", e);
                }
              }
            },
            "notifyTx0")
        .start();
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

    // resync on first run
    WalletStateSupplier walletStateSupplier = walletSupplier.getWalletStateSupplier();
    if (config.isResyncOnFirstRun() && !walletStateSupplier.isSynced()) {
      // only resync if we have remixable utxos
      if (!getUtxoSupplier().findUtxos(true, WhirlpoolAccount.POSTMIX).isEmpty()) {
        if (log.isDebugEnabled()) {
          log.debug("First run => resync");
        }
        try {
          resync();
        } catch (Exception e) {
          log.error("", e);
        }
      }
      walletStateSupplier.setSynced(true);
    }

    // check postmix index against coordinator
    checkPostmixIndex();
  }

  public void close() {
    persistOrchestrator.stop();
    dataOrchestrator.stop();
  }

  public synchronized void start() {
    if (isStarted()) {
      if (log.isDebugEnabled()) {
        log.debug("NOT starting WhirlpoolWallet: already started");
      }
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
    utxoChanges.getUtxosAdded().addAll(getUtxoSupplier().getUtxos());
    this._onUtxoChanges(utxoChanges);
  }

  public synchronized void stop() {
    if (!isStarted()) {
      if (log.isDebugEnabled()) {
        log.debug("NOT stopping WhirlpoolWallet: not started");
      }
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
    return walletDataSupplier.getUtxoSupplier();
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return walletDataSupplier.getMinerFeeSupplier();
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  protected UtxoConfigSupplier getUtxoConfigSupplier() {
    return walletDataSupplier.getUtxoConfigSupplier();
  }

  public Observable<MixProgress> mix(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    return mixOrchestrator.mixNow(whirlpoolUtxo);
  }

  public void onMixSuccess(WhirlpoolUtxo whirlpoolUtxo, MixSuccess mixSuccess) {
    // preserve utxo config
    Utxo receiveUtxo = mixSuccess.getReceiveUtxo();
    getUtxoConfigSupplier()
        .forwardUtxoConfig(whirlpoolUtxo, receiveUtxo.getHash(), (int) receiveUtxo.getIndex());

    // change Tor identity
    config.getTorClientService().changeIdentity();

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
    getUtxoSupplier().expire();
    dataOrchestrator.notifyOrchestrator();
    if (waitComplete) {
      // TODO wait for orchestrator to complete
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
      }
    }
  }

  public void resync() throws Exception {
    Collection<WhirlpoolUtxo> whirlpoolUtxos =
        getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX);

    log.info("Resynchronizing mix counters...");

    Map<String, TxsResponse.Tx> txs = fetchTxsPostmix();

    int fixedUtxos = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      int mixsDone = recountMixsDone(whirlpoolUtxo, txs);
      if (mixsDone != whirlpoolUtxo.getMixsDone()) {
        log.info(
            "Fixed "
                + whirlpoolUtxo.getUtxo().tx_hash
                + ":"
                + whirlpoolUtxo.getUtxo().tx_output_n
                + ": "
                + whirlpoolUtxo.getMixsDone()
                + " => "
                + mixsDone);
        whirlpoolUtxo.setMixsDone(mixsDone);
        fixedUtxos++;
      }
    }
    log.info("Resync success: " + fixedUtxos + "/" + whirlpoolUtxos.size() + " utxos updated.");
  }

  private int recountMixsDone(WhirlpoolUtxo whirlpoolUtxo, Map<String, TxsResponse.Tx> txs) {
    int mixsDone = 0;

    String txid = whirlpoolUtxo.getUtxo().tx_hash;
    while (true) {
      TxsResponse.Tx tx = txs.get(txid);
      mixsDone++;
      if (tx == null || tx.inputs == null || tx.inputs.length == 0) {
        return mixsDone;
      }
      txid = tx.inputs[0].prev_out.txid;
    }
  }

  private Map<String, TxsResponse.Tx> fetchTxsPostmix() throws Exception {
    Map<String, TxsResponse.Tx> txs = new LinkedHashMap<String, TxsResponse.Tx>();
    int page = -1;
    String[] zpubs = new String[] {getWalletPostmix().getZpub()};
    TxsResponse txsResponse;
    do {
      page++;
      txsResponse = config.getBackendApi().fetchTxs(zpubs, page, FETCH_TXS_PER_PAGE);
      if (txsResponse.txs != null) {
        for (TxsResponse.Tx tx : txsResponse.txs) {
          txs.put(tx.hash, tx);
        }
      }
      log.info("Resync: fetching postmix history... " + txs.size() + "/" + txsResponse.n_tx);
    } while ((page * FETCH_TXS_PER_PAGE) < txsResponse.n_tx);
    return txs;
  }

  private void checkPostmixIndex() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("checking postmixIndex...");
    }
    IIndexHandler postmixIndexHandler = getWalletPostmix().getIndexHandler();
    int initialPostmixIndex =
        ClientUtils.computeNextReceiveAddressIndex(postmixIndexHandler, config.isMobile());
    int postmixIndex = initialPostmixIndex;
    while (true) {
      try {
        // check next output
        checkPostmixIndex(postmixIndex).blockingSingle().get();

        // success!
        if (postmixIndex != initialPostmixIndex) {
          if (log.isDebugEnabled()) {
            log.debug("fixing postmixIndex: " + initialPostmixIndex + " -> " + postmixIndex);
          }
          postmixIndexHandler.confirmUnconfirmed(postmixIndex);
        }
        return;
      } catch (RuntimeException runtimeException) { // blockingGet wraps errors in RuntimeException
        Throwable e = runtimeException.getCause();
        String restErrorMessage = ClientUtils.parseRestErrorMessage(e);
        if (restErrorMessage != null && "Output already registered".equals(restErrorMessage)) {
          log.warn("postmixIndex already used: " + postmixIndex);

          // try second next index
          ClientUtils.computeNextReceiveAddressIndex(postmixIndexHandler, config.isMobile());
          postmixIndex =
              ClientUtils.computeNextReceiveAddressIndex(postmixIndexHandler, config.isMobile());

          // avoid flooding
          try {
            Thread.sleep(500);
          } catch (InterruptedException ee) {
          }
        } else {
          throw new Exception(
              "checkPostmixIndex failed when checking postmixIndex=" + postmixIndex, e);
        }
        if ((postmixIndex - initialPostmixIndex) > CHECK_POSTMIX_INDEX_MAX) {
          throw new NotifiableException(
              "PostmixIndex error - please resync your wallet or contact support");
        }
      }
    }
  }

  private Observable<Optional<String>> checkPostmixIndex(int postmixIndex) throws Exception {
    HD_Address hdAddress = getWalletPostmix().getAddressAt(Bip84Wallet.CHAIN_RECEIVE, postmixIndex);
    String outputAddress = bech32Util.toBech32(hdAddress, config.getNetworkParameters());
    String signature = hdAddress.getECKey().signMessage(outputAddress);
    CheckOutputRequest checkOutputRequest = new CheckOutputRequest(outputAddress, signature);
    return config.getServerApi().checkOutput(checkOutputRequest);
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

  public boolean hasMoreMixingThreadAvailable(String poolId, boolean liquidity) {
    return mixOrchestrator.hasMoreMixingThreadAvailable(poolId, liquidity);
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

  public WhirlpoolWalletConfig getConfig() {
    return config;
  }
}
