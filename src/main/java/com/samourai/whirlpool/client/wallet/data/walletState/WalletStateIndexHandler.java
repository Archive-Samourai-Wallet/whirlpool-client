package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.AbstractIndexHandler;

public class WalletStateIndexHandler extends AbstractIndexHandler {
  private PersistableWalletStateSupplier walletStateSupplier;
  private String key;
  private int defaultValue;

  public WalletStateIndexHandler(
      PersistableWalletStateSupplier walletStateSupplier, String key, int defaultValue) {
    super();
    this.walletStateSupplier = walletStateSupplier;
    this.key = key;
    this.defaultValue = defaultValue;
  }

  @Override
  public int get() {
    return walletStateSupplier.get(key, defaultValue);
  }

  @Override
  public synchronized int getAndIncrement() {
    return walletStateSupplier.getAndIncrement(key, defaultValue);
  }

  @Override
  public synchronized void set(int value) {
    walletStateSupplier.set(key, value);
  }
}
