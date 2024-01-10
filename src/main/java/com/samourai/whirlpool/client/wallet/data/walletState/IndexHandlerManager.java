package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.bipWallet.BipDerivation;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class IndexHandlerManager {
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private Map<String, IIndexHandler> indexHandlers;

  public IndexHandlerManager() {
    this.indexHandlers = new LinkedHashMap<>();
  }

  protected abstract IIndexHandler createIndexHandler(final String persistKey);

  protected String computePersistKeyWallet(
      WhirlpoolAccount account, BipDerivation bipDerivation, Chain chain) {
    return account.name() + "_" + bipDerivation.getPurpose() + "_" + chain.getIndex();
  }

  public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
    String persistKey =
        computePersistKeyWallet(bipWallet.getAccount(), bipWallet.getDerivation(), chain);
    return getIndexHandler(persistKey);
  }

  protected IIndexHandler getIndexHandler(String persistKey) {
    IIndexHandler indexHandler = indexHandlers.get(persistKey);
    if (indexHandler == null) {
      indexHandler = createIndexHandler(persistKey);
      indexHandlers.put(persistKey, indexHandler);
    }
    return indexHandler;
  }

  public IIndexHandler getIndexHandlerExternal() {
    return getIndexHandler(EXTERNAL_INDEX_HANDLER);
  }
}
