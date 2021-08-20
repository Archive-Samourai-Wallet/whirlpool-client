package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistableWalletStateSupplier extends AbstractPersistableSupplier<WalletStateData>
    implements WalletStateSupplier {
  private static final Logger log = LoggerFactory.getLogger(PersistableWalletStateSupplier.class);
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private final IIndexHandler externalIndexHandler;
  private boolean synced; // postmix counters sync

  public PersistableWalletStateSupplier(WalletStatePersister persister, int externalIndexDefault)
      throws Exception {
    super(null, persister, log);
    this.externalIndexHandler =
        new WalletStateIndexHandler(this, EXTERNAL_INDEX_HANDLER, externalIndexDefault);
    this.synced = true;
  }

  @Override
  public void setWalletIndex(
      WhirlpoolAccount account, AddressType addressType, Chain chain, int value) throws Exception {
    WalletStateData currentValue = getValue();
    if (currentValue == null) {
      // should never happen
      throw new Exception("Cannot setWalletIndex(), no value loaded yet!");
    }

    String persistKey = computePersistKey(account, addressType, chain);
    currentValue.setWalletIndex(persistKey, value);
  }

  protected String computePersistKey(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    return account.name() + "_" + addressType.getPurpose() + "_" + chain.getIndex();
  }

  @Override
  public void load() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("load()");
    }
    WalletStateData currentValue = getValue();
    if (currentValue != null) {
      throw new Exception("Cannot load(), value already loaded!");
    }

    // FIRST LOAD
    // read indexs from file
    super.load();
  }

  public IIndexHandler computeIndexHandler(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    String persistKey = computePersistKey(account, addressType, chain);
    return new WalletStateIndexHandler(this, persistKey, 0);
  }

  protected int get(String key, int defaultValue) {
    return getValue().get(key, defaultValue);
  }

  protected int getAndIncrement(String key, int defaultValue) {
    return getValue().getAndIncrement(key, defaultValue);
  }

  protected void set(String key, int value) {
    getValue().set(key, value);
    if (log.isDebugEnabled()) {
      log.debug("set: [" + key + "]=" + value);
    }
  }

  public boolean isSynced() {
    return synced;
  }

  public void setSynced(boolean synced) {
    this.synced = synced;
  }

  @Override
  public boolean isInitialized() {
    return getValue().isInitialized();
  }

  @Override
  public void setInitialized(boolean initialized) {
    getValue().setInitialized(initialized);
  }

  @Override
  public IIndexHandler getExternalIndexHandler() {
    return externalIndexHandler;
  }
}
