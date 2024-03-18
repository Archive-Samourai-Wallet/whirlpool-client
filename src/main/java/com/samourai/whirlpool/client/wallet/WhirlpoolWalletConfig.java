package com.samourai.whirlpool.client.wallet;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.xmanagerClient.XManagerClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.ITx0PreviewServiceConfig;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImpl;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImplV1;
import com.samourai.whirlpool.protocol.util.XorMask;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletConfig extends WhirlpoolClientConfig
    implements ITx0PreviewServiceConfig {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolWalletConfig.class);

  private DataSourceFactory dataSourceFactory;
  private DataPersisterFactory dataPersisterFactory;
  private boolean mobile;

  private int maxClients;
  private int maxClientsPerPool;
  private int extraLiquidityClientsPerPool;
  private int clientDelay;
  private int autoTx0Delay;
  private String autoTx0PoolId;
  private boolean autoTx0Aggregate; // only on testnet
  private Tx0FeeTarget autoTx0FeeTarget;
  private boolean autoMix;
  private String scode;
  private int tx0MaxOutputs;
  private int tx0AttemptsAddressReuse;
  private int tx0AttemptsSoroban;
  private Map<String, Long> overspend;

  private int refreshUtxoDelay;
  private int refreshPoolsDelay;
  private int refreshPaynymDelay;

  private int feeMin;
  private int feeMax;
  private int feeFallback;

  private boolean resyncOnFirstRun;
  private boolean postmixIndexCheck;
  private boolean postmixIndexAutoFix;
  private int persistDelaySeconds;
  private String partner;
  private int bip47AccountId;

  private ISecretPointFactory secretPointFactory;
  private SorobanWalletService sorobanWalletService;
  private BIP47UtilGeneric bip47Util;
  private FeeOpReturnImpl feeOpReturnImpl;

  public WhirlpoolWalletConfig(
      DataSourceFactory dataSourceFactory,
      ISecretPointFactory secretPointFactory,
      CryptoUtil cryptoUtil,
      SorobanWalletService sorobanWalletService,
      IHttpClientService httpClientService,
      BIP47UtilGeneric bip47Util,
      SamouraiNetwork samouraiNetwork,
      boolean mobile,
      boolean onion) {
    // Android => odd indexs, CLI => even indexs
    super(
        httpClientService,
        bip47Util,
        cryptoUtil,
        null,
        samouraiNetwork,
        mobile ? IndexRange.ODD : IndexRange.EVEN,
        onion);

    this.dataSourceFactory = dataSourceFactory;
    this.secretPointFactory = secretPointFactory;
    this.sorobanWalletService = sorobanWalletService;
    this.dataPersisterFactory = new FileDataPersisterFactory();
    this.mobile = mobile;

    // default settings
    this.maxClients = mobile ? 1 : 5;
    this.maxClientsPerPool = 1;
    this.extraLiquidityClientsPerPool = mobile ? 0 : 1;
    this.clientDelay = 30;
    this.autoTx0Delay = 60;
    this.autoTx0PoolId = null;
    this.autoTx0Aggregate = false;
    this.autoTx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
    this.autoMix = true;
    this.scode = null;
    this.tx0MaxOutputs = 0;
    this.tx0AttemptsAddressReuse = 5;
    this.tx0AttemptsSoroban = 3;
    this.overspend = new LinkedHashMap<String, Long>();

    // technical settings
    this.refreshUtxoDelay = 60; // 1min
    this.refreshPoolsDelay = 600; // 10min
    this.refreshPaynymDelay = 3600; // 1h

    this.feeMin = 1;
    this.feeMax = 510;
    this.feeFallback = 75;

    this.resyncOnFirstRun = false;
    this.postmixIndexCheck = true;
    this.postmixIndexAutoFix = true;
    this.persistDelaySeconds = 10;
    this.partner = WhirlpoolProtocol.PARTNER_ID_SAMOURAI;
    this.bip47AccountId = 0;

    this.bip47Util = Bip47UtilJava.getInstance();

    // use OpReturnImplV1
    XorMask xorMask = XorMask.getInstance(secretPointFactory);
    feeOpReturnImpl = new FeeOpReturnImplV1(xorMask);
  }

  public WhirlpoolWalletConfig(
      DataSourceFactory dataSourceFactory, SorobanConfig sorobanConfig, boolean mobile) {
    this(
        dataSourceFactory,
        sorobanConfig.getExtLibJConfig().getSecretPointFactory(),
        sorobanConfig.getExtLibJConfig().getCryptoUtil(),
        sorobanConfig.getSorobanWalletService(),
        sorobanConfig.getExtLibJConfig().getHttpClientService(),
        sorobanConfig.getExtLibJConfig().getBip47Util(),
        sorobanConfig.getExtLibJConfig().getSamouraiNetwork(),
        mobile,
        sorobanConfig.getExtLibJConfig().isOnion());
  }

  public void verify() throws Exception {
    boolean isTestnet =
        FormatsUtilGeneric.getInstance().isTestNet(getSamouraiNetwork().getParams());

    // require testnet for autoTx0Aggregate
    if (autoTx0Aggregate && !isTestnet) {
      throw new RuntimeException("autoTx0Aggregate is only available for testnet");
    }

    // require autoTx0PoolId for autoTx0Aggregate
    if (autoTx0Aggregate && StringUtils.isEmpty(autoTx0PoolId)) {
      throw new RuntimeException("autoTx0Aggregate requires autoTx0PoolId");
    }

    // verify JCE provider doesn't throw any exception
    try {
      ECKey ecKey = new ECKey();
      secretPointFactory
          .newSecretPoint(ecKey.getPrivKeyBytes(), ecKey.getPubKey())
          .ECDHSecretAsBytes();
    } catch (Exception e) {
      log.error("secretPointFactory not supported", e);
      String javaVersion = System.getProperty("java.version");
      throw new NotifiableException(
          "Java version not supported, please use a another Java runtime (current: "
              + javaVersion
              + ", recommended: OpenJDK 8-11).");
    }
  }

  public XManagerClient computeXManagerClient() {
    boolean testnet = FormatsUtilGeneric.getInstance().isTestNet(getSamouraiNetwork().getParams());
    return new XManagerClient(
        getHttpClientService().getHttpClient(HttpUsage.BACKEND), testnet, false);
  }

  public DataSourceFactory getDataSourceFactory() {
    return dataSourceFactory;
  }

  public DataPersisterFactory getDataPersisterFactory() {
    return dataPersisterFactory;
  }

  public void setDataPersisterFactory(DataPersisterFactory dataPersisterFactory) {
    this.dataPersisterFactory = dataPersisterFactory;
  }

  public boolean isMobile() {
    return mobile;
  }

  public void setMobile(boolean mobile) {
    this.mobile = mobile;
  }

  public int getMaxClients() {
    return maxClients;
  }

  public void setMaxClients(int maxClients) {
    this.maxClients = maxClients;
  }

  public int getMaxClientsPerPool() {
    return maxClientsPerPool;
  }

  public void setMaxClientsPerPool(int maxClientsPerPool) {
    this.maxClientsPerPool = maxClientsPerPool;
  }

  public int getExtraLiquidityClientsPerPool() {
    return extraLiquidityClientsPerPool;
  }

  public void setExtraLiquidityClientsPerPool(int extraLiquidityClientsPerPool) {
    this.extraLiquidityClientsPerPool = extraLiquidityClientsPerPool;
  }

  public int getClientDelay() {
    return clientDelay;
  }

  public void setClientDelay(int clientDelay) {
    this.clientDelay = clientDelay;
  }

  public int getAutoTx0Delay() {
    return autoTx0Delay;
  }

  public void setAutoTx0Delay(int autoTx0Delay) {
    this.autoTx0Delay = autoTx0Delay;
  }

  public boolean isAutoTx0() {
    return !StringUtils.isEmpty(autoTx0PoolId);
  }

  public String getAutoTx0PoolId() {
    return autoTx0PoolId;
  }

  public boolean isAutoTx0Aggregate() {
    return autoTx0Aggregate;
  }

  public void setAutoTx0Aggregate(boolean autoTx0Aggregate) {
    this.autoTx0Aggregate = autoTx0Aggregate;
  }

  public void setAutoTx0PoolId(String autoTx0PoolId) {
    this.autoTx0PoolId = autoTx0PoolId;
  }

  public Tx0FeeTarget getAutoTx0FeeTarget() {
    return autoTx0FeeTarget;
  }

  public void setAutoTx0FeeTarget(Tx0FeeTarget autoTx0FeeTarget) {
    this.autoTx0FeeTarget = autoTx0FeeTarget;
  }

  public boolean isAutoMix() {
    return autoMix;
  }

  public void setAutoMix(boolean autoMix) {
    this.autoMix = autoMix;
  }

  public String getScode() {
    return scode;
  }

  public void setScode(String scode) {
    this.scode = scode;
  }

  public int getTx0MaxOutputs() {
    return tx0MaxOutputs;
  }

  public void setTx0MaxOutputs(int tx0MaxOutputs) {
    this.tx0MaxOutputs = tx0MaxOutputs;
  }

  public int getTx0AttemptsAddressReuse() {
    return tx0AttemptsAddressReuse;
  }

  public void setTx0AttemptsSoroban(int tx0AttemptsSoroban) {
    this.tx0AttemptsSoroban = tx0AttemptsSoroban;
  }

  public int getTx0AttemptsSoroban() {
    return tx0AttemptsSoroban;
  }

  public void setTx0AttemptsAddressReuse(int tx0AttemptsAddressReuse) {
    this.tx0AttemptsAddressReuse = tx0AttemptsAddressReuse;
  }

  public Long getOverspend(String poolId) {
    return overspend != null ? overspend.get(poolId) : null;
  }

  public void setOverspend(Map<String, Long> overspend) {
    this.overspend = overspend;
  }

  public int getRefreshUtxoDelay() {
    return refreshUtxoDelay;
  }

  public void setRefreshUtxoDelay(int refreshUtxoDelay) {
    this.refreshUtxoDelay = refreshUtxoDelay;
  }

  public int getRefreshPoolsDelay() {
    return refreshPoolsDelay;
  }

  public void setRefreshPoolsDelay(int refreshPoolsDelay) {
    this.refreshPoolsDelay = refreshPoolsDelay;
  }

  public int getRefreshPaynymDelay() {
    return refreshPaynymDelay;
  }

  public void setRefreshPaynymDelay(int refreshPaynymDelay) {
    this.refreshPaynymDelay = refreshPaynymDelay;
  }

  public int getFeeMin() {
    return feeMin;
  }

  public void setFeeMin(int feeMin) {
    this.feeMin = feeMin;
  }

  public int getFeeMax() {
    return feeMax;
  }

  public void setFeeMax(int feeMax) {
    this.feeMax = feeMax;
  }

  public int getFeeFallback() {
    return feeFallback;
  }

  public void setFeeFallback(int feeFallback) {
    this.feeFallback = feeFallback;
  }

  public boolean isResyncOnFirstRun() {
    return resyncOnFirstRun;
  }

  public void setResyncOnFirstRun(boolean resyncOnFirstRun) {
    this.resyncOnFirstRun = resyncOnFirstRun;
  }

  public boolean isPostmixIndexCheck() {
    return postmixIndexCheck;
  }

  public void setPostmixIndexCheck(boolean postmixIndexCheck) {
    this.postmixIndexCheck = postmixIndexCheck;
  }

  public boolean isPostmixIndexAutoFix() {
    return postmixIndexAutoFix;
  }

  public void setPostmixIndexAutoFix(boolean postmixIndexAutoFix) {
    this.postmixIndexAutoFix = postmixIndexAutoFix;
  }

  public int getPersistDelaySeconds() {
    return persistDelaySeconds;
  }

  public void setPersistDelaySeconds(int persistDelaySeconds) {
    this.persistDelaySeconds = persistDelaySeconds;
  }

  public String getPartner() {
    return partner;
  }

  public void setPartner(String partner) {
    this.partner = partner;
  }

  public int getBip47AccountId() {
    return bip47AccountId;
  }

  public void setBip47AccountId(int bip47AccountId) {
    this.bip47AccountId = bip47AccountId;
  }

  public ISecretPointFactory getSecretPointFactory() {
    return secretPointFactory;
  }

  public void setSecretPointFactory(ISecretPointFactory secretPointFactory) {
    this.secretPointFactory = secretPointFactory;
  }

  public SorobanWalletService getSorobanWalletService() {
    return sorobanWalletService;
  }

  public BIP47UtilGeneric getBip47Util() {
    return bip47Util;
  }

  public void setBip47Util(BIP47UtilGeneric bip47Util) {
    this.bip47Util = bip47Util;
  }

  public FeeOpReturnImpl getFeeOpReturnImpl() {
    return feeOpReturnImpl;
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new LinkedHashMap<String, String>();
    configInfo.put("dataSourceFactory", dataSourceFactory.getClass().getSimpleName());
    configInfo.put("dataPersisterFactory", dataPersisterFactory.getClass().getSimpleName());
    configInfo.put(
        "server",
        "network="
            + getSamouraiNetwork().getParams().getPaymentProtocolId()
            + ", torOnionCoordinator="
            + Boolean.toString(isTorOnionCoordinator()));
    configInfo.put(
        "externalDestination",
        (getExternalDestination() != null ? getExternalDestination().toString() : "null"));
    configInfo.put("indexRangePostmix", getIndexRangePostmix().name());
    configInfo.put(
        "refreshDelay",
        "refreshUtxoDelay="
            + refreshUtxoDelay
            + ", refreshPoolsDelay="
            + refreshPoolsDelay
            + ", refreshPaynymDelay="
            + refreshPaynymDelay);
    configInfo.put(
        "mix",
        "mobile="
            + isMobile()
            + ", maxClients="
            + getMaxClients()
            + ", maxClientsPerPool="
            + getMaxClientsPerPool()
            + ", extraLiquidityClientsPerPool="
            + getExtraLiquidityClientsPerPool()
            + ", clientDelay="
            + getClientDelay()
            + ", autoTx0Delay="
            + getAutoTx0Delay()
            + ", autoTx0="
            + (isAutoTx0() ? getAutoTx0PoolId() : "false")
            + ", autoTx0Aggregate="
            + isAutoTx0Aggregate()
            + ", autoTx0FeeTarget="
            + getAutoTx0FeeTarget().name()
            + ", autoMix="
            + isAutoMix()
            + ", scode="
            + (scode != null ? ClientUtils.maskString(scode) : "null")
            + ", overspend="
            + (overspend != null ? overspend.toString() : "null"));
    configInfo.put(
        "tx0",
        "tx0MaxOutputs="
            + tx0MaxOutputs
            + ", tx0AttemptsAddressReuse="
            + tx0AttemptsAddressReuse
            + ", tx0AttemptsSoroban="
            + tx0AttemptsSoroban);
    configInfo.put(
        "fee", "fallback=" + getFeeFallback() + ", min=" + getFeeMin() + ", max=" + getFeeMax());
    configInfo.put("resyncOnFirstRun", Boolean.toString(resyncOnFirstRun));
    configInfo.put("postmixIndexCheck", Boolean.toString(postmixIndexCheck));
    configInfo.put("postmixIndexAutoFix", Boolean.toString(postmixIndexAutoFix));
    configInfo.put("persistDelaySeconds", Integer.toString(persistDelaySeconds));
    configInfo.put("secretPointFactory", secretPointFactory.getClass().getName());
    configInfo.put("cryptoUtil", getCryptoUtil().getClass().getName());
    configInfo.put(
        "sorobanWalletService",
        sorobanWalletService != null ? sorobanWalletService.getClass().getName() : "null");
    configInfo.put("bip47Util", bip47Util.getClass().getName());
    return configInfo;
  }
}
