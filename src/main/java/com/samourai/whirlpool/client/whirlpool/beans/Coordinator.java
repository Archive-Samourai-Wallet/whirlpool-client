package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Coordinator {
  private final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private String name;
  private PaymentCode sender;
  private Collection<String> poolIds;
  private Collection<String> sorobanNodeUrls;

  public Coordinator(
      String name,
      PaymentCode sender,
      Collection<String> poolIds,
      Collection<String> sorobanNodeUrls) {
    this.name = name;
    this.sender = sender;
    this.poolIds = poolIds;
    this.sorobanNodeUrls = sorobanNodeUrls;
  }

  public String getName() {
    return name;
  }

  public PaymentCode getSender() {
    return sender;
  }

  public Collection<String> getPoolIds() {
    return poolIds;
  }

  public Collection<String> getSorobanNodeUrls() {
    return sorobanNodeUrls;
  }

  @Override
  public String toString() {
    return "name='" + name + '\'' + ", sender=" + sender + ", sorobanNodeUrls=" + sorobanNodeUrls;
  }
}
