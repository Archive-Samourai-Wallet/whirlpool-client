package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bipWallet.BipWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Context {
  private Logger log = LoggerFactory.getLogger(Tx0Context.class);

  private final BipWallet bipWalletPremix;
  private final BipWallet bipWalletChange;
  private int initialIndexPremix;
  private int initialIndexChange;

  public Tx0Context(BipWallet bipWalletPremix, BipWallet bipWalletChange) {
    this.bipWalletPremix = bipWalletPremix;
    this.bipWalletChange = bipWalletChange;
    this.initialIndexPremix = bipWalletPremix.getIndexHandlerReceive().get();
    this.initialIndexChange = bipWalletChange.getIndexHandlerChange().get();
  }

  public void revertIndexPremix() {
    bipWalletPremix.getIndexHandlerReceive().set(initialIndexPremix, true);
  }

  public void revertIndexChange() {
    bipWalletChange.getIndexHandlerChange().set(initialIndexChange, true);
  }
}
