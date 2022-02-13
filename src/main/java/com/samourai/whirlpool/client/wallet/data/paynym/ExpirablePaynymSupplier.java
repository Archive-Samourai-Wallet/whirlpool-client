package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.wallet.api.paynym.PaynymApi;
import com.samourai.wallet.api.paynym.beans.*;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.whirlpool.client.event.PaynymChangeEvent;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirablePaynymSupplier extends ExpirableSupplier<PaynymState>
    implements PaynymSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirablePaynymSupplier.class);

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
      return paynymApi
          .getNymInfo(paymentCode)
          .map(
              new Function<GetNymInfoResponse, PaynymState>() {
                @Override
                public PaynymState apply(GetNymInfoResponse nymInfoResponse) {
                  return new PaynymState(
                      paymentCode,
                      nymInfoResponse.nymID,
                      nymInfoResponse.nymName,
                      nymInfoResponse.nymAvatar,
                      nymInfoResponse.segwit,
                      nymInfoResponse.following,
                      nymInfoResponse.followers);
                }
              })
          .blockingSingle();
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
      token = paynymApi.getToken(paymentCode).blockingSingle();
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
                new Action() {
                  @Override
                  public void run() throws Exception {
                    String myToken = getToken();

                    // claim
                    paynymApi.claim(myToken, bip47Wallet).blockingSingle();
                  }
                })
            .doOnComplete(
                new Action() {
                  @Override
                  public void run() throws Exception {
                    String myToken = getToken();

                    // add
                    paynymApi.addPaynym(myToken, bip47Wallet).blockingSingle();
                  }
                })
            .doOnComplete(
                new Action() {
                  @Override
                  public void run() throws Exception {
                    // set claimed state
                    walletStateSupplier.setNymClaimed(true);
                    refresh();
                  }
                }));
  }

  @Override
  public Completable follow(String paymentCodeTarget) throws Exception {
    return Completable.fromObservable(paynymApi.follow(getToken(), bip47Wallet, paymentCodeTarget))
        .doOnComplete(
            new Action() {
              @Override
              public void run() throws Exception {
                refresh();
              }
            });
  }

  @Override
  public Completable unfollow(String paymentCodeTarget) throws Exception {
    return Completable.fromObservable(
            paynymApi.unfollow(getToken(), bip47Wallet, paymentCodeTarget))
        .doOnComplete(
            new Action() {
              @Override
              public void run() throws Exception {
                refresh();
              }
            });
  }

  @Override
  public PaynymState getState() {
    return getValue();
  }

  @Override
  public String getPaymentCode() {
    return paymentCode;
  }
}
