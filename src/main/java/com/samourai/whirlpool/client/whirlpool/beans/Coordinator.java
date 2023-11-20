package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Coordinator {
  private final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private String coordinatorId;
  private PaymentCode paymentCode;
  private String urlClear;
  private String urlOnion;
  private Collection<String> poolIds;

  public Coordinator(
      String coordinatorId,
      PaymentCode paymentCode,
      String urlClear,
      String urlOnion,
      Collection<String> poolIds) {
    this.coordinatorId = coordinatorId;
    this.paymentCode = paymentCode;
    this.urlClear = urlClear;
    this.urlOnion = urlOnion;
    this.poolIds = poolIds;
  }

  public String getCoordinatorId() {
    return coordinatorId;
  }

  public PaymentCode getPaymentCode() {
    return paymentCode;
  }

  public String getUrlClear() {
    return urlClear;
  }

  public String getUrlOnion() {
    return urlOnion;
  }

  public Collection<String> getPoolIds() {
    return poolIds;
  }

  @Override
  public String toString() {
    return "coordinatorId='"
        + coordinatorId
        + '\''
        + ", paymentCode="
        + paymentCode
        + ", urlClear='"
        + urlClear
        + '\''
        + ", urlOnion='"
        + urlOnion;
  }
}
