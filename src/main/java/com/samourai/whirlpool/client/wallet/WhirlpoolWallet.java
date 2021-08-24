package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.*;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.spend.SpendBuilder;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.UnconfirmedUtxoException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.wallet.orchestrator.AutoTx0Orchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.MixOrchestratorImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.rest.CheckOutputRequest;
import com.samourai.whirlpool.protocol.rest.Tx0NotifyRequest;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.Map;
import java8.util.Optional;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWallet.class);
  private static final int CHECK_POSTMIX_INDEX_MAX = 30;

  private Bech32UtilGeneric bech32Util;

  private String walletIdentifier;
  private WhirlpoolWalletConfig config;
  private Tx0Service tx0Service;
  private WalletAggregateService walletAggregateService;

  private HD_Wallet bip44w;
  private DataSource dataSource;
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

    this.bech32Util = Bech32UtilGeneric.getInstance();

    this.walletIdentifier = walletIdentifier;
    this.config = config;
    this.tx0Service = new Tx0Service(config);
    this.walletAggregateService =
        new WalletAggregateService(config.getNetworkParameters(), bech32Util, this);

    this.bip44w = bip44w;
    this.dataPersister = null;
    this.dataSource = null;

    this.mixOrchestrator = null;
    this.autoTx0Orchestrator = Optional.empty();
    this.mixingState = new MixingStateEditable(this, false);
  }

  protected static String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
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
    return tx0Preview(
        pool, tx0Config, toUnspentOutputs(whirlpoolUtxos), tx0FeeTarget, mixFeeTarget);
  }

  public Tx0Preview tx0Preview(
      Pool pool,
      Tx0Config tx0Config,
      Collection<UnspentOutput> whirlpoolUtxos,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {
    Tx0Param tx0Param = getTx0ParamService().getTx0Param(pool, tx0FeeTarget, mixFeeTarget);
    return tx0Service.tx0Preview(whirlpoolUtxos, tx0Config, tx0Param);
  }

  public Tx0 tx0(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget,
      Tx0Config tx0Config)
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

  public Tx0 tx0(
      Collection<WhirlpoolUtxo> spendFroms,
      Pool pool,
      Tx0Config tx0Config,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget)
      throws Exception {

    // check confirmations
    for (WhirlpoolUtxo spendFrom : spendFroms) {
      int latestBlockHeight = getChainSupplier().getLatestBlock().height;
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
        pushTx(ClientUtils.getTxHex(tx0.getTx()));
      } catch (Exception e) {
        // preserve pushTx message
        throw new NotifiableException(e.getMessage());
      }

      // notify
      WhirlpoolEventService.getInstance().post(new Tx0Event(this, tx0));
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

  public void open() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Opening wallet " + walletIdentifier);
    }

    // instanciate data
    this.dataPersister = config.getDataPersisterFactory().createDataPersister(this, bip44w);
    this.dataSource = config.getDataSourceFactory().createDataSource(this, bip44w, dataPersister);

    // start orchestrators
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
    dataPersister.load();

    // forced persist initial data (or fail)
    dataPersister.persist(true);

    // open data
    dataPersister.open();
    dataSource.open();

    // log wallets
    for (BipWalletAndAddressType bipWallet : getWalletSupplier().getWallets()) {
      String nextReceivePath =
          bipWallet.getNextAddress(false).getPathFull(bipWallet.getAddressType());
      String nextChangePath =
          bipWallet.getNextChangeAddress(false).getPathFull(bipWallet.getAddressType());
      String pub =
          log.isDebugEnabled() ? bipWallet.getPub() : ClientUtils.maskString(bipWallet.getPub());
      log.info(
          " +WALLET "
              + bipWallet.getAccount()
              + ", "
              + bipWallet.getAddressType()
              + ", receive="
              + nextReceivePath
              + ", change="
              + nextChangePath
              + ", "
              + pub);
    }

    // check postmix index against coordinator
    checkPostmixIndex();

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

  public synchronized void start() {
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
    UtxoData utxoData = getUtxoSupplier().getValue();
    WhirlpoolEventService.getInstance().post(new WalletStartEvent(this, utxoData));
    onUtxoChanges(utxoData);
  }

  public void onUtxoChanges(UtxoData utxoData) {
    if (mixOrchestrator != null) {
      mixOrchestrator.onUtxoChanges(utxoData.getUtxoChanges());
    }
    if (autoTx0Orchestrator.isPresent()) {
      autoTx0Orchestrator.get().onUtxoChanges(utxoData.getUtxoChanges());
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

  public void pushTx(String txHex) throws Exception {
    dataSource.pushTx(txHex);
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

  public Tx0ParamService getTx0ParamService() {
    return dataSource.getTx0ParamService();
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
    refreshUtxosDelay();

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
        log.error("onMixFail(" + failReason + "): stopping mixing");
        stop();
        break;

      case DISCONNECTED:
      case MIX_FAILED:
        // retry later
        log.info("onMixFail(" + failReason + "): will retry later");
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
        log.warn("onMixFail(" + failReason + "): won't retry");
        break;

      default:
        // not retrying
        log.warn("onMixFail(" + failReason + "): unknown reason");
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
      getUtxoSupplier().refresh();
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private void checkPostmixIndex() throws Exception {
    IIndexHandler postmixIndexHandler = getWalletPostmix().getIndexHandler();
    int initialPostmixIndex =
        ClientUtils.computeNextReceiveAddressIndex(postmixIndexHandler, config.isMobile());
    if (log.isDebugEnabled()) {
      log.debug("checking postmixIndex: " + initialPostmixIndex);
    }
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
        } else {
          postmixIndexHandler.cancelUnconfirmed(initialPostmixIndex);
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
    walletAggregateService.toAddress(getWalletDeposit(), toAddress);

    // refresh
    getUtxoSupplier().refresh();
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

  protected DataPersister getDataPersister() {
    return dataPersister;
  }

  protected DataSource getDataSource() {
    return dataSource;
  }
}
