package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Coordinator {
  private final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private String coordinatorId;
  private PaymentCode paymentCode;
  private Collection<String> poolIds;

  public Coordinator(String coordinatorId, PaymentCode paymentCode, Collection<String> poolIds) {
    this.coordinatorId = coordinatorId;
    this.paymentCode = paymentCode;
    this.poolIds = poolIds;
  }

  public String getCoordinatorId() {
    return coordinatorId;
  }

  public PaymentCode getPaymentCode() {
    return paymentCode;
  }

  public Collection<String> getPoolIds() {
    return poolIds;
  }

  @Override
  public String toString() {
    return "coordinatorId='" + coordinatorId + '\'' + ", paymentCode=" + paymentCode;
  }
}
