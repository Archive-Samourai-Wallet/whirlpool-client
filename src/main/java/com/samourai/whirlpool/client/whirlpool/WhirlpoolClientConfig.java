package com.samourai.whirlpool.client.whirlpool;

import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.constants.WhirlpoolNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.httpClient.IHttpProxyService;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.SorobanAppWhirlpool;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import java.util.Collection;

public class WhirlpoolClientConfig {
  private IHttpClientService httpClientService;
  private IHttpProxyService httpProxyService;
  private RpcClientService rpcClientService;
  private BIP47UtilGeneric bip47Util;
  private CryptoUtil cryptoUtil;
  private ExternalDestination externalDestination;
  private WhirlpoolNetwork whirlpoolNetwork;
  private IndexRange indexRangePostmix;
  private boolean torOnionCoordinator;

  private SorobanAppWhirlpool sorobanAppWhirlpool;
  private ClientCryptoService clientCryptoService;

  public WhirlpoolClientConfig(
      IHttpClientService httpClientService,
      IHttpProxyService httpProxyService,
      RpcClientService rpcClientService,
      BIP47UtilGeneric bip47Util,
      CryptoUtil cryptoUtil,
      ExternalDestination externalDestination,
      WhirlpoolNetwork whirlpoolNetwork,
      IndexRange indexRangePostmix,
      boolean torOnionCoordinator) {
    this.httpClientService = httpClientService;
    this.httpProxyService = httpProxyService;
    this.rpcClientService = rpcClientService;
    this.bip47Util = bip47Util;
    this.cryptoUtil = cryptoUtil;
    this.externalDestination = externalDestination;
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.indexRangePostmix = indexRangePostmix;
    this.torOnionCoordinator = torOnionCoordinator;
    this.sorobanAppWhirlpool = new SorobanAppWhirlpool(whirlpoolNetwork);

    this.clientCryptoService = new ClientCryptoService();
  }

  public IHttpClientService getHttpClientService() {
    return httpClientService;
  }

  public IHttpClient getHttpClient(HttpUsage httpUsage) {
    return httpClientService.getHttpClient(httpUsage);
  }

  public IHttpProxyService getHttpProxyService() {
    return httpProxyService;
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

  public void setHttpProxyService(IHttpProxyService httpProxyService) {
    this.httpProxyService = httpProxyService;
  }

  public SorobanAppWhirlpool getSorobanAppWhirlpool() {
    return sorobanAppWhirlpool;
  }

  public WhirlpoolApiClient createWhirlpoolApiClient(CoordinatorSupplier coordinatorSupplier) {
    RpcSessionClient rpcSession = createRpcSession(coordinatorSupplier);
    return new WhirlpoolApiClient(rpcSession, sorobanAppWhirlpool);
  }

  public RpcSessionClient createRpcSession(CoordinatorSupplier coordinatorSupplierOrNull) {
    RpcSessionClient rpcSession =
        new RpcSessionClient(getRpcClientService().generateRpcWallet().createRpcSession());
    if (coordinatorSupplierOrNull != null) {
      Collection<Coordinator> coordinators = coordinatorSupplierOrNull.getCoordinators(); // TODO
      if (!coordinators.isEmpty()) {
        rpcSession.setCoordinator(coordinators.iterator().next());
      }
    }
    return rpcSession;
  }

  public ClientCryptoService getClientCryptoService() {
    return clientCryptoService;
  }
}
