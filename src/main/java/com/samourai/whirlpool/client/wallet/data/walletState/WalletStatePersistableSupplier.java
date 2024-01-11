package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.AbstractIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.supplier.IPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStatePersistableSupplier extends AbstractPersistableSupplier<WalletStateData>
    implements WalletStateSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletStatePersistableSupplier.class);

  private final IndexHandlerManager indexHandlerManager;

  public WalletStatePersistableSupplier(
      IPersister<WalletStateData> persister, ExternalDestination externalDestination) {
    super(persister, log);

    this.indexHandlerManager =
        new IndexHandlerManager() {
          @Override
          protected IIndexHandler createIndexHandler(String persistKey) {
            int defaultValue = 0;
            return new AbstractIndexHandler() {
              @Override
              public int getAndIncrement() {
                return getValue().getAndIncrement(persistKey, defaultValue);
              }

              @Override
              public int get() {
                return getValue().get(persistKey, defaultValue);
              }

              @Override
              protected void set(int value) {
                getValue().set(persistKey, value);
                if (log.isDebugEnabled()) {
                  log.debug("set: [" + persistKey + "]=" + value);
                }
              }
            };
          }
        };
  }

  @Override
  protected void validate(WalletStateData value) {
    // nothing to do
  }

  @Override
  protected void onValueChange(WalletStateData value) throws Exception {
    // nothing to do
  }

  @Override
  public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
    return indexHandlerManager.getIndexHandlerWallet(bipWallet, chain);
  }

  @Override
  public IIndexHandler getIndexHandlerExternal() {
    return indexHandlerManager.getIndexHandlerExternal();
  }

  @Override
  public boolean isInitialized() {
    return getValue().isInitialized();
  }

  @Override
  public void setInitialized(boolean value) {
    getValue().setInitialized(value);
  }

  @Override
  public boolean isNymClaimed() {
    return getValue().isNymClaimed();
  }

  @Override
  public void setNymClaimed(boolean value) {
    getValue().setNymClaimed(value);
  }
}
