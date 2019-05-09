package com.samourai.api.client;

import com.samourai.api.client.beans.MultiAddrResponse;
import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.http.client.HttpException;
import com.samourai.http.client.IHttpClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.pushTx.AbstractPushTxService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamouraiApi extends AbstractPushTxService {
  private Logger log = LoggerFactory.getLogger(SamouraiApi.class);

  // samourai backend urls
  private static final String URL_BACKEND_TESTNET = "https://api.samouraiwallet.com/test";
  private static final String URL_BACKEND_MAINNET = "https://api.samouraiwallet.com";

  private static final String URL_UNSPENT = "/v2/unspent?active=";
  private static final String URL_MULTIADDR = "/v2/multiaddr?active=";
  private static final String URL_INIT_BIP84 = "/v2/xpub";
  private static final String URL_FEES = "/v2/fees";
  private static final String URL_PUSHTX = "/v2/pushtx/";

  private IHttpClient httpClient;
  private String urlBackend;

  public SamouraiApi(IHttpClient httpClient, boolean testnet) {
    this(httpClient, testnet ? URL_BACKEND_TESTNET : URL_BACKEND_MAINNET);
  }

  public SamouraiApi(IHttpClient httpClient, String urlBackend) {
    this.httpClient = httpClient;
    this.urlBackend = urlBackend;
  }

  public List<UnspentResponse.UnspentOutput> fetchUtxos(String zpub) throws Exception {
    String url = urlBackend + URL_UNSPENT + zpub;
    if (log.isDebugEnabled()) {
      log.debug("fetchUtxos: " + url);
    }
    UnspentResponse unspentResponse = httpClient.parseJson(url, UnspentResponse.class);
    List<UnspentResponse.UnspentOutput> unspentOutputs =
        new ArrayList<UnspentResponse.UnspentOutput>();
    if (unspentResponse.unspent_outputs != null) {
      unspentOutputs = Arrays.asList(unspentResponse.unspent_outputs);
    }
    return unspentOutputs;
  }

  public List<MultiAddrResponse.Address> fetchAddresses(String zpub) throws Exception {
    String url = urlBackend + URL_MULTIADDR + zpub;
    if (log.isDebugEnabled()) {
      log.debug("fetchAddress: " + url);
    }
    MultiAddrResponse multiAddrResponse = httpClient.parseJson(url, MultiAddrResponse.class);
    List<MultiAddrResponse.Address> addresses = new ArrayList<MultiAddrResponse.Address>();
    if (multiAddrResponse.addresses != null) {
      addresses = Arrays.asList(multiAddrResponse.addresses);
    }
    return addresses;
  }

  public MultiAddrResponse.Address fetchAddress(String zpub) throws Exception {
    List<MultiAddrResponse.Address> addresses = fetchAddresses(zpub);
    if (addresses.size() != 1) {
      throw new Exception("Address count=" + addresses.size());
    }
    MultiAddrResponse.Address address = addresses.get(0);

    if (log.isDebugEnabled()) {
      log.debug(
          "fetchAddress "
              + zpub
              + ": account_index="
              + address.account_index
              + ", change_index="
              + address.change_index);
    }
    return address;
  }

  public void initBip84(String zpub) throws Exception {
    String url = urlBackend + URL_INIT_BIP84;
    if (log.isDebugEnabled()) {
      log.debug("initBip84: zpub=" + zpub);
    }
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("xpub", zpub);
    postBody.put("type", "new");
    postBody.put("segwit", "bip84");
    httpClient.postUrlEncoded(url, postBody);
  }

  public SamouraiFee fetchFees() throws Exception {
    String url = urlBackend + URL_FEES;
    Map<String, Integer> feeResponse = httpClient.parseJson(url, Map.class);
    if (feeResponse == null) {
      throw new Exception("Invalid fee response from server");
    }
    return new SamouraiFee(feeResponse);
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("pushTx... " + txHex);
    } else {
      log.info("pushTx...");
    }
    String url = urlBackend + URL_PUSHTX;
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("tx", txHex);
    try {
      httpClient.postUrlEncoded(url, postBody);
    } catch (HttpException e) {
      if (log.isDebugEnabled()) {
        log.error("pushTx failed", e);
      }
      log.error(
          "PushTx failed: response="
              + e.getResponseBody()
              + ". error="
              + e.getMessage()
              + " for txHex="
              + txHex);
      throw new NotifiableException(
          "PushTx failed (" + e.getResponseBody() + ") for txHex=" + txHex);
    }
  }

  @Override
  public boolean testConnectivity() {
    try {
      fetchFees();
      return true;
    } catch (Exception e) {
      log.error("", e);
      return false;
    }
  }
}
