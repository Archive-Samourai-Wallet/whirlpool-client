package com.samourai.whirlpool.client.whirlpool;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;

public class WhirlpoolClientConfig {
  private IHttpClientService httpClientService;
  private BIP47UtilGeneric bip47Util;
  private CryptoUtil cryptoUtil;
  private ExternalDestination externalDestination;
  private SamouraiNetwork samouraiNetwork;
  private IndexRange indexRangePostmix;
  private boolean torOnionCoordinator;

  private ClientCryptoService clientCryptoService;

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      BIP47UtilGeneric bip47Util,
      CryptoUtil cryptoUtil,
      ExternalDestination externalDestination,
      SamouraiNetwork samouraiNetwork,
      IndexRange indexRangePostmix,
      boolean torOnionCoordinator) {
    this.httpClientService = httpClientService;
    this.bip47Util = bip47Util;
    this.cryptoUtil = cryptoUtil;
    this.externalDestination = externalDestination;
    this.samouraiNetwork = samouraiNetwork;
    this.indexRangePostmix = indexRangePostmix;
    this.torOnionCoordinator = torOnionCoordinator;
    this.clientCryptoService = new ClientCryptoService();
  }

  public IHttpClientService getHttpClientService() {
    return httpClientService;
  }

  public IHttpClient getHttpClient(HttpUsage httpUsage) {
    return httpClientService.getHttpClient(httpUsage);
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

  public SamouraiNetwork getSamouraiNetwork() {
    return samouraiNetwork;
  }

  public void setSamouraiNetwork(SamouraiNetwork samouraiNetwork) {
    this.samouraiNetwork = samouraiNetwork;
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

  public ClientCryptoService getClientCryptoService() {
    return clientCryptoService;
  }
}
