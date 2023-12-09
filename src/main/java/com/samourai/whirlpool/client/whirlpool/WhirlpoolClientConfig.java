package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import com.samourai.whirlpool.protocol.SorobanProtocolWhirlpool;

public class WhirlpoolClientConfig {
  private IHttpClientService httpClientService;
  private TorClientService torClientService;
  private RpcClientService rpcClientService;
  private BIP47UtilGeneric bip47Util;
  private CryptoUtil cryptoUtil;
  private ExternalDestination externalDestination;
  private WhirlpoolNetwork whirlpoolNetwork;
  private IndexRange indexRangePostmix;
  private boolean torOnionCoordinator;

  private SorobanProtocolWhirlpool sorobanProtocolWhirlpool;
  private ClientCryptoService clientCryptoService;

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      TorClientService torClientService,
      RpcClientService rpcClientService,
      BIP47UtilGeneric bip47Util,
      CryptoUtil cryptoUtil,
      ExternalDestination externalDestination,
      WhirlpoolNetwork whirlpoolNetwork,
      IndexRange indexRangePostmix,
      boolean torOnionCoordinator) {
    this.httpClientService = httpClientService;
    this.torClientService = torClientService;
    this.rpcClientService = rpcClientService;
    this.bip47Util = bip47Util;
    this.cryptoUtil = cryptoUtil;
    this.externalDestination = externalDestination;
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.indexRangePostmix = indexRangePostmix;
    this.torOnionCoordinator = torOnionCoordinator;

    this.sorobanProtocolWhirlpool = new SorobanProtocolWhirlpool(whirlpoolNetwork);
    this.clientCryptoService = new ClientCryptoService();
  }

  public IHttpClientService getHttpClientService() {
    return httpClientService;
  }

  public IHttpClient getHttpClient(HttpUsage httpUsage) {
    return httpClientService.getHttpClient(httpUsage);
  }

  public TorClientService getTorClientService() {
    return torClientService;
  }

  public RpcClientService getRpcClientService() {
    return rpcClientService;
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

  public SorobanProtocolWhirlpool getSorobanProtocolWhirlpool() {
    return sorobanProtocolWhirlpool;
  }

  public ClientCryptoService getClientCryptoService() {
    return clientCryptoService;
  }
}
