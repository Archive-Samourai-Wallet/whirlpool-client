package com.samourai.whirlpool.client.wallet;

import com.google.common.primitives.Bytes;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bip47.rpc.BIP47Account;
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
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.provider.SimpleCahootsUtxoProvider;
import com.samourai.wallet.send.spend.SpendBuilder;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.DebugUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
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
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.soroban.api.WhirlpoolApiClient;
import com.samourai.xmanager.client.XManagerClient;
import com.samourai.xmanager.protocol.XManagerService;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
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
  private BIP47Account bip47Account;
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
  private MixHistory mixHistory;

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
            .getBIP44(
                seed,
                passphrase != null ? passphrase : "",
                config.getWhirlpoolNetwork().getParams()),
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

    Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
    this.walletAggregateService =
        new WalletAggregateService(config.getWhirlpoolNetwork().getParams(), bech32Util, this);
    this.postmixIndexService = new PostmixIndexService(config);

    this.bip44w = bip44w;
    this.bip47Account = new BIP47Wallet(bip44w).getAccount(config.getBip47AccountId());
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
    this.mixHistory = new MixHistory();
  }

  protected static String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  public Single<Cahoots> tx0x2(
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0Config tx0Config,
      PaymentCode paymentCodeCounterparty)
      throws Exception {
    // adapt tx0() for WhirlpoolUtxo
    Callable<Single<Cahoots>> runTx0x2 =
        () ->
            tx0x2(
                WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos),
                tx0Config,
                pool,
                paymentCodeCounterparty);
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0x2);
  }

  public Single<Cahoots> tx0x2(
      Collection<UnspentOutput> spendFroms,
      Tx0Config tx0Config,
      Pool pool,
      PaymentCode paymentCodeCounterparty)
      throws Exception {
    // build initial TX0
    Tx0 tx0Initial =
        asyncUtil.blockingGet(
            withWhirlpoolApiClient(
                whirlpoolApiClient ->
                    tx0Service.tx0(
                        spendFroms,
                        getWalletSupplier(),
                        pool,
                        tx0Config,
                        getUtxoSupplier(),
                        whirlpoolApiClient,
                        getCoordinatorSupplier())));

    // start Cahoots
    long minerFee = getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_4); // never used
    int account = 0; // never used
    Tx0x2Context tx0x2Context =
        Tx0x2Context.newInitiator(getCahootsWallet(), account, minerFee, tx0Service, tx0Initial);
    return getSorobanWalletInitiator().meetAndInitiate(tx0x2Context, paymentCodeCounterparty);
  }

  public Single<Tx0Previews> tx0Previews(
      Collection<WhirlpoolUtxo> whirlpoolUtxos, Tx0Config tx0Config) throws Exception {
    return tx0Previews(tx0Config, WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos));
  }

  public Single<Tx0Previews> tx0Previews(
      Tx0Config tx0Config, Collection<UnspentOutput> whirlpoolUtxos) throws Exception {
    return withWhirlpoolApiClient(
        whirlpoolApiClient ->
            dataSource
                .getTx0PreviewService()
                .tx0Previews(
                    tx0Config, whirlpoolUtxos, whirlpoolApiClient, getCoordinatorSupplier()));
  }

  private <T> T handleUtxoStatusForTx0(Collection<WhirlpoolUtxo> whirlpoolUtxos, Callable<T> runTx0)
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
      T tx0 = runTx0.call();

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

  public List<Tx0> tx0Cascade(
      Collection<WhirlpoolUtxo> whirlpoolUtxos, Collection<Pool> pools, Tx0Config tx0Config)
      throws Exception {
    // adapt tx0Cascade() for WhirlpoolUtxo
    Callable<List<Tx0>> runTx0 =
        () -> tx0Cascade(WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos), tx0Config, pools);
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0);
  }

  public List<Tx0> tx0Cascade(
      Collection<UnspentOutput> spendFroms, Tx0Config tx0Config, Collection<Pool> pools)
      throws Exception {

    // create TX0s
    List<Tx0> tx0List =
        withWhirlpoolApiClient(
            whirlpoolApiClient ->
                tx0Service.tx0Cascade(
                    spendFroms,
                    getWalletSupplier(),
                    pools,
                    tx0Config,
                    getUtxoSupplier(),
                    whirlpoolApiClient,
                    getCoordinatorSupplier()));

    // broadcast each TX0
    int num = 1;
    for (Tx0 tx0 : tx0List) {
      if (log.isDebugEnabled()) {
        log.debug("Pushing Tx0 " + (num) + "/" + tx0List.size() + ": " + tx0);
      }
      // broadcast
      tx0Service.pushTx0WithRetryOnAddressReuse(tx0, this);
      num++;
    }
    // refresh new utxos in background
    refreshUtxosDelayAsync().subscribe();
    return tx0List;
  }

  public Tx0 tx0(Collection<WhirlpoolUtxo> whirlpoolUtxos, Pool pool, Tx0Config tx0Config)
      throws Exception {
    // adapt tx0() for WhirlpoolUtxo
    Callable<Tx0> runTx0 =
        () ->
            asyncUtil.blockingGet(
                tx0(WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos), tx0Config, pool));
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0);
  }

  public Single<Tx0> tx0(Collection<UnspentOutput> spendFroms, Tx0Config tx0Config, Pool pool)
      throws Exception {
    int initialPremixIndex = getWalletPremix().getIndexHandlerReceive().get();
    int initialChangeIndex = getWalletDeposit().getIndexHandlerChange().get();
    try {
      // create tx0
      return withWhirlpoolApiClient(
              whirlpoolApiClient ->
                  tx0Service.tx0(
                      spendFroms,
                      getWalletSupplier(),
                      pool,
                      tx0Config,
                      getUtxoSupplier(),
                      whirlpoolApiClient,
                      getCoordinatorSupplier()))
          .map(
              tx0 -> {
                // broadcast (or retry on address-reuse)
                tx0Service.pushTx0WithRetryOnAddressReuse(tx0, this);
                // refresh new utxos in background
                refreshUtxosDelayAsync().subscribe();
                return tx0;
              });
    } catch (Exception e) {
      // revert index
      getWalletPremix().getIndexHandlerReceive().set(initialPremixIndex, true);
      getWalletDeposit().getIndexHandlerChange().set(initialChangeIndex, true);
      throw e;
    }
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
    this.tx0Service =
        new Tx0Service(
            config.getWhirlpoolNetwork().getParams(),
            dataSource.getTx0PreviewService(),
            config.getFeeOpReturnImpl());
    this.paynymSupplier = dataSource.getPaynymSupplier();
    this.cahootsWallet =
        new CahootsWallet(
            getWalletSupplier(),
            getChainSupplier(),
            BIP_FORMAT.PROVIDER,
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
    if (log.isDebugEnabled()) {
      for (BipWallet bipWallet : getWalletSupplier().getWallets()) {
        String nextReceivePath = bipWallet.getNextAddressReceive(false).getPathAddress();
        String nextChangePath = bipWallet.getNextAddressChange(false).getPathAddress();
        String pub = ClientUtils.maskString(bipWallet.getBipPub());
        log.debug(
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
                + ", "
                + pub);
      }
      ExternalDestination externalDestination = config.getExternalDestination();
      if (externalDestination != null) {
        IPostmixHandler postmixHandlerExternal = getPostmixHandler();
        MixDestination mixDestination =
            postmixHandlerExternal.computeDestinationNext(); // increments unconfirmed
        String pub = ClientUtils.maskString(externalDestination.getXpub());
        log.debug(
            " +EXTERNAL-XPUB: bipFormat="
                + BIP_FORMAT.SEGWIT_NATIVE.getId()
                + ", receive="
                + mixDestination.getPath()
                + ", "
                + pub);
        postmixHandlerExternal.onMixFail(); // revert unconfirmed index
      }
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
    // check coordinators up
    checkCoordinators();

    // check postmix index against coordinator
    return checkAndFixPostmixIndex()
        .doOnComplete(
            () -> {
              // start mixing on success
              doStart();
            });
  }

  protected void checkCoordinators() {
    // wait for online Whirlpool coordinator
    while (dataSource.getCoordinatorSupplier().getCoordinators().isEmpty()) {
      log.warn("Waiting for Whirlpool coordinator to be online...");
      try {
        wait(5000);
      } catch (InterruptedException e) {
      }
      try {
        dataSource.getCoordinatorSupplier().refresh();
      } catch (Exception e) {
        log.error("", e);
      }
    }
    log.info(
        "Found "
            + dataSource.getCoordinatorSupplier().getPools().size()
            + " pools and "
            + dataSource.getCoordinatorSupplier().getCoordinators().size()
            + " coordinator(s)");
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

  public <R> R withWhirlpoolApiClient(CallbackWithArg<WhirlpoolApiClient, R> callable)
      throws Exception {
    WhirlpoolApiClient whirlpoolApiClient = config.createWhirlpoolApiClient();
    return callable.apply(whirlpoolApiClient);
  }

  public PoolSupplier getPoolSupplier() {
    return getCoordinatorSupplier();
  }

  public CoordinatorSupplier getCoordinatorSupplier() {
    return dataSource.getCoordinatorSupplier();
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

  public void onMixSuccess(
      MixParams mixParams, Utxo receiveUtxo, MixDestination receiveDestination) {
    WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    // log
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String logPrefix = "[MIX] " + (poolId != null ? poolId + " " : "");
    log.info(
        logPrefix
            + "⣿ WHIRLPOOL SUCCESS ⣿ txid: "
            + receiveUtxo.getHash()
            + ", receiveAddress="
            + receiveDestination.getAddress()
            + ", path="
            + receiveDestination.getPath()
            + ", type="
            + receiveDestination.getType());

    // forward utxoConfig
    int newMixsDone = whirlpoolUtxo.getMixsDone() + 1;
    getUtxoConfigSupplier()
        .setMixsDone(receiveUtxo.getHash(), (int) receiveUtxo.getIndex(), newMixsDone);

    // stats
    mixHistory.onMixSuccess(mixParams, receiveUtxo, receiveDestination);

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
    String logPrefix = "[MIX] " + (poolId != null ? poolId + " " : "");

    String message = failReason.getMessage();
    if (notifiableError != null) {
      message += " ; " + notifiableError;
    }
    if (failReason.isSilent()) {
      log.info(logPrefix + message);
    } else {
      log.error(logPrefix + "⣿ WHIRLPOOL FAILED ⣿ " + message);
    }

    // mix history
    if (!failReason.isSilent()) {
      mixHistory.onMixFail(mixParams, failReason, notifiableError);
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
        try {
          checkAndFixPostmixIndex(mixParams.getPostmixHandler());
        } catch (Exception e) {
          // stop mixing on postmixIndex error
          log.error(e.getMessage());
          stop();
        }
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
    String logPrefix = "[MIX] " + (poolId != null ? poolId + " " : "");

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
    return ClientUtils.sleepUtxosDelayAsync(config.getWhirlpoolNetwork().getParams())
        .doOnComplete(() -> refreshUtxosAsync().blockingAwait());
  }

  /** Refresh utxos now */
  public Completable refreshUtxosAsync() {
    return asyncUtil.runIOAsyncCompletable(() -> getUtxoSupplier().refresh());
  }

  public synchronized Completable checkAndFixPostmixIndex() {
    return asyncUtil.runIOAsyncCompletable(
        () -> {
          if (!config.isPostmixIndexCheck()) {
            // check disabled
            log.warn("postmixIndexCheck is disabled");
            return;
          }
          // check POSTMIX
          checkAndFixPostmixIndex(getPostmixHandler());

          // check external-xpub
          ExternalDestination externalDestination = config.getExternalDestination();
          if (externalDestination != null) {
            checkAndFixPostmixIndex(externalDestination.computePostmixHandler(this));
          }
        });
  }

  protected void checkAndFixPostmixIndex(IPostmixHandler postmixHandler)
      throws NotifiableException {
    if (log.isDebugEnabled()) {
      log.debug("Checking next index: " + postmixHandler.getClass().getName());
    }
    ISeenBackend seenBackend = getSeenBackend();
    try {
      // check
      postmixIndexService.checkPostmixIndex(postmixHandler, seenBackend);
    } catch (PostmixIndexAlreadyUsedException e) {
      // postmix index is desynchronized
      if (log.isDebugEnabled()) {
        log.warn("postmixIndex is desynchronized: " + e.getMessage());
      }
      WhirlpoolEventService.getInstance().post(new PostmixIndexAlreadyUsedEvent(this));
      if (config.isPostmixIndexAutoFix()) {
        // autofix
        try {
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixProgressEvent(this));
          postmixIndexService.fixPostmixIndex(postmixHandler, seenBackend);
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixSuccessEvent(this));
        } catch (PostmixIndexAlreadyUsedException ee) {
          // could not autofix
          WhirlpoolEventService.getInstance().post(new PostmixIndexFixFailEvent(this));
          throw new NotifiableException(
              "PostmixIndex error - please resync your wallet or contact support. PostmixIndex="
                  + ee.getPostmixIndex());
        } catch (Exception ee) {
          // ignore other errors such as http timeout
          log.warn("ignoring fixPostmixIndex failure", ee);
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

  public MixHistory getMixHistory() {
    return mixHistory;
  }

  protected DataPersister getDataPersister() {
    return dataPersister;
  }

  protected DataSource getDataSource() {
    return dataSource;
  }

  public ISweepBackend getSweepBackend() throws Exception {
    return dataSource.getSweepBackend();
  }

  public XManagerClient getXManagerClient() {
    if (xManagerClient == null) {
      xManagerClient = config.computeXManagerClient();
    }
    return xManagerClient;
  }

  public ISeenBackend getSeenBackend() {
    return dataSource.getSeenBackend();
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
        getBip47Account(),
        bip47WalletOutgoingIdx);
  }

  public IPostmixHandler getPostmixHandler() {
    return new Bip84PostmixHandler(
        config.getWhirlpoolNetwork().getParams(),
        getWalletPostmix(),
        config.getIndexRangePostmix());
  }

  public IPostmixHandler computePostmixHandler(WhirlpoolUtxo whirlpoolUtxo) {
    ExternalDestination externalDestination = config.getExternalDestination();
    if (externalDestination != null) {
      int nextMixsDone = whirlpoolUtxo.getMixsDone() + 1;
      if (nextMixsDone >= externalDestination.getMixs()) {
        // random factor for privacy
        if (externalDestination.useRandomDelay()) {
          if (log.isDebugEnabled()) {
            log.debug(
                "Mixing to POSTMIX, external destination randomly delayed for better privacy ("
                    + whirlpoolUtxo
                    + ")");
          }
        } else {
          if (log.isDebugEnabled()) {
            log.debug("Mixing to EXTERNAL (" + whirlpoolUtxo + ")");
          }
          return externalDestination.computePostmixHandler(this);
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug(
              "Mixing to POSTMIX, mix "
                  + nextMixsDone
                  + "/"
                  + externalDestination.getMixs()
                  + " before external destination ("
                  + whirlpoolUtxo
                  + ")");
        }
      }
    }
    return getPostmixHandler();
  }

  public PostmixIndexService getPostmixIndexService() {
    return postmixIndexService;
  }

  public BIP47Account getBip47Account() {
    return bip47Account;
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
