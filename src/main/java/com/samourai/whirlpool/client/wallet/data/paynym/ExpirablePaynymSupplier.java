package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.api.paynym.PaynymApi;
import com.samourai.wallet.api.paynym.PaynymServer;
import com.samourai.wallet.api.paynym.beans.*;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.event.PaynymChangeEvent;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirablePaynymSupplier extends ExpirableSupplier<PaynymState>
    implements PaynymSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirablePaynymSupplier.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private BIP47Wallet bip47Wallet;
  private PaynymApi paynymApi;
  private WalletStateSupplier walletStateSupplier;

  private String paymentCode;
  private String token;

  public ExpirablePaynymSupplier(
      int refreshDelay,
      BIP47Wallet bip47Wallet,
      PaynymApi paynymApi,
      WalletStateSupplier walletStateSupplier) {
    super(refreshDelay, log);
    this.bip47Wallet = bip47Wallet;
    this.paynymApi = paynymApi;
    this.walletStateSupplier = walletStateSupplier;

    this.paymentCode = bip47Wallet.getAccount(0).getPaymentCode();
    this.token = null;
  }

  public static ExpirablePaynymSupplier create(
      WhirlpoolWalletConfig config, HD_Wallet bip44w, WalletStateSupplier walletStateSupplier) {
    int refreshPaynymDelay = config.getRefreshPaynymDelay();
    PaynymApi paynymApi = computePaynymApi(config);
    BIP47Wallet bip47Wallet = new BIP47Wallet(bip44w);
    return new ExpirablePaynymSupplier(
        refreshPaynymDelay, bip47Wallet, paynymApi, walletStateSupplier);
  }

  protected static PaynymApi computePaynymApi(WhirlpoolWalletConfig config) {
    IHttpClient httpClient = config.getHttpClient(HttpUsage.BACKEND);
    String serverUrl = PaynymServer.get().getUrl();
    return new PaynymApi(httpClient, serverUrl, config.getBip47Util());
  }

  @Override
  protected PaynymState fetch() throws Exception {
    if (!walletStateSupplier.isNymClaimed()) {
      if (log.isDebugEnabled()) {
        log.debug("not fetching Paynym: not claimed yet");
      }
      return new PaynymState(getPaymentCode());
    }

    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    try {
      return asyncUtil.blockingSingle(
          paynymApi
              .getNymInfo(paymentCode)
              .map(
                  nymInfoResponse ->
                      new PaynymState(
                          paymentCode,
                          nymInfoResponse.nymID,
                          nymInfoResponse.nymName,
                          nymInfoResponse.nymAvatar,
                          nymInfoResponse.segwit,
                          nymInfoResponse.following,
                          nymInfoResponse.followers)));
    } catch (Exception e) {
      // forward paynym error
      throw new NotifiableException(e.getMessage());
    }
  }

  @Override
  protected void validate(PaynymState value) throws Exception {
    // nothing to do
  }

  @Override
  protected void onValueChange(PaynymState value) {
    WhirlpoolEventService.getInstance().post(new PaynymChangeEvent(value));
  }

  protected synchronized String getToken() throws Exception {
    if (token == null) {
      token = asyncUtil.blockingSingle(paynymApi.getToken(paymentCode));
    }
    return token;
  }

  @Override
  public Completable claim() throws Exception {
    // create
    return Completable.fromObservable(
        paynymApi
            .createPaynym(paymentCode)
            .doOnComplete(
                () -> {
                  String myToken = getToken();

                  // claim
                  asyncUtil.blockingSingle(paynymApi.claim(myToken, bip47Wallet));
                })
            .doOnComplete(
                () -> {
                  String myToken = getToken();

                  // add
                  asyncUtil.blockingSingle(paynymApi.addPaynym(myToken, bip47Wallet));
                })
            .doOnComplete(
                () -> {
                  // set claimed state
                  walletStateSupplier.setNymClaimed(true);
                  refresh();
                }));
  }

  @Override
  public Completable follow(String paymentCodeTarget) throws Exception {
    return Completable.fromObservable(paynymApi.follow(getToken(), bip47Wallet, paymentCodeTarget))
        .doOnComplete(() -> refresh());
  }

  @Override
  public Completable unfollow(String paymentCodeTarget) throws Exception {
    return Completable.fromObservable(
            paynymApi.unfollow(getToken(), bip47Wallet, paymentCodeTarget))
        .doOnComplete(() -> refresh());
  }

  @Override
  public PaynymState getPaynymState() {
    return getValue();
  }

  @Override
  public String getPaymentCode() {
    return paymentCode;
  }
}