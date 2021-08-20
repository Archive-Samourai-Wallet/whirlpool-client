package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

public interface WalletStateSupplier {
  IIndexHandler getExternalIndexHandler();

  IIndexHandler computeIndexHandler(WhirlpoolAccount account, AddressType addressType, Chain chain);

  boolean isInitialized();

  void setInitialized(boolean initialized);

  boolean isSynced();

  void setSynced(boolean synced);

  void setWalletIndex(WhirlpoolAccount account, AddressType addressType, Chain chain, int value)
      throws Exception;
}
