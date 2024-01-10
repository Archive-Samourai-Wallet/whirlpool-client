package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.bipWallet.BipDerivation;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.AbstractIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.supplier.IPersister;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStatePersistableSupplier extends AbstractPersistableSupplier<WalletStateData>
    implements WalletStateSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletStatePersistableSupplier.class);
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private Map<String, IIndexHandler> indexHandlers;

  public WalletStatePersistableSupplier(IPersister<WalletStateData> persister) {
    super(persister, log);
    this.indexHandlers = new LinkedHashMap<>();
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
    String persistKey =
        computePersistKeyWallet(bipWallet.getAccount(), bipWallet.getDerivation(), chain);
    return getIndexHandler(persistKey);
  }

  @Override
  public IIndexHandler getIndexHandlerExternal() {
    return getIndexHandler(EXTERNAL_INDEX_HANDLER);
  }

  protected IIndexHandler getIndexHandler(String persistKey) {
    IIndexHandler indexHandler = indexHandlers.get(persistKey);
    if (indexHandler == null) {
      indexHandler = createIndexHandler(persistKey);
      indexHandlers.put(persistKey, indexHandler);
    }
    return indexHandler;
  }

  protected IIndexHandler createIndexHandler(final String persistKey) {
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

  protected String computePersistKeyWallet(
      WhirlpoolAccount account, BipDerivation bipDerivation, Chain chain) {
    return account.name() + "_" + bipDerivation.getPurpose() + "_" + chain.getIndex();
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
