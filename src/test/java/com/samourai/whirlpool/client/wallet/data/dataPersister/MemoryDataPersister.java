package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class MemoryDataPersister extends OrchestratedDataPersister {

  public MemoryDataPersister(
      WhirlpoolWallet whirlpoolWallet,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier)
      throws Exception {
    super(whirlpoolWallet, 999999, walletStateSupplier, utxoConfigSupplier);
  }

  @Override
  public void persist(boolean force) throws Exception {
    // disabled
  }
}
