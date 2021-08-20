package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.whirlpool.client.wallet.data.utxoConfig.PersistableUtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public interface DataPersister {

  void open() throws Exception;

  void close() throws Exception;

  WalletStateSupplier getWalletStateSupplier();

  PersistableUtxoConfigSupplier getUtxoConfigSupplier();
}
