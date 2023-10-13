package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Context;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.ricochet.RicochetConfig;
import com.samourai.wallet.send.provider.SimpleCahootsUtxoProvider;
import com.samourai.wallet.send.spend.SpendBuilder;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.DebugUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
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
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.xmanager.client.XManagerClient;
import com.samourai.xmanager.protocol.XManagerService;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
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
  private DataPersister dataPersister;
  private DataSource dataSource;
  private Tx0Service tx0Service;
  private PaynymSupplier paynymSupplier;
  private CahootsWallet cahootsWallet;
  private SorobanWalletInitiator sorobanWalletInitiator;
  private SorobanWalletCounterparty sorobanWalletCounterparty;

  protected MixOrchestratorImpl mixOrchestrator;
  private Optional<AutoTx0Orchestrator> autoTx0Orchestrator;
  private MixingStateEditable mixingState;

  private XManagerClient xManagerClient;

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
    this.xManagerClient = null;

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

    this.walletAggregateService = new WalletAggregateService(config.getNetworkParameters(), this);
    this.postmixIndexService = new PostmixIndexService(config);

    this.bip44w = bip44w;
    this.dataPersister = null;
    this.dataSource = null;
    this.tx0Service = null; // will be set with datasource
    this.paynymSupplier = null; // will be set with datasource
    this.cahootsWallet = null; // will be set with datasource
    this.sorobanWalletInitiator = null; // will be set with getter
    this.sorobanWalletCounterparty = null; // will be set with getter

    this.mixOrchestrator = null;
    this.autoTx0Orchestrator = Optional.empty();
    this.mixingState = new MixingStateEditable(this, false);
  }

  protected static String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  public Tx0x2Context tx0x2Context(Tx0Config tx0Config) throws Exception {
    // start Cahoots
    long minerFee = getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_4); // never used
    int account = 0; // never used
    ServerApi serverApi = config.getServerApi();
    return Tx0x2Context.newInitiator(
        getCahootsWallet(), account, minerFee, tx0Service, tx0Config, serverApi);
  }

  public Single<Cahoots> tx0x2(Tx0Config tx0Config, PaymentCode paymentCodeCounterparty)
      throws Exception {
    Callable<Single<Cahoots>> runTx0x2 = () -> doTx0x2(tx0Config, paymentCodeCounterparty);
    return handleTx0(tx0Config.getOwnSpendFromUtxos(), runTx0x2);
  }

  protected Single<Cahoots> doTx0x2(Tx0Config tx0Config, PaymentCode paymentCodeCounterparty)
      throws Exception {
    // initiator context
    Tx0x2Context context = tx0x2Context(tx0Config);

    // start Cahoots
    return getSorobanWalletInitiator().meetAndInitiate(context, paymentCodeCounterparty);
  }

  public Optional<Tx0PreviewResult> tx0Preview(Tx0PreviewConfig tx0PreviewConfig, Pool pool)
      throws Exception {
    return dataSource.getTx0PreviewService().tx0Preview(tx0PreviewConfig, pool);
  }

  public Map<String, Tx0PreviewResult> tx0Previews(
      Tx0PreviewConfig tx0PreviewConfig, Collection<Pool> pools) throws Exception {
    return dataSource.getTx0PreviewService().tx0Previews(tx0PreviewConfig, pools);
  }

  public Tx0Result tx0(Tx0Config tx0Config) throws Exception {
    Callable<Tx0Result> runTx0 = () -> doTx0(tx0Config);
    return handleTx0(tx0Config.getOwnSpendFromUtxos(), runTx0);
  }

  protected Tx0Result doTx0(Tx0Config tx0Config) throws Exception {
    // create TX0s
    Tx0Result tx0Result =
        tx0Service
            .tx0(tx0Config)
            .orElseThrow(
                () ->
                    new NotifiableException(
                        "Tx0 is not possible for pool: " + tx0Config.getPool().getPoolId()));
    List<Tx0> tx0List = tx0Result.getList();

    // broadcast each TX0
    int num = 1;
    for (Tx0 tx0 : tx0List) {
      if (log.isDebugEnabled()) {
        log.debug("Pushing Tx0 (" + (num) + "/" + tx0List.size() + "): " + tx0);
      }
      // broadcast
      tx0Service.pushTx0WithRetryOnAddressReuse(tx0, this);
      num++;
    }
    // refresh new utxos in background
    refreshUtxosDelayAsync().subscribe();
    return new Tx0Result(tx0List);
  }

  private <T> T handleTx0(Collection<? extends BipUtxo> utxos, Callable<T> runTx0)
      throws Exception {

    // find WhirlpoolUtxos
    Collection<WhirlpoolUtxo> whirlpoolUtxos =
        utxos.stream()
            .map(utxo -> getUtxoSupplier().findUtxo(utxo.getTxHash(), utxo.getTxOutputIndex()))
            .filter(utxo -> utxo != null)
            .collect(Collectors.toList());

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
      // save indexs
      int initialPremixIndex = getWalletPremix().getIndexHandlerReceive().get();
      int initialChangeIndex = getWalletDeposit().getIndexHandlerChange().get();
      T tx0;
      try {
        // run
        tx0 = runTx0.call();
      } catch (Exception e) {
        // revert indexs
        getWalletPremix().getIndexHandlerReceive().set(initialPremixIndex, true);
        getWalletDeposit().getIndexHandlerChange().set(initialChangeIndex, true);
        throw e;
      }

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

  public Tx0Config getTx0Config(
      Pool pool,
      Collection<? extends BipUtxo> spendFroms,
      Tx0FeeTarget tx0FeeTarget,
      Tx0FeeTarget mixFeeTarget) {
    BipWallet premixWallet = getWalletSupplier().getWallet(BIP_WALLET.PREMIX_BIP84);
    BipWallet changeWallet = getWalletSupplier().getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet feeChangeWallet = changeWallet;
    Tx0Config tx0Config =
        new Tx0Config(
            tx0FeeTarget,
            mixFeeTarget,
            (Collection<BipUtxo>) spendFroms,
            getUtxoSupplier(),
            premixWallet,
            changeWallet,
            feeChangeWallet,
            pool);
    return tx0Config;
  }

  public boolean isStarted() {
    return mixingState.isStarted();
  }

  public void open(String passphrase) throws Exception {
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
                passphrase,
                dataPersister.getWalletStateSupplier(),
                dataPersister.getUtxoConfigSupplier());
    dataSource.getTx0PreviewService()._setPoolSupplier(dataSource.getPoolSupplier());
    this.tx0Service =
        new Tx0Service(
            config.getNetworkParameters(),
            dataSource.getTx0PreviewService(),
            config.getFeeOpReturnImpl());
    this.paynymSupplier = dataSource.getPaynymSupplier();
    this.cahootsWallet =
        new CahootsWallet(
            getWalletSupplier(),
            getChainSupplier(),
            BIP_FORMAT.PROVIDER,
            config.getNetworkParameters(),
            new SimpleCahootsUtxoProvider(getUtxoSupplier()));

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
      String nextReceivePath = bipWallet.getNextAddressReceive(false).getPathAddress();
      String nextChangePath = bipWallet.getNextAddressChange(false).getPathAddress();
      log.info(
          " +WALLET "
              + bipWallet.getId()
              + ": account="
              + bipWallet.getAccount()
              + ", bipFormat="
              + bipWallet.getBipFormatDefault().getId()
              + ", receive="
              + nextReceivePath
              + ", change="
              + nextChangePath
              + ", xpub="
              + ClientUtils.maskString(bipWallet.getXPub())
              + ", bipPub="
              + ClientUtils.maskString(bipWallet.getBipPub()));
    }
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
  }

  public synchronized Completable startAsync() {
    // check postmix index against coordinator
    return checkAndFixPostmixIndexAsync()
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

  public IPushTx getPushTx() {
    return getDataSource().getPushTx();
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

  public Tx0Service getTx0Service() {
    return tx0Service;
  }

  // used by Sparrow
  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return dataPersister.getUtxoConfigSupplier();
  }

  public void mix(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    mixOrchestrator.mixNow(whirlpoolUtxo);
  }

  public void onMixSuccess(MixParams mixParams, UtxoDetail receiveUtxo) {
    WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    // log
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String logPrefix = " - [MIX] " + (poolId != null ? poolId + " " : "");
    MixDestination destination = whirlpoolUtxo.getUtxoState().getMixProgress().getDestination();
    log.info(
        logPrefix
            + "⣿ WHIRLPOOL SUCCESS ⣿ txid: "
            + receiveUtxo.getTxHash()
            + ", receiveAddress="
            + destination.getAddress()
            + ", path="
            + destination.getPath()
            + ", type="
            + destination.getType());

    // forward utxoConfig
    int newMixsDone = whirlpoolUtxo.getMixsDone() + 1;
    getUtxoConfigSupplier()
        .setMixsDone(receiveUtxo.getTxHash(), receiveUtxo.getTxOutputIndex(), newMixsDone);

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
        checkAndFixPostmixIndexAsync()
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

  public synchronized Completable checkAndFixPostmixIndexAsync() {
    return asyncUtil.runIOAsyncCompletable(
        () -> {
          if (!config.isPostmixIndexCheck()) {
            // check disabled
            log.warn("postmixIndexCheck is disabled");
            return;
          }
          checkAndFixPostmixIndex();
        });
  }

  protected void checkAndFixPostmixIndex() throws NotifiableException {
    try {
      // check
      postmixIndexService.checkPostmixIndex(getWalletPostmix());
    } catch (PostmixIndexAlreadyUsedException e) {
      // postmix index is desynchronized
      log.error(
          "postmixIndex is desynchronized: " + e.getClass().getSimpleName() + " " + e.getMessage());
      WhirlpoolEventService.getInstance().post(new PostmixIndexAlreadyUsedEvent(this));
      if (config.isPostmixIndexAutoFix()) {
        // autofix
        try {
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixProgressEvent(this));
          postmixIndexService.fixPostmixIndex(getWalletPostmix());
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixSuccessEvent(this));
        } catch (PostmixIndexAlreadyUsedException ee) {
          // could not autofix
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixFailEvent(this));
          throw new NotifiableException(
              "PostmixIndex error - please resync your wallet or contact support. PostmixIndex="
                  + ee.getPostmixIndex());
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
    return getWalletDeposit().getNextAddressReceive(increment).getAddressString();
  }

  public void notifyError(String message) {
    log.error(message);
  }

  public SpendBuilder getSpendBuilder() {
    return new SpendBuilder(getUtxoSupplier());
  }

  public String getZpubDeposit() {
    return getWalletDeposit().getBipPub();
  }

  public String getZpubPremix() {
    return getWalletPremix().getBipPub();
  }

  public String getZpubPostmix() {
    return getWalletPostmix().getBipPub();
  }

  public String getZpubBadBank() {
    return getWalletBadbank().getBipPub();
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

  public XManagerClient getXManagerClient() {
    if (xManagerClient == null) {
      xManagerClient = config.computeXManagerClient();
    }
    return xManagerClient;
  }

  public RicochetConfig newRicochetConfig(
      int feePerB, boolean useTimeLock, WhirlpoolAccount spendAccount) {
    long latestBlock = getChainSupplier().getLatestBlock().height;
    BipWallet bipWalletRicochet = getWalletSupplier().getWallet(BIP_WALLET.RICOCHET_BIP84);
    BipWallet bipWalletChange =
        getWalletSupplier().getWallet(spendAccount, BIP_FORMAT.SEGWIT_NATIVE);
    int bip47WalletOutgoingIdx = 0; // TODO zl !!!
    boolean samouraiFeeViaBIP47 = getPaynymSupplier().getPaynymState().isClaimed();
    String samouraiFeeAddress = getXManagerClient().getAddressOrDefault(XManagerService.RICOCHET);
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
        getBip47Wallet(),
        bip47WalletOutgoingIdx);
  }

  public BIP47Wallet getBip47Wallet() {
    return new BIP47Wallet(bip44w);
  }

  public CahootsWallet getCahootsWallet() {
    return cahootsWallet;
  }

  public SorobanWalletInitiator getSorobanWalletInitiator() {
    if (sorobanWalletInitiator == null) {
      SorobanWalletService sorobanWalletService = config.getSorobanWalletService();
      if (sorobanWalletService == null) {
        log.error("whirlpoolWalletConfig.sorobanWalletService is NULL");
        return null;
      }
      this.sorobanWalletInitiator =
          sorobanWalletService.getSorobanWalletInitiator(getCahootsWallet());
    }
    return sorobanWalletInitiator;
  }

  public SorobanWalletCounterparty getSorobanWalletCounterparty() {
    if (sorobanWalletCounterparty == null) {
      SorobanWalletService sorobanWalletService = config.getSorobanWalletService();
      if (sorobanWalletService == null) {
        log.error("whirlpoolWalletConfig.sorobanWalletService is NULL");
        return null;
      }
      this.sorobanWalletCounterparty =
          sorobanWalletService.getSorobanWalletCounterparty(getCahootsWallet());
    }
    return sorobanWalletCounterparty;
  }
}
