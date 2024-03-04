package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.bipWallet.BipDerivation;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.hd.Chain;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class IndexHandlerManager {
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private final IIndexHandler indexHandlerExternal;
  private Map<String, IIndexHandler> indexHandlerWallets;

  public IndexHandlerManager() {
    this.indexHandlerExternal = createIndexHandler(EXTERNAL_INDEX_HANDLER);
    this.indexHandlerWallets = new LinkedHashMap<>();
  }

  protected abstract IIndexHandler createIndexHandler(final String persistKey);

  protected String computePersistKeyWallet(
      SamouraiAccount account, BipDerivation bipDerivation, Chain chain) {
    return account.name() + "_" + bipDerivation.getPurpose() + "_" + chain.getIndex();
  }

  public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
    String persistKey =
        computePersistKeyWallet(bipWallet.getAccount(), bipWallet.getDerivation(), chain);
    IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
    if (indexHandlerWallet == null) {
      indexHandlerWallet = createIndexHandler(persistKey);
      indexHandlerWallets.put(persistKey, indexHandlerWallet);
    }
    return indexHandlerWallet;
  }

  public IIndexHandler getIndexHandlerExternal() {
    return indexHandlerExternal;
  }
}
