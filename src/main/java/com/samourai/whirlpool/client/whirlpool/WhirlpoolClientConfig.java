package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import org.bitcoinj.core.NetworkParameters;

public class WhirlpoolClientConfig {
  private IHttpClientService httpClientService;
  private IStompClientService stompClientService;
  private ServerApi serverApi;
  private NetworkParameters networkParameters;
  private boolean mobile;
  private int reconnectDelay;
  private int reconnectUntil;

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      ServerApi serverApi,
      NetworkParameters networkParameters,
      boolean mobile) {
    this(httpClientService, stompClientService, serverApi, networkParameters, mobile, 5, 500);
  }

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      ServerApi serverApi,
      NetworkParameters networkParameters,
      boolean mobile,
      int reconnectDelay,
      int reconnectUntil) {
    this.httpClientService = httpClientService;
    this.stompClientService = stompClientService;
    this.serverApi = serverApi;
    this.networkParameters = networkParameters;
    this.mobile = mobile;
    this.reconnectDelay = reconnectDelay;
    this.reconnectUntil = reconnectUntil;
  }

  public IHttpClient getHttpClient(HttpUsage httpUsage) {
    return httpClientService.getHttpClient(httpUsage);
  }

  public IStompClientService getStompClientService() {
    return stompClientService;
  }

  public ServerApi getServerApi() {
    return serverApi;
  }

  public void setServerApi(ServerApi serverApi) {
    this.serverApi = serverApi;
  }

  public NetworkParameters getNetworkParameters() {
    return networkParameters;
  }

  public void setNetworkParameters(NetworkParameters networkParameters) {
    this.networkParameters = networkParameters;
  }

  public boolean isMobile() {
    return mobile;
  }

  public void setMobile(boolean mobile) {
    this.mobile = mobile;
  }

  public int getReconnectDelay() {
    return reconnectDelay;
  }

  public void setReconnectDelay(int reconnectDelay) {
    this.reconnectDelay = reconnectDelay;
  }

  public int getReconnectUntil() {
    return reconnectUntil;
  }

  public void setReconnectUntil(int reconnectUntil) {
    this.reconnectUntil = reconnectUntil;
  }
}
