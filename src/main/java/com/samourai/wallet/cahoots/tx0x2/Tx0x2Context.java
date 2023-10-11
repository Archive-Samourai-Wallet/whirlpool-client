package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Context extends CahootsContext {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Context.class);

  private Tx0Service tx0Service;
  private Tx0Config tx0ConfigInitiator; // only set for initiator
  private ServerApi serverApiInitiator; // only set for initiator
  private Tx0Result tx0ResultInitiator; // set at step2 for initiator

  protected Tx0x2Context(
      CahootsWallet cahootsWallet,
      CahootsTypeUser typeUser,
      int account,
      Long feePerB,
      Tx0Service tx0Service,
      Tx0Config tx0ConfigInitiator,
      ServerApi serverApiInitiator) {
    super(
        cahootsWallet,
        typeUser,
        CahootsType.TX0X2,
        account,
        feePerB,
        0L, // never used
        null);
    this.tx0Service = tx0Service;
    this.tx0ConfigInitiator = tx0ConfigInitiator;
    this.serverApiInitiator = serverApiInitiator;
    this.tx0ResultInitiator = null;
  }

  public static Tx0x2Context newInitiator(
      CahootsWallet cahootsWallet,
      int account,
      long feePerB,
      Tx0Service tx0Service,
      Tx0Config tx0Config,
      ServerApi serverApiInitiator) {
    tx0Config.setTx0x2Decoy(false); // no decoy for Tx0x2

    return new Tx0x2Context(
        cahootsWallet,
        CahootsTypeUser.SENDER,
        account,
        feePerB,
        tx0Service,
        tx0Config,
        serverApiInitiator);
  }

  public static Tx0x2Context newCounterparty(
      CahootsWallet cahootsWallet, int account, Tx0Service tx0Service) {
    return new Tx0x2Context(
        cahootsWallet, CahootsTypeUser.COUNTERPARTY, account, null, tx0Service, null, null);
  }

  public Tx0Service getTx0Service() {
    return tx0Service;
  }

  public Tx0Config getTx0ConfigInitiator() {
    return tx0ConfigInitiator;
  }

  public ServerApi getServerApiInitiator() {
    return serverApiInitiator;
  }

  public Tx0Result getTx0ResultInitiator() {
    return tx0ResultInitiator;
  }

  public void setTx0ResultInitiator(Tx0Result tx0ResultInitiator) {
    this.tx0ResultInitiator = tx0ResultInitiator;
  }
}
