package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.spend.SpendBuilder;
import com.samourai.whirlpool.client.event.Tx0Event;
import com.samourai.whirlpool.client.event.UtxosChangeEvent;
import com.samourai.whirlpool.client.event.WalletStartEvent;
import com.samourai.whirlpool.client.event.WalletStopEvent;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.UnconfirmedUtxoException;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
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
import java8.util.Optional;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWallet.class);
  private static final int FETCH_TXS_PER_PAGE = 300;
  private static final int CHECK_POSTMIX_INDEX_MAX = 30;

  private String walletIdentifier;
  private WhirlpoolWalletConfig config;
  private Tx0Service tx0Service;
  private WalletAggregateService walletAggregateService;

  private Bech32UtilGeneric bech32Util;

  private final WalletDataSupplier walletDataSupplier;

  private DataOrchestrator dataOrchestrator;
  private PersistOrchestrator persistOrchestrator;
  protected MixOrchestratorImpl mixOrchestrator;
  private Optional<AutoTx0Orchestrator> autoTx0Orchestrator;

  private MixingStateEditable mixingState;

  protected WhirlpoolWallet(WhirlpoolWallet whirlpoolWallet) {
    this(
        whirlpoolWallet.walletIdentifier,
        whirlpoolWallet.config,
        whirlpoolWallet.tx0Service,
        whirlpoolWallet.walletAggregateService,
        whirlpoolWallet.bech32Util,
        whirlpoolWallet.walletDataSupplier);
  }

  public WhirlpoolWallet(
      String walletIdentifier,
      WhirlpoolWalletConfig config,
      Tx0Service tx0Service,
      WalletAggregateService walletAggregateService,
      Bech32UtilGeneric bech32Util,
      WalletDataSupplier walletDataSupplier) {
    this.walletIdentifier = walletIdentifier;
    this.config = config;
    this.tx0Service = tx0Service;
    this.walletAggregateService = walletAggregateService;

    this.bech32Util = bech32Util;

    this.walletDataSupplier = walletDataSupplier;

    this.mixingState = new MixingStateEditable(false);
  }

  public long computeTx0SpendFromBalanceMin(
      Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    Tx0Param tx0Param = getTx0ParamService().getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Param.getSpendFromBalanceMin();
  }

  public Tx0Preview tx0Preview(
      Pool pool,
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {
    Tx0Param tx0Param = getTx0ParamService().getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Service.tx0Preview(toUnspentOutputs(whirlpoolUtxos), tx0Config, tx0Param);
  }

  public Tx0 tx0(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Tx0Config tx0Config)
      throws Exception {

    // verify utxos
    String poolId = pool.getPoolId();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      // check status
      WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
      if (!WhirlpoolUtxoStatus.READY.equals(utxoStatus)
          && !WhirlpoolUtxoStatus.STOP.equals(utxoStatus)
          && !WhirlpoolUtxoStatus.TX0_FAILED.equals(utxoStatus)
          // when aggregating
          && !WhirlpoolUtxoStatus.MIX_QUEUE.equals(utxoStatus)
          && !WhirlpoolUtxoStatus.MIX_FAILED.equals(utxoStatus)) {
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
      Tx0 tx0 = tx0(whirlpoolUtxos, pool, tx0Config, tx0FeeTarget, mixFeeTarget);

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
      Collection<WhirlpoolUtxo> spendFroms,
      Pool pool,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {

    // check confirmations
    for (WhirlpoolUtxo spendFrom : spendFroms) {
      int latestBlockHeight = getChainSupplier().getLatestBlockHeight();
      int confirmations = spendFrom.computeConfirmations(latestBlockHeight);
      if (confirmations < config.getTx0MinConfirmations()) {
        log.error("Minimum confirmation(s) for tx0: " + config.getTx0MinConfirmations());
        throw new UnconfirmedUtxoException(spendFrom);
      }
    }

    Tx0Param tx0Param = getTx0ParamService().getTx0Param(pool, tx0FeeTarget, mixFeeTarget);

    // run tx0
    int initialPremixIndex = getWalletPremix().getIndexHandler().get();
    try {
      Tx0 tx0 =
          tx0Service.tx0(
              toUnspentOutputs(spendFroms),
              getWalletDeposit(),
              getWalletPremix(),
              getWalletPostmix(),
              getWalletBadbank(),
              tx0Config,
              tx0Param,
              getUtxoSupplier());

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

      // notify
      WhirlpoolEventService.getInstance().post(new Tx0Event(tx0));
      notifyCoordinatorTx0(tx0.getTx().getHashAsString(), pool.getPoolId());

      // refresh new utxos in background
      refreshUtxosDelay();
      return tx0;
    } catch (Exception e) {
      // revert index
      getWalletPremix().getIndexHandler().set(initialPremixIndex);
      throw e;
    }
  }

  private Collection<UnspentOutput> toUnspentOutputs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    return StreamSupport.stream(whirlpoolUtxos)
        .map(
            new Function<WhirlpoolUtxo, UnspentOutput>() {
              @Override
              public UnspentOutput apply(WhirlpoolUtxo whirlpoolUtxo) {
                return whirlpoolUtxo.getUtxo();
              }
            })
        .collect(Collectors.<UnspentOutput>toList());
  }

  private void notifyCoordinatorTx0(final String txid, final String poolId) {
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Tx0NotifyRequest tx0NotifyRequest = new Tx0NotifyRequest(txid, poolId);
                  config.getServerApi().tx0Notify(tx0NotifyRequest).blockingSingle().get();
                } catch (Exception e) {
                  // ignore failures
                  log.warn("notifyCoordinatorTx0 failed", e);
                }
              }
            },
            "notifyCoordinatorTx0")
        .start();
  }

  public Tx0Config getTx0Config() {
    Tx0Config tx0Config = new Tx0Config();
    return tx0Config;
  }

  public boolean isStarted() {
    return mixingState.isStarted();
  }

  protected void open() throws Exception {
    // instanciate orchestrators
    List<AbstractSupplier> suppliers = new LinkedList<AbstractSupplier>();
    suppliers.add(walletDataSupplier.getPoolSupplier());
    suppliers.add(walletDataSupplier.getWalletSupplier().getWalletStateSupplier());
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
      this.autoTx0Orchestrator =
          Optional.of(new AutoTx0Orchestrator(this, config, getTx0ParamService()));
    } else {
      this.autoTx0Orchestrator = Optional.empty();
    }

    // load initial data (or fail)
    dataOrchestrator.loadInitialData();

    // persist initial data (or fail)
    persistOrchestrator.persistInitialData();

    // keep these orchestrators running (even when mix stopped)
    dataOrchestrator.start(true);
    persistOrchestrator.start(true);

    // resync on first run
    WalletStateSupplier walletStateSupplier = getWalletSupplier().getWalletStateSupplier();
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

  protected void close() {
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
    mixingState.setStarted(true);

    // notify startup
    UtxoData utxoData = getUtxoSupplier().getValue();
    WhirlpoolEventService.getInstance().post(new WalletStartEvent(utxoData));
    WhirlpoolEventService.getInstance().post(new UtxosChangeEvent(utxoData));
  }

  public synchronized void stop() {
    if (!isStarted()) {
      if (log.isDebugEnabled()) {
        log.debug("NOT stopping WhirlpoolWallet: not started");
      }
      return;
    }
    log.info(" • Stopping WhirlpoolWallet");

    mixingState.setStarted(false);

    // notify stop
    WhirlpoolEventService.getInstance().post(new WalletStopEvent());
  }

  public void mixQueue(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixQueue(whirlpoolUtxo);
  }

  public void mixStop(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixStop(whirlpoolUtxo, true, false);
  }

  public BipWalletAndAddressType getWalletDeposit() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_NATIVE);
  }

  public BipWalletAndAddressType getWalletPremix() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.PREMIX, AddressType.SEGWIT_NATIVE);
  }

  public BipWalletAndAddressType getWalletPostmix() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.POSTMIX, AddressType.SEGWIT_NATIVE);
  }

  public BipWalletAndAddressType getWalletBadbank() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.BADBANK, AddressType.SEGWIT_NATIVE);
  }

  public WalletSupplier getWalletSupplier() {
    return walletDataSupplier.getWalletSupplier();
  }

  public UtxoSupplier getUtxoSupplier() {
    return walletDataSupplier.getUtxoSupplier();
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return walletDataSupplier.getMinerFeeSupplier();
  }

  public ChainSupplier getChainSupplier() {
    return walletDataSupplier.getChainSupplier();
  }

  public PoolSupplier getPoolSupplier() {
    return walletDataSupplier.getPoolSupplier();
  }

  private Tx0ParamService getTx0ParamService() {
    return walletDataSupplier.getTx0ParamService();
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
            refreshUtxos();
          }
        });
  }

  /** Refresh utxos now. */
  public void refreshUtxos() {
    try {
      walletDataSupplier.expireAndReload();
    } catch (Exception e) {
      log.error("", e);
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
    String[] zpubs = new String[] {getWalletPostmix().getPub()};
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
    HD_Address hdAddress = getWalletPostmix().getAddressAt(Chain.RECEIVE.getIndex(), postmixIndex);
    String outputAddress = bech32Util.toBech32(hdAddress, config.getNetworkParameters());
    String signature = hdAddress.getECKey().signMessage(outputAddress);
    CheckOutputRequest checkOutputRequest = new CheckOutputRequest(outputAddress, signature);
    return config.getServerApi().checkOutput(checkOutputRequest);
  }

  public void aggregate() throws Exception {
    // aggregate
    boolean success = walletAggregateService.consolidateWallet(this, getUtxoSupplier());

    // reset mixing threads to avoid mixing obsolete consolidated utxos
    mixOrchestrator.stopMixingClients();
    getUtxoSupplier().expire();

    if (!success) {
      throw new NotifiableException("AutoAggregatePostmix failed (nothing to aggregate?)");
    }
    if (log.isDebugEnabled()) {
      log.debug("AutoAggregatePostmix SUCCESS.");
    }
  }

  public void aggregateTo(String toAddress) throws Exception {
    // aggregate
    aggregate();

    // send to destination
    log.info(" • Moving funds to: " + toAddress);
    walletAggregateService.toAddress(getWalletDeposit(), toAddress, this);

    // expire
    getUtxoSupplier().expire();
  }

  public MixingState getMixingState() {
    return mixingState;
  }

  public String getDepositAddress(boolean increment) {
    return bech32Util.toBech32(
        getWalletDeposit().getNextAddress(increment), config.getNetworkParameters());
  }

  public void notifyError(String message) {
    log.error(message);
  }

  public SpendBuilder getSpendBuilder(Runnable restoreChangeIndexes) {
    return new SpendBuilder(config.getNetworkParameters(), getUtxoSupplier(), restoreChangeIndexes);
  }

  public String getZpubDeposit() {
    return getWalletDeposit().getPub();
  }

  public String getZpubPremix() {
    return getWalletPremix().getPub();
  }

  public String getZpubPostmix() {
    return getWalletPostmix().getPub();
  }

  public String getZpubBadBank() {
    return getWalletBadbank().getPub();
  }

  public String getWalletIdentifier() {
    return walletIdentifier;
  }

  public WhirlpoolWalletConfig getConfig() {
    return config;
  }
}
