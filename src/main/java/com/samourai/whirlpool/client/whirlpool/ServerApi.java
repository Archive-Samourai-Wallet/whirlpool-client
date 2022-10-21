package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IWhirlpoolHttpClientService;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.whirlpool.client.exception.PushTxErrorResponseException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.*;
import io.reactivex.Single;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Whirlpool server API */
public class ServerApi {
  private Logger log = LoggerFactory.getLogger(ServerApi.class);

  private String urlServer;
  private IHttpClient httpClientRest;
  private IHttpClient httpClientRegOutput;

  public ServerApi(String urlServer, IWhirlpoolHttpClientService httpClientService) {
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
    return httpClientRest.getJson(url, PoolsResponse.class, null);
  }

  public Single<Optional<Tx0DataResponseV2>> fetchTx0Data(
      Tx0DataRequestV2 tx0DataRequest, boolean opReturnV0) throws Exception {
    httpClientRest.connect();
    String url = WhirlpoolProtocol.getUrlTx0Data(urlServer, opReturnV0);
    if (log.isDebugEnabled()) {
      log.debug("POST " + url + ": " + ClientUtils.toJsonString(tx0DataRequest));
    }
    return httpClientRest.postJson(url, Tx0DataResponseV2.class, null, tx0DataRequest);
  }

  public String getWsUrlConnect() {
    return WhirlpoolProtocol.getUrlConnect(urlServer);
  }

  public Single<Optional<String>> checkOutput(CheckOutputRequest checkOutputRequest)
      throws Exception {
    // POST request through a different identity for mix privacy
    httpClientRegOutput.connect();

    String checkOutputUrl = WhirlpoolProtocol.getUrlCheckOutput(urlServer);
    if (log.isDebugEnabled()) {
      log.debug("POST " + checkOutputUrl + ": " + ClientUtils.toJsonString(checkOutputRequest));
    }
    return httpClientRegOutput.postJson(checkOutputUrl, String.class, null, checkOutputRequest);
  }

  public Single<Optional<String>> registerOutput(RegisterOutputRequest registerOutputRequest)
      throws Exception {
    // POST request through a different identity for mix privacy
    httpClientRegOutput.connect();

    String registerOutputUrl = WhirlpoolProtocol.getUrlRegisterOutput(urlServer);
    if (log.isDebugEnabled()) {
      log.debug(
          "POST " + registerOutputUrl + ": " + ClientUtils.toJsonString(registerOutputRequest));
    }
    return httpClientRegOutput.postJson(
        registerOutputUrl, String.class, null, registerOutputRequest);
  }

  public Single<PushTxSuccessResponse> pushTx0(Tx0PushRequest request) throws Exception {
    httpClientRest.connect();

    String url = WhirlpoolProtocol.getUrlTx0Push(urlServer);
    if (log.isDebugEnabled()) {
      log.debug("POST " + url + ": " + ClientUtils.toJsonString(request));
    }
    return httpClientRest
        .postJson(url, PushTxSuccessResponse.class, null, request)
        .map(o -> o.get())
        .onErrorResumeNext(
            throwable -> {
              return Single.error(responseError(throwable));
            });
  }

  protected Throwable responseError(Throwable e) {
    if (e instanceof HttpException) {
      String responseBody = ((HttpException) e).getResponseBody();
      try {
        PushTxErrorResponse pushTxErrorResponse =
            ClientUtils.fromJson(responseBody, PushTxErrorResponse.class);
        if (!StringUtils.isEmpty(
            pushTxErrorResponse.pushTxErrorCode)) { // skip false-positive NotifiableException
          return new PushTxErrorResponseException(pushTxErrorResponse);
        }
      } catch (Exception ee) {
        log.error("Not a pushTxErrorResponse: " + responseBody, ee);
      }
    }
    return e;
  }

  public String toString() {
    return "urlServer=" + urlServer;
  }
}
