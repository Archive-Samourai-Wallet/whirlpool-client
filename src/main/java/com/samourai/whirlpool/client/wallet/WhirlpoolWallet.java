package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.ricochet.RicochetConfig;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.spend.SpendBuilder;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.PushTxErrorResponseException;
import com.samourai.whirlpool.client.exception.UnconfirmedUtxoException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.DebugUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceWithSweep;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.wallet.orchestrator.AutoTx0Orchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.MixOrchestratorImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.rest.PushTxErrorResponse;
import com.samourai.whirlpool.protocol.rest.PushTxSuccessResponse;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import com.samourai.xmanager.protocol.XManagerService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWallet.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private String walletIdentifier;
  private WhirlpoolWalletConfig config;
  private WalletAggregateService walletAggregateService;
  private PostmixIndexService postmixIndexService;

  private HD_Wallet bip44w;
  private DataSource dataSource;
  private Tx0Service tx0Service;
  private PaynymSupplier paynymSupplier;
  private DataPersister dataPersister;

  protected MixOrchestratorImpl mixOrchestrator;
  private Optional<AutoTx0Orchestrator> autoTx0Orchestrator;
  private MixingStateEditable mixingState;

  protected WhirlpoolWallet(WhirlpoolWallet whirlpoolWallet) throws Exception {
    this(whirlpoolWallet.config, whirlpoolWallet.bip44w, whirlpoolWallet.walletIdentifier);
  }

  public WhirlpoolWallet(WhirlpoolWalletConfig config, byte[] seed, String passphrase)
      throws Exception {
    this(config, seed, passphrase, null);
  }

  public WhirlpoolWallet(
      WhirlpoolWalletConfig config, byte[] seed, String passphrase, String walletIdentifier)
      throws Exception {
    this(
        config,
        HD_WalletFactoryGeneric.getInstance()
            .getBIP44(seed, passphrase != null ? passphrase : "", config.getNetworkParameters()),
        walletIdentifier);
  }

  public WhirlpoolWallet(WhirlpoolWalletConfig config, HD_Wallet bip44w) throws Exception {
    this(config, bip44w, null);
  }

  public WhirlpoolWallet(WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier)
      throws Exception {

    if (walletIdentifier == null) {
      walletIdentifier =
          computeWalletIdentifier(bip44w.getSeed(), bip44w.getPassphrase(), bip44w.getParams());
    }

    // debug whirlpoolWalletConfig
    if (log.isDebugEnabled()) {
      log.debug("New WhirlpoolWallet with config:");
      for (Map.Entry<String, String> entry : config.getConfigInfo().entrySet()) {
        log.debug("[whirlpoolWalletConfig/" + entry.getKey() + "] " + entry.getValue());
      }
      log.debug("[walletIdentifier] " + walletIdentifier);
    }

    // verify config
    config.verify();

    this.walletIdentifier = walletIdentifier;
    this.config = config;

    Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
    this.walletAggregateService =
        new WalletAggregateService(config.getNetworkParameters(), bech32Util, this);
    this.postmixIndexService = new PostmixIndexService(config, bech32Util);

    this.bip44w = bip44w;
    this.dataPersister = null;
    this.dataSource = null;
    this.tx0Service = null; // will be set with datasource
    this.paynymSupplier = null; // will be set with datasource

    this.mixOrchestrator = null;
    this.autoTx0Orchestrator = Optional.empty();
    this.mixingState = new MixingStateEditable(this, false);
  }

  protected static String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  public Tx0Previews tx0Previews(Collection<WhirlpoolUtxo> whirlpoolUtxos, Tx0Config tx0Config)
      throws Exception {
    return tx0Previews(tx0Config, toUnspentOutputs(whirlpoolUtxos));
  }

  public Tx0Previews tx0Previews(Tx0Config tx0Config, Collection<UnspentOutput> whirlpoolUtxos)
      throws Exception {
    return dataSource.getTx0PreviewService().tx0Previews(tx0Config, whirlpoolUtxos);
  }

  public Tx0 tx0(Collection<WhirlpoolUtxo> whirlpoolUtxos, Pool pool, Tx0Config tx0Config)
      throws Exception {

    // verify utxos
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

    // set utxos status
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      whirlpoolUtxo.getUtxoState().setStatus(WhirlpoolUtxoStatus.TX0, true, true);
    }
    try {
      // run
      Tx0 tx0 = tx0(toUnspentOutputs(whirlpoolUtxos), tx0Config, pool);

      // success
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatus(WhirlpoolUtxoStatus.TX0_SUCCESS, true, true);
      }

      return tx0;
    } catch (Exception e) {
      // error
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        String error = NotifiableException.computeNotifiableException(e).getMessage();
        utxoState.setStatusError(WhirlpoolUtxoStatus.TX0_FAILED, error);
      }
      throw e;
    }
  }

  public Tx0 tx0(Collection<UnspentOutput> spendFroms, Tx0Config tx0Config, Pool pool)
      throws Exception {

    // check confirmations
    for (UnspentOutput spendFrom : spendFroms) {
      if (spendFrom.confirmations < config.getTx0MinConfirmations()) {
        log.error("Minimum confirmation(s) for tx0: " + config.getTx0MinConfirmations());
        throw new UnconfirmedUtxoException(spendFrom);
      }
    }

    int initialPremixIndex = getWalletPremix().getIndexHandlerReceive().get();
    int initialChangeIndex = getWalletDeposit().getIndexHandlerChange().get();
    try {
      // run tx0
      Tx0 tx0 = doTx0(spendFroms, tx0Config, pool);

      // notify
      WhirlpoolEventService.getInstance().post(new Tx0Event(this, tx0));

      // refresh new utxos in background
      refreshUtxosDelayAsync().subscribe();
      return tx0;
    } catch (Exception e) {
      // revert index
      getWalletPremix().getIndexHandlerReceive().set(initialPremixIndex, true);
      getWalletDeposit().getIndexHandlerChange().set(initialChangeIndex, true);
      throw e;
    }
  }

  private synchronized Tx0 doTx0(
      Collection<UnspentOutput> spendFroms, Tx0Config tx0Config, Pool pool) throws Exception {
    Exception tx0Exception = null;

    for (int i = 0; i < config.getTx0MaxRetry(); i++) {
      int premixIndex = getWalletPremix().getIndexHandlerReceive().get();
      int changeIndex = getWalletDeposit().getIndexHandlerChange().get();

      Tx0 tx0 =
          tx0Service.tx0(
              spendFroms,
              getWalletDeposit(),
              getWalletPremix(),
              getWalletPostmix(),
              getWalletBadbank(),
              pool,
              tx0Config,
              getUtxoSupplier());

      log.info(
          " • Tx0 result: txid="
              + tx0.getTx().getHashAsString()
              + ", nbPremixs="
              + tx0.getPremixOutputs().size());
      if (log.isDebugEnabled()) {
        log.debug(tx0.getTx().toString());
      }

      // pushT to coordinator retry on address reuse
      try {
        asyncUtil.blockingSingle(pushTx0(tx0));
        return tx0;
      } catch (PushTxErrorResponseException e) {
        PushTxErrorResponse pushTxErrorResponse = e.getPushTxErrorResponse();
        if (pushTxErrorResponse.voutsAddressReuse == null
            || pushTxErrorResponse.voutsAddressReuse.isEmpty()) {
          // not an address-reuse => abort
          throw e;
        }

        // manage premix address reuses
        Collection<Integer> premixOutputIndexs =
            ClientUtils.getOutputIndexs(tx0.getPremixOutputs());
        boolean isPremixReuse =
            pushTxErrorResponse.voutsAddressReuse != null
                && !ClientUtils.intersect(pushTxErrorResponse.voutsAddressReuse, premixOutputIndexs)
                    .isEmpty();

        // manage change address reuses
        Collection<Integer> changeOutputIndexs =
            ClientUtils.getOutputIndexs(tx0.getChangeOutputs());
        boolean isChangeReuse =
            pushTxErrorResponse.voutsAddressReuse != null
                && !ClientUtils.intersect(pushTxErrorResponse.voutsAddressReuse, changeOutputIndexs)
                    .isEmpty();

        // preserve pushTx message and retry with next index
        log.warn(
            "tx0 failed: "
                + e.getMessage()
                + ", pushTxErrorCode="
                + pushTxErrorResponse.pushTxErrorCode
                + ", attempt="
                + i
                + "/"
                + config.getTx0MaxRetry()
                + ", premixIndex="
                + premixIndex
                + ", changeIndex="
                + changeIndex
                + ", isPremixReuse="
                + isPremixReuse
                + ", isChangeReuse="
                + isChangeReuse);

        // revert non-reused indexs for next retry
        if (!isPremixReuse) {
          getWalletPremix().getIndexHandlerReceive().set(premixIndex, true);
        }
        if (!isChangeReuse) {
          getWalletDeposit().getIndexHandlerChange().set(changeIndex, true);
        }

        tx0Exception = new NotifiableException("PushTX0 failed: " + e.getMessage());
      }
    }
    throw tx0Exception;
  }

  protected Observable<PushTxSuccessResponse> pushTx0(Tx0 tx0) throws Exception {
    String tx64 = WhirlpoolProtocol.encodeBytes(tx0.getTx().bitcoinSerialize());
    String poolId = tx0.getPool().getPoolId();
    Tx0PushRequest request = new Tx0PushRequest(tx64, poolId);
    return config.getServerApi().pushTx0(request);
  }

  private Collection<UnspentOutput> toUnspentOutputs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    return whirlpoolUtxos.stream()
        .map(whirlpoolUtxo -> whirlpoolUtxo.getUtxo())
        .collect(Collectors.<UnspentOutput>toList());
  }

  public Tx0Config getTx0Config(Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    Tx0Config tx0Config =
        new Tx0Config(
            getTx0PreviewService(),
            getPoolSupplier().getPools(),
            tx0FeeTarget,
            mixFeeTarget,
            WhirlpoolAccount.DEPOSIT);
    return tx0Config;
  }

  public boolean isStarted() {
    return mixingState.isStarted();
  }

  public void open() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Opening wallet " + walletIdentifier);
    }

    // instanciate data
    this.dataPersister = config.getDataPersisterFactory().createDataPersister(this, bip44w);
    this.dataSource =
        config
            .getDataSourceFactory()
            .createDataSource(
                this,
                bip44w,
                dataPersister.getWalletStateSupplier(),
                dataPersister.getUtxoConfigSupplier());
    this.tx0Service =
        new Tx0Service(config, dataSource.getTx0PreviewService(), config.computeFeeOpReturnImpl());
    this.paynymSupplier = dataSource.getPaynymSupplier();

    // start orchestrators
    int loopDelay = config.getRefreshUtxoDelay() * 1000;
    this.mixOrchestrator =
        new MixOrchestratorImpl(mixingState, loopDelay, config, getPoolSupplier(), this);

    if (config.isAutoTx0()) {
      this.autoTx0Orchestrator = Optional.of(new AutoTx0Orchestrator(this, config));
    } else {
      this.autoTx0Orchestrator = Optional.empty();
    }

    // load initial data (or fail)
    dataPersister.load();

    // forced persist initial data (or fail)
    dataPersister.persist(true);

    // open data
    dataPersister.open();
    dataSource.open();

    // log wallets
    for (BipWallet bipWallet : getWalletSupplier().getWallets()) {
      String nextReceivePath = bipWallet.getNextAddress(false).getPathAddress();
      String nextChangePath = bipWallet.getNextChangeAddress(false).getPathAddress();
      String pub =
          log.isDebugEnabled() ? bipWallet.getPub() : ClientUtils.maskString(bipWallet.getPub());
      log.info(
          " +WALLET "
              + bipWallet.getId()
              + ": account="
              + bipWallet.getAccount()
              + ", bipFormat="
              + bipWallet.getBipFormat().getId()
              + ", receive="
              + nextReceivePath
              + ", change="
              + nextChangePath
              + ", "
              + pub);
    }

    // notify
    WhirlpoolEventService.getInstance().post(new WalletOpenEvent(this));
  }

  public void close() {
    if (log.isDebugEnabled()) {
      log.debug("Closing wallet " + walletIdentifier);
    }
    stop();

    try {
      dataSource.close();
    } catch (Exception e) {
      log.error("", e);
    }

    // persist before exit
    try {
      dataPersister.persist(false);
    } catch (Exception e) {
      log.error("", e);
    }

    try {
      dataPersister.close();
    } catch (Exception e) {
      log.error("", e);
    }

    // notify
    WhirlpoolEventService.getInstance().post(new WalletCloseEvent(this));
  }

  public synchronized Completable startAsync() {
    // check postmix index against coordinator
    return checkPostmixIndexAsync()
        .doOnComplete(
            () -> {
              // start mixing on success
              doStart();
            });
  }

  protected synchronized void doStart() {
    if (isStarted()) {
      if (log.isDebugEnabled()) {
        log.debug("NOT starting WhirlpoolWallet: already started");
      }
      return;
    }
    log.info(" • Starting WhirlpoolWallet");
    mixingState.setStarted(true);

    mixOrchestrator.start(true);
    if (autoTx0Orchestrator.isPresent()) {
      autoTx0Orchestrator.get().start(true);
    }

    // notify startup
    onStartup(getUtxoSupplier().getValue());
  }

  protected void onStartup(UtxoData utxoData) {
    // simulate "firstFetch" of all utxos to get it correctly queued
    WhirlpoolUtxoChanges startupUtxoChanges = new WhirlpoolUtxoChanges(true);
    startupUtxoChanges.getUtxosAdded().addAll(utxoData.getUtxos().values());
    if (mixOrchestrator != null) {
      mixOrchestrator.onUtxoChanges(startupUtxoChanges);
    }
    if (autoTx0Orchestrator.isPresent()) {
      autoTx0Orchestrator.get().onUtxoChanges(startupUtxoChanges);
    }
    WhirlpoolEventService.getInstance().post(new WalletStartEvent(this, utxoData));
  }

  public void onUtxoChanges(UtxoData utxoData) {
    if (isStarted()) {
      if (mixOrchestrator != null) {
        mixOrchestrator.onUtxoChanges(utxoData.getUtxoChanges());
      }
      if (autoTx0Orchestrator.isPresent()) {
        autoTx0Orchestrator.get().onUtxoChanges(utxoData.getUtxoChanges());
      }
    }
    WhirlpoolEventService.getInstance().post(new UtxoChangesEvent(this, utxoData));
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

    if (autoTx0Orchestrator.isPresent()) {
      autoTx0Orchestrator.get().stop();
    }
    mixOrchestrator.stop();

    // notify
    WhirlpoolEventService.getInstance().post(new WalletStopEvent(this));
  }

  public String pushTx(String txHex) throws Exception {
    return dataSource.pushTx(txHex);
  }

  public void mixQueue(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixQueue(whirlpoolUtxo);
  }

  public void mixStop(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    this.mixOrchestrator.mixStop(whirlpoolUtxo, true, false);
  }

  public BipWallet getWalletDeposit() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.DEPOSIT, BIP_FORMAT.SEGWIT_NATIVE);
  }

  public BipWallet getWalletPremix() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.PREMIX, BIP_FORMAT.SEGWIT_NATIVE);
  }

  public BipWallet getWalletPostmix() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.POSTMIX, BIP_FORMAT.SEGWIT_NATIVE);
  }

  public BipWallet getWalletBadbank() {
    return getWalletSupplier().getWallet(WhirlpoolAccount.BADBANK, BIP_FORMAT.SEGWIT_NATIVE);
  }

  public WalletSupplier getWalletSupplier() {
    return dataSource.getWalletSupplier();
  }

  public WalletStateSupplier getWalletStateSupplier() {
    return dataPersister.getWalletStateSupplier();
  }

  public UtxoSupplier getUtxoSupplier() {
    return dataSource.getUtxoSupplier();
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return dataSource.getMinerFeeSupplier();
  }

  public ChainSupplier getChainSupplier() {
    return dataSource.getChainSupplier();
  }

  public PoolSupplier getPoolSupplier() {
    return dataSource.getPoolSupplier();
  }

  public PaynymSupplier getPaynymSupplier() {
    return paynymSupplier;
  }

  public Tx0PreviewService getTx0PreviewService() {
    return dataSource.getTx0PreviewService();
  }

  // used by Sparrow
  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return dataPersister.getUtxoConfigSupplier();
  }

  public void mix(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    mixOrchestrator.mixNow(whirlpoolUtxo);
  }

  public void onMixSuccess(MixParams mixParams, Utxo receiveUtxo) {
    WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    // log
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String logPrefix = " - [MIX] " + (poolId != null ? poolId + " " : "");
    MixDestination destination = whirlpoolUtxo.getUtxoState().getMixProgress().getDestination();
    log.info(
        logPrefix
            + "⣿ WHIRLPOOL SUCCESS ⣿ txid: "
            + receiveUtxo.getHash()
            + ", receiveAddress="
            + destination.getAddress()
            + ", path="
            + destination.getPath()
            + ", type="
            + destination.getType());

    // forward utxoConfig
    int newMixsDone = whirlpoolUtxo.getMixsDone() + 1;
    getUtxoConfigSupplier()
        .setUtxo(receiveUtxo.getHash(), (int) receiveUtxo.getIndex(), newMixsDone);

    // persist
    try {
      dataPersister.persist(true);
    } catch (Exception e) {
      log.error("", e);
    }

    // change Tor identity
    config.getTorClientService().changeIdentity();

    // refresh new utxos in background
    refreshUtxosDelayAsync().subscribe();

    // notify
    WhirlpoolEventService.getInstance().post(new MixSuccessEvent(this, mixParams, receiveUtxo));
  }

  public void onMixFail(MixParams mixParams, MixFailReason failReason, String notifiableError) {
    WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    // log
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String logPrefix = " - [MIX] " + (poolId != null ? poolId + " " : "");

    String message = failReason.getMessage();
    if (notifiableError != null) {
      message += " ; " + notifiableError;
    }
    if (MixFailReason.CANCEL.equals(failReason)) {
      log.info(logPrefix + message);
    } else {
      MixDestination destination = whirlpoolUtxo.getUtxoState().getMixProgress().getDestination();
      String destinationStr =
          (destination != null
              ? ", receiveAddress="
                  + destination.getAddress()
                  + ", path="
                  + destination.getPath()
                  + ", type="
                  + destination.getType()
              : "");
      log.error(logPrefix + "⣿ WHIRLPOOL FAILED ⣿ " + message + destinationStr);
    }

    // notify
    WhirlpoolEventService.getInstance()
        .post(new MixFailEvent(this, mixParams, failReason, notifiableError));

    switch (failReason) {
      case PROTOCOL_MISMATCH:
        // stop mixing on protocol mismatch
        stop();
        break;

      case DISCONNECTED:
      case MIX_FAILED:
      case INPUT_REJECTED:
      case INTERNAL_ERROR:
        // retry later
        try {
          mixQueue(whirlpoolUtxo);
        } catch (Exception e) {
          log.error("", e);
        }

        // check postmixIndex
        checkPostmixIndexAsync()
            .doOnError(
                e -> {
                  // stop mixing on postmixIndex error
                  log.error(e.getMessage());
                  stop();
                })
            .subscribe();
        break;

      case STOP:
      case CANCEL:
      default:
        // not retrying
        break;
    }
  }

  public void onMixProgress(MixParams mixParams) {
    WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    // log
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String logPrefix = " - [MIX] " + (poolId != null ? poolId + " " : "");

    MixStep step = whirlpoolUtxo.getUtxoState().getMixProgress().getMixStep();
    String asciiProgress = renderProgress(step.getProgressPercent());
    log.info(logPrefix + asciiProgress + " " + step + " : " + step.getMessage());

    // notify
    WhirlpoolEventService.getInstance().post(new MixProgressEvent(this, mixParams));
  }

  private String renderProgress(int progressPercent) {
    StringBuilder progress = new StringBuilder();
    for (int i = 0; i < 100; i += 10) {
      progress.append(i < progressPercent ? "▮" : "▯");
    }
    progress.append(" (" + progressPercent + "%)");
    return progress.toString();
  }

  /** Refresh utxos in background after utxosDelay */
  public Completable refreshUtxosDelayAsync() {
    return ClientUtils.sleepUtxosDelayAsync(config.getNetworkParameters())
        .doOnComplete(() -> refreshUtxosAsync().blockingAwait());
  }

  /** Refresh utxos now */
  public Completable refreshUtxosAsync() {
    return asyncUtil.runIOAsyncCompletable(() -> getUtxoSupplier().refresh());
  }

  public synchronized Completable checkPostmixIndexAsync() {
    return asyncUtil.runIOAsyncCompletable(
        () -> {
          if (!config.isPostmixIndexCheck()) {
            // check disabled
            log.warn("postmixIndexCheck is disabled");
            return;
          }

          doCheckPostmixIndex();
        });
  }

  protected void doCheckPostmixIndex() throws Exception {
    try {
      // check
      postmixIndexService.checkPostmixIndex(getWalletPostmix());
    } catch (Exception e) {
      log.error(
          "postmixIndex is desynchronized: " + e.getClass().getSimpleName() + " " + e.getMessage());
      // postmix index is desynchronized
      WhirlpoolEventService.getInstance().post(new PostmixIndexAlreadyUsedEvent(this));
      if (config.isPostmixIndexAutoFix()) {
        // autofix
        try {
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixProgressEvent(this));
          postmixIndexService.fixPostmixIndex(getWalletPostmix());
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixSuccessEvent(this));
        } catch (Exception ee) {
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixFailEvent(this));
          throw ee;
        }
      }
    }
  }

  public void aggregate() throws Exception {
    // aggregate
    boolean success = walletAggregateService.consolidateWallet();

    // reset mixing threads to avoid mixing obsolete consolidated utxos
    mixOrchestrator.stopMixingClients();
    getUtxoSupplier().refresh();

    if (!success) {
      throw new NotifiableException("Aggregate failed (nothing to aggregate?)");
    }
    if (log.isDebugEnabled()) {
      log.debug("Aggregate SUCCESS.");
    }
  }

  public void aggregateTo(String toAddress) throws Exception {
    // aggregate
    aggregate();

    // send to destination
    log.info(" • Moving funds to: " + toAddress);
    walletAggregateService.toAddress(WhirlpoolAccount.DEPOSIT, toAddress);

    // refresh
    getUtxoSupplier().refresh();
  }

  public String getDebug() {
    return DebugUtils.getDebug(this);
  }

  public MixingState getMixingState() {
    return mixingState;
  }

  public String getDepositAddress(boolean increment) {
    return getWalletDeposit().getNextAddress(increment).getAddressString();
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

  protected DataPersister getDataPersister() {
    return dataPersister;
  }

  protected DataSource getDataSource() {
    return dataSource;
  }

  public ISweepBackend getSweepBackend() throws Exception {
    if (!(dataSource instanceof DataSourceWithSweep)) {
      throw new NotifiableException("Sweep not supported by current datasource");
    }
    return ((DataSourceWithSweep) dataSource).getSweepBackend();
  }

  public RicochetConfig newRicochetConfig(
      int feePerB, boolean useTimeLock, WhirlpoolAccount spendAccount) {
    long latestBlock = getChainSupplier().getLatestBlock().height;
    BipWallet bipWalletRicochet = getWalletSupplier().getWallet(BIP_WALLET.RICOCHET_BIP84);
    BipWallet bipWalletChange =
        getWalletSupplier().getWallet(spendAccount, BIP_FORMAT.SEGWIT_NATIVE);
    BIP47Wallet bip47Wallet = new BIP47Wallet(bip44w);
    int bip47WalletOutgoingIdx = 0; // TODO zl !!!
    boolean samouraiFeeViaBIP47 = getPaynymSupplier().getPaynymState().isClaimed();
    String samouraiFeeAddress =
        config.computeXManagerClient().getAddressOrDefault(XManagerService.RICOCHET);
    return new RicochetConfig(
        feePerB,
        samouraiFeeViaBIP47,
        samouraiFeeAddress,
        useTimeLock,
        true,
        latestBlock,
        getUtxoSupplier(),
        config.getBip47Util(),
        bipWalletRicochet,
        bipWalletChange,
        spendAccount,
        bip47Wallet,
        bip47WalletOutgoingIdx);
  }
}
