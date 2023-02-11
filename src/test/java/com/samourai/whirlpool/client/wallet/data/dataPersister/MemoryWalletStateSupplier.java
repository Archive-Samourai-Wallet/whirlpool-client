package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.data.walletState.IndexHandlerManager;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class MemoryWalletStateSupplier implements WalletStateSupplier {
  private final IndexHandlerManager indexHandlerManager;

  public MemoryWalletStateSupplier() {
    this.indexHandlerManager =
        new IndexHandlerManager(0) {
          @Override
          protected IIndexHandler createIndexHandler(String persistKey, int defaultValue) {
            return new MemoryIndexHandler();
          }
        };
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public void setInitialized(boolean value) {
    // nothing to do
  }

  @Override
  public boolean isNymClaimed() {
    return false;
  }

  @Override
  public void setNymClaimed(boolean value) {
    // nothing to do
  }

  @Override
  public void load() throws Exception {
    // nothing to do
  }

  @Override
  public boolean persist(boolean force) throws Exception {
    return false;
  }

  @Override
  public IIndexHandler getIndexHandlerExternal() {
    return indexHandlerManager.getIndexHandlerExternal();
  }

  @Override
  public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
    return indexHandlerManager.getIndexHandlerWallet(bipWallet, chain);
  }
}
