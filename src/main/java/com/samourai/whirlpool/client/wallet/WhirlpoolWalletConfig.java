package com.samourai.whirlpool.client.wallet;

import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.bip47.rpc.java.SecretPointFactoryJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.whirlpool.client.tx0.ITx0ParamServiceConfig;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletConfig extends WhirlpoolClientConfig implements ITx0ParamServiceConfig {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWalletConfig.class);

  private int maxClients;
  private int maxClientsPerPool;
  private boolean liquidityClient;
  private int clientDelay;
  private String autoTx0PoolId;
  private Tx0FeeTarget autoTx0FeeTarget;
  private boolean autoMix;
  private String scode;
  private int tx0MaxOutputs;
  private int tx0FakeOutputRandomFactor;
  private int tx0FakeOutputMinValue;
  private Map<String, Long> overspend;

  private BackendApi backendApi;
  private int tx0Delay;
  private int tx0MinConfirmations;
  private int refreshUtxoDelay;
  private int refreshPoolsDelay;

  private int feeMin;
  private int feeMax;
  private int feeFallback;

  private boolean resyncOnFirstRun;

  private ISecretPointFactory secretPointFactory;

  public WhirlpoolWalletConfig(
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      TorClientService torClientService,
      ServerApi serverApi,
      NetworkParameters params,
      boolean mobile,
      BackendApi backendApi) {
    super(httpClientService, stompClientService, torClientService, serverApi, null, params, mobile);

    // default settings
    this.maxClients = 5;
    this.maxClientsPerPool = 1;
    this.liquidityClient = true;
    this.clientDelay = 30;
    this.autoTx0PoolId = null;
    this.autoTx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
    this.autoMix = false;
    this.scode = null;
    this.tx0MaxOutputs = 0;
    this.tx0FakeOutputRandomFactor = 0;
    this.tx0FakeOutputMinValue = 10000;
    this.overspend = new LinkedHashMap<String, Long>();

    // technical settings
    this.backendApi = backendApi;
    this.tx0Delay = 30;
    this.tx0MinConfirmations = 0;
    this.refreshUtxoDelay = 60; // 1min
    this.refreshPoolsDelay = 600; // 10min

    this.feeMin = 1;
    this.feeMax = 510;
    this.feeFallback = 75;

    this.resyncOnFirstRun = false;

    this.secretPointFactory = SecretPointFactoryJava.getInstance();
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

  public boolean isLiquidityClient() {
    return liquidityClient;
  }

  public void setLiquidityClient(boolean liquidityClient) {
    this.liquidityClient = liquidityClient;
  }

  public int getClientDelay() {
    return clientDelay;
  }

  public void setClientDelay(int clientDelay) {
    this.clientDelay = clientDelay;
  }

  public boolean isAutoTx0() {
    return !StringUtils.isEmpty(autoTx0PoolId);
  }

  public String getAutoTx0PoolId() {
    return autoTx0PoolId;
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

  public int getTx0FakeOutputRandomFactor() {
    return tx0FakeOutputRandomFactor;
  }

  public void setTx0FakeOutputRandomFactor(int tx0FakeOutputRandomFactor) {
    this.tx0FakeOutputRandomFactor = tx0FakeOutputRandomFactor;
  }

  public int getTx0FakeOutputMinValue() {
    return tx0FakeOutputMinValue;
  }

  public void setTx0FakeOutputMinValue(int tx0FakeOutputMinValue) {
    this.tx0FakeOutputMinValue = tx0FakeOutputMinValue;
  }

  public Long getOverspend(String poolId) {
    return overspend != null ? overspend.get(poolId) : null;
  }

  public void setOverspend(Map<String, Long> overspend) {
    this.overspend = overspend;
  }

  public BackendApi getBackendApi() {
    return backendApi;
  }

  public int getTx0Delay() {
    return tx0Delay;
  }

  public void setTx0Delay(int tx0Delay) {
    this.tx0Delay = tx0Delay;
  }

  public int getTx0MinConfirmations() {
    return tx0MinConfirmations;
  }

  public void setTx0MinConfirmations(int tx0MinConfirmations) {
    this.tx0MinConfirmations = tx0MinConfirmations;
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

  public ISecretPointFactory getSecretPointFactory() {
    return secretPointFactory;
  }

  public void setSecretPointFactory(ISecretPointFactory secretPointFactory) {
    this.secretPointFactory = secretPointFactory;
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new LinkedHashMap<String, String>();
    configInfo.put("protocolVersion", WhirlpoolProtocol.PROTOCOL_VERSION);
    configInfo.put(
        "server", getServerApi() + ", network=" + getNetworkParameters().getPaymentProtocolId());
    configInfo.put(
        "externalDestination",
        (getExternalDestination() != null ? getExternalDestination().toString() : "null"));
    configInfo.put(
        "refreshDelay",
        "refreshUtxoDelay=" + refreshUtxoDelay + ", refreshPoolsDelay=" + refreshPoolsDelay);
    configInfo.put(
        "mix",
        "mobile="
            + isMobile()
            + ", maxClients="
            + getMaxClients()
            + ", maxClientsPerPool="
            + getMaxClientsPerPool()
            + ", liquidityClient="
            + isLiquidityClient()
            + ", clientDelay="
            + getClientDelay()
            + ", tx0Delay="
            + getTx0Delay()
            + ", autoTx0="
            + (isAutoTx0() ? getAutoTx0PoolId() : "false")
            + ", autoTx0FeeTarget="
            + getAutoTx0FeeTarget().name()
            + ", autoMix="
            + isAutoMix()
            + ", scode="
            + (scode != null ? ClientUtils.maskString(scode) : "null")
            + ", tx0MaxOutputs="
            + tx0MaxOutputs
            + ", tx0FakeOutputRandomFactor="
            + tx0FakeOutputRandomFactor
            + ", tx0FakeOutputMinValue="
            + tx0FakeOutputMinValue
            + ", overspend="
            + (overspend != null ? overspend.toString() : "null"));
    configInfo.put(
        "fee", "fallback=" + getFeeFallback() + ", min=" + getFeeMin() + ", max=" + getFeeMax());
    configInfo.put("resyncOnFirstRun", Boolean.toString(resyncOnFirstRun));
    return configInfo;
  }
}
