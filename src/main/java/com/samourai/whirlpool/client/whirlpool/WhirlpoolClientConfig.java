package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;

public class WhirlpoolClientConfig {
  private IHttpClientService httpClientService;
  private IStompClientService stompClientService;
  private TorClientService torClientService;
  private RpcClientService rpcClientService;
  private SorobanClientApi sorobanClientApi;
  private BIP47UtilGeneric bip47Util;
  private CryptoUtil cryptoUtil;
  private ExternalDestination externalDestination;
  private WhirlpoolNetwork whirlpoolNetwork;
  private IndexRange indexRangePostmix;
  private boolean torOnionCoordinator;

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      TorClientService torClientService,
      RpcClientService rpcClientService,
      SorobanClientApi sorobanClientApi,
      BIP47UtilGeneric bip47Util,
      CryptoUtil cryptoUtil,
      ExternalDestination externalDestination,
      WhirlpoolNetwork whirlpoolNetwork,
      IndexRange indexRangePostmix,
      boolean torOnionCoordinator) {
    this.httpClientService = httpClientService;
    this.stompClientService = stompClientService;
    this.torClientService = torClientService;
    this.rpcClientService = rpcClientService;
    this.sorobanClientApi = sorobanClientApi;
    this.bip47Util = bip47Util;
    this.cryptoUtil = cryptoUtil;
    this.externalDestination = externalDestination;
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.indexRangePostmix = indexRangePostmix;
    this.torOnionCoordinator = torOnionCoordinator;
  }

  public IHttpClientService getHttpClientService() {
    return httpClientService;
  }

  public IHttpClient getHttpClient(HttpUsage httpUsage) {
    return httpClientService.getHttpClient(httpUsage);
  }

  public IStompClientService getStompClientService() {
    return stompClientService;
  }

  public TorClientService getTorClientService() {
    return torClientService;
  }

  public RpcSession getRpcSession() {
    return rpcClientService.getRpcSession("whirlpoolClient");
  }

  public SorobanClientApi getSorobanClientApi() {
    return sorobanClientApi;
  }

  public void setSorobanClientApi(SorobanClientApi sorobanClientApi) {
    this.sorobanClientApi = sorobanClientApi;
  }

  public BIP47UtilGeneric getBip47Util() {
    return bip47Util;
  }

  public void setBip47Util(BIP47UtilGeneric bip47Util) {
    this.bip47Util = bip47Util;
  }

  public CryptoUtil getCryptoUtil() {
    return cryptoUtil;
  }

  public void setCryptoUtil(CryptoUtil cryptoUtil) {
    this.cryptoUtil = cryptoUtil;
  }

  public ExternalDestination getExternalDestination() {
    return externalDestination;
  }

  public void setExternalDestination(ExternalDestination externalDestination) {
    this.externalDestination = externalDestination;
  }

  public WhirlpoolNetwork getWhirlpoolNetwork() {
    return whirlpoolNetwork;
  }

  public void setWhirlpoolNetwork(WhirlpoolNetwork whirlpoolNetwork) {
    this.whirlpoolNetwork = whirlpoolNetwork;
  }

  public IndexRange getIndexRangePostmix() {
    return indexRangePostmix;
  }

  public void setIndexRangePostmix(IndexRange indexRangePostmix) {
    this.indexRangePostmix = indexRangePostmix;
  }

  public boolean isTorOnionCoordinator() {
    return torOnionCoordinator;
  }

  public void setTorClientService(TorClientService torClientService) {
    this.torClientService = torClientService;
  }
}
