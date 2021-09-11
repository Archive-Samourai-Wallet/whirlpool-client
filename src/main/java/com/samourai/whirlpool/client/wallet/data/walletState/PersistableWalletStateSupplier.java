package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicPersistableSupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistableWalletStateSupplier extends BasicPersistableSupplier<WalletStateData>
    implements WalletStateSupplier {
  private static final Logger log = LoggerFactory.getLogger(PersistableWalletStateSupplier.class);
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private final IIndexHandler indexHandlerExternal;
  private Map<String, IIndexHandler> indexHandlerWallets;

  public PersistableWalletStateSupplier(
      WalletStatePersister persister, ExternalDestination externalDestination) {
    super(persister, log);

    int externalIndexDefault =
        externalDestination != null ? externalDestination.getStartIndex() : 0;
    this.indexHandlerExternal =
        new WalletStateIndexHandler(this, EXTERNAL_INDEX_HANDLER, externalIndexDefault);
    this.indexHandlerWallets = new LinkedHashMap<String, IIndexHandler>();
  }

  @Override
  public void setWalletIndex(
      WhirlpoolAccount account, AddressType addressType, Chain chain, int value) throws Exception {
    WalletStateData currentValue = getValue();
    if (currentValue == null) {
      // should never happen
      throw new Exception("Cannot setWalletIndex(), no value loaded yet!");
    }

    String persistKey = computePersistKeyWallet(account, addressType, chain);
    currentValue.setWalletIndex(persistKey, value);
  }

  @Override
  public IIndexHandler getIndexHandlerWallet(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    String persistKey = computePersistKeyWallet(account, addressType, chain);
    IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
    if (indexHandlerWallet == null) {
      indexHandlerWallet = createIndexHandlerWallet(account, addressType, chain, persistKey);
      indexHandlerWallets.put(persistKey, indexHandlerWallet);
    }
    return indexHandlerWallet;
  }

  protected IIndexHandler createIndexHandlerWallet(
      WhirlpoolAccount account, AddressType addressType, Chain chain, String persistKey) {
    return new WalletStateIndexHandler(this, persistKey, 0);
  }

  protected String computePersistKeyWallet(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    return account.name() + "_" + addressType.getPurpose() + "_" + chain.getIndex();
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

  @Override
  public boolean isInitialized() {
    return getValue().isInitialized();
  }

  @Override
  public void setInitialized(boolean initialized) {
    getValue().setInitialized(initialized);
  }

  @Override
  public IIndexHandler getIndexHandlerExternal() {
    return indexHandlerExternal;
  }
}
