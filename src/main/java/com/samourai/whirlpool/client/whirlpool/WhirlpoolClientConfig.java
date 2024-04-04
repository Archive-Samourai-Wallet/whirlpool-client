package com.samourai.whirlpool.client.whirlpool;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;

public class WhirlpoolClientConfig {
  private ExternalDestination externalDestination;
  private IndexRange indexRangePostmix;
  private ClientCryptoService clientCryptoService;
  private SorobanConfig sorobanConfig;

  public WhirlpoolClientConfig(
      SorobanConfig sorobanConfig,
      ExternalDestination externalDestination,
      IndexRange indexRangePostmix,
      ClientCryptoService clientCryptoService) {
    this.sorobanConfig = sorobanConfig;
    this.externalDestination = externalDestination;
    this.indexRangePostmix = indexRangePostmix;
    this.clientCryptoService = clientCryptoService;
  }

  public WhirlpoolClientConfig(
      SorobanConfig sorobanConfig,
      ExternalDestination externalDestination,
      IndexRange indexRangePostmix) {
    this(sorobanConfig, externalDestination, indexRangePostmix, new ClientCryptoService());
  }

  public SorobanConfig getSorobanConfig() {
    return sorobanConfig;
  }

  public void setSorobanConfig(SorobanConfig sorobanConfig) {
    this.sorobanConfig = sorobanConfig;
  }

  public SamouraiNetwork getSamouraiNetwork() {
    return sorobanConfig.getExtLibJConfig().getSamouraiNetwork();
  }

  public ExternalDestination getExternalDestination() {
    return externalDestination;
  }

  public void setExternalDestination(ExternalDestination externalDestination) {
    this.externalDestination = externalDestination;
  }

  public IndexRange getIndexRangePostmix() {
    return indexRangePostmix;
  }

  public void setIndexRangePostmix(IndexRange indexRangePostmix) {
    this.indexRangePostmix = indexRangePostmix;
  }

  public ClientCryptoService getClientCryptoService() {
    return clientCryptoService;
  }

  public void setClientCryptoService(ClientCryptoService clientCryptoService) {
    this.clientCryptoService = clientCryptoService;
  }
}
