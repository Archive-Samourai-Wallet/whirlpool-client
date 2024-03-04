package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.bipWallet.BipWalletSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.websocketClient.IWebsocketClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class DojoDataSourceFactory implements DataSourceFactory {
  private String dojoUrl;
  private String dojoApiKey; // may be null
  private IWebsocketClient wsClient; // may be null
  private BipWalletSupplier bipWalletSupplier;

  public DojoDataSourceFactory(
      String dojoUrl,
      String dojoApiKey,
      final IWebsocketClient wsClient,
      BipWalletSupplier bipWalletSupplier) {
    this.dojoUrl = dojoUrl;
    this.dojoApiKey = dojoApiKey;
    this.wsClient = wsClient;
    this.bipWalletSupplier = bipWalletSupplier;
  }

  // Samourai backend
  public DojoDataSourceFactory(
      BackendServer backendServer,
      boolean onion,
      final IWebsocketClient wsClient,
      BipWalletSupplier bipWalletSupplier) {
    this(backendServer.getBackendUrl(onion), null, wsClient, bipWalletSupplier);
  }

  // overridable
  protected String computeDojoApiKey(
      WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, String passphrase) throws Exception {
    return this.dojoApiKey;
  }

  @Override
  public DataSource createDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      String passphrase,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier)
      throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    IHttpClient httpClientBackend = config.getHttpClient(HttpUsage.BACKEND);

    // configure backendApi
    String myDojoApiKey = computeDojoApiKey(whirlpoolWallet, bip44w, passphrase);
    BackendApi backendApi;
    if (myDojoApiKey != null) {
      // dojo
      backendApi = BackendApi.newBackendApiDojo(httpClientBackend, dojoUrl, myDojoApiKey);
    } else {
      // samourai
      backendApi = BackendApi.newBackendApiSamourai(httpClientBackend, dojoUrl);
    }

    // configure backendWsApi
    BackendWsApi backendWsApi = wsClient != null ? backendApi.newBackendWsApi(wsClient) : null;
    checkConnectivity(backendApi, backendWsApi);

    return new DojoDataSource(
        whirlpoolWallet,
        bip44w,
        walletStateSupplier,
        utxoConfigSupplier,
        bipWalletSupplier,
        backendApi,
        backendWsApi);
  }

  protected void checkConnectivity(BackendApi backendApi, BackendWsApi backendWsApi)
      throws Exception {
    if (!backendApi.testConnectivity()) {
      throw new NotifiableException(
          "Unable to connect to wallet backend: " + backendApi.getUrlBackend());
    }
  }
}
