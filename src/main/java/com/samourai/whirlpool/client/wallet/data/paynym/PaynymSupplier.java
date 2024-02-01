package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.Completable;

public interface PaynymSupplier {
  PaymentCode getPaymentCode();

  Completable claim() throws Exception;

  Completable follow(String paymentCodeTarget) throws Exception;

  Completable unfollow(String paymentCodeTarget) throws Exception;

  PaynymState getPaynymState();

  void refresh() throws Exception;

  void load() throws Exception;
}
