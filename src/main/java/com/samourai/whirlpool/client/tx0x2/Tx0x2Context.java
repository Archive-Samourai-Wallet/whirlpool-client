package com.samourai.whirlpool.client.tx0x2;

import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Context extends CahootsContext {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Context.class);

  private Tx0Service tx0Service;
  private Tx0 tx0Initiator; // only set for initiator

  protected Tx0x2Context(
      CahootsWallet cahootsWallet,
      CahootsTypeUser typeUser,
      int account,
      Long feePerB,
      Tx0Service tx0Service,
      Tx0 tx0Initiator) {
    super(
        cahootsWallet,
        typeUser,
        CahootsType.TX0X2,
        account,
        feePerB,
        tx0Initiator != null ? tx0Initiator.getSpendValue() - tx0Initiator.getTx0MinerFee() : null,
        null);
    this.tx0Service = tx0Service;
    this.tx0Initiator = tx0Initiator;
  }

  public static Tx0x2Context newInitiator(
      CahootsWallet cahootsWallet,
      int account,
      long feePerB,
      Tx0Service tx0Service,
      Tx0 tx0Initiator) {
    return new Tx0x2Context(
        cahootsWallet, CahootsTypeUser.SENDER, account, feePerB, tx0Service, tx0Initiator);
  }

  public static Tx0x2Context newCounterparty(
      CahootsWallet cahootsWallet, int account, Tx0Service tx0Service) {
    return new Tx0x2Context(
        cahootsWallet, CahootsTypeUser.COUNTERPARTY, account, null, tx0Service, null);
  }

  public Tx0Service getTx0Service() {
    return tx0Service;
  }

  public Tx0 getTx0Initiator() {
    return tx0Initiator;
  }
}
