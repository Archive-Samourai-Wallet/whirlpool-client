package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.wallet.api.paynym.PaynymApi;
import com.samourai.wallet.api.paynym.PaynymServer;
import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
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

  private BIP47Account bip47Account;
  private PaynymApi paynymApi;
  private WalletStateSupplier walletStateSupplier;

  private String token;

  public ExpirablePaynymSupplier(
      int refreshDelay,
      BIP47Account bip47Account,
      PaynymApi paynymApi,
      WalletStateSupplier walletStateSupplier) {
    super(refreshDelay, log);
    this.bip47Account = bip47Account;
    this.paynymApi = paynymApi;
    this.walletStateSupplier = walletStateSupplier;
    this.token = null;
  }

  public static ExpirablePaynymSupplier create(
      WhirlpoolWalletConfig config,
      BIP47Account bip47Account,
      WalletStateSupplier walletStateSupplier) {
    int refreshPaynymDelay = config.getRefreshPaynymDelay();
    PaynymApi paynymApi = computePaynymApi(config);
    return new ExpirablePaynymSupplier(
        refreshPaynymDelay, bip47Account, paynymApi, walletStateSupplier);
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
      return asyncUtil.blockingGet(
          paynymApi
              .getNymInfo(bip47Account.getPaymentCode().toString())
              .map(
                  nymInfoResponse ->
                      new PaynymState(
                          bip47Account.getPaymentCode(),
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
      String paymentCode = bip47Account.getPaymentCode().toString();
      token = asyncUtil.blockingGet(paynymApi.getToken(paymentCode));
    }
    return token;
  }

  @Override
  public Completable claim() throws Exception {
    // create
    String paymentCode = bip47Account.getPaymentCode().toString();
    return Completable.fromSingle(
        paynymApi
            .createPaynym(paymentCode)
            .doAfterSuccess(
                single -> {
                  String myToken = getToken();

                  // claim
                  asyncUtil.blockingGet(paynymApi.claim(myToken, bip47Account));
                })
            .doAfterSuccess(
                single -> {
                  String myToken = getToken();

                  // add
                  asyncUtil.blockingGet(paynymApi.addPaynym(myToken, bip47Account));
                })
            .doAfterSuccess(
                single -> {
                  // set claimed state
                  walletStateSupplier.setNymClaimed(true);
                  refresh();
                }));
  }

  @Override
  public Completable follow(String paymentCodeTarget) throws Exception {
    return Completable.fromSingle(paynymApi.follow(getToken(), bip47Account, paymentCodeTarget))
        .doOnComplete(() -> refresh());
  }

  @Override
  public Completable unfollow(String paymentCodeTarget) throws Exception {
    return Completable.fromSingle(paynymApi.unfollow(getToken(), bip47Account, paymentCodeTarget))
        .doOnComplete(() -> refresh());
  }

  @Override
  public PaynymState getPaynymState() {
    return getValue();
  }

  @Override
  public PaymentCode getPaymentCode() {
    return bip47Account.getPaymentCode();
  }
}
