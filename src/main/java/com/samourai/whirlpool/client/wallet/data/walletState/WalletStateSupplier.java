package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.client.indexHandler.IndexHandlerSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.PersistableSupplier;

public interface WalletStateSupplier extends PersistableSupplier, IndexHandlerSupplier {
  boolean isInitialized();

  void setInitialized(boolean value);

  boolean isNymClaimed();

  void setNymClaimed(boolean value);
}
