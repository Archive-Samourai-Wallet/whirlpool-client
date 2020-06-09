package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import com.samourai.whirlpool.protocol.rest.RegisterOutputRequest;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponse;
import io.reactivex.Observable;
import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Whirlpool server API */
public class ServerApi {
  private Logger log = LoggerFactory.getLogger(BackendApi.class);

  private String urlServer;
  private IHttpClient httpClientRest;
  private IHttpClient httpClientRegOutput;

  public ServerApi(String urlServer, IHttpClientService httpClientService) {
    this(
        urlServer,
        httpClientService.getHttpClient(HttpUsage.COORDINATOR_REST),
        httpClientService.getHttpClient(HttpUsage.COORDINATOR_REGISTER_OUTPUT));
  }

  public ServerApi(String urlServer, IHttpClient httpClientRest, IHttpClient httpClientRegOutput) {
    this.urlServer = urlServer;
    this.httpClientRest = httpClientRest;
    this.httpClientRegOutput = httpClientRegOutput;
  }

  public PoolsResponse fetchPools() throws Exception {
    String url = WhirlpoolProtocol.getUrlFetchPools(urlServer);
    if (log.isDebugEnabled()) {
      log.debug("fetchPools: " + url);
    }
    httpClientRest.connect();
    PoolsResponse poolsResponse = httpClientRest.getJson(url, PoolsResponse.class, null);
    return poolsResponse;
  }

  public Tx0DataResponse fetchTx0Data(String poolId, String scode) throws Exception {
    String url = WhirlpoolProtocol.getUrlTx0Data(urlServer, poolId, scode);
    Tx0DataResponse tx0Response = httpClientRest.getJson(url, Tx0DataResponse.class, null);
    return tx0Response;
  }

  public String getWsUrlConnect() {
    return WhirlpoolProtocol.getUrlConnect(urlServer);
  }

  public Observable<Optional<String>> registerOutput(RegisterOutputRequest registerOutputRequest)
      throws Exception {
    // POST request through a different identity for mix privacy
    httpClientRegOutput.connect();

    String registerOutputUrl = WhirlpoolProtocol.getUrlRegisterOutput(urlServer);
    if (log.isDebugEnabled()) {
      log.debug(
          "POST " + registerOutputUrl + ": " + ClientUtils.toJsonString(registerOutputRequest));
    }
    Observable<Optional<String>> observable =
        httpClientRegOutput.postJson(registerOutputUrl, String.class, null, registerOutputRequest);
    return observable;
  }

  public String toString() {
    return "urlServer=" + urlServer;
  }
}