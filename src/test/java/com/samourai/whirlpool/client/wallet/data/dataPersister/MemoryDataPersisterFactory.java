package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class MemoryDataPersisterFactory extends FileDataPersisterFactory {

  public MemoryDataPersisterFactory() {
    super();
  }

  @Override
  public DataPersister createDataPersister(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
      throws Exception {
    WalletStateSupplier walletStateSupplier = new MemoryWalletStateSupplier();
    UtxoConfigSupplier utxoConfigSupplier = computeUtxoConfigSupplier(whirlpoolWallet);
    return new MemoryDataPersister(whirlpoolWallet, walletStateSupplier, utxoConfigSupplier);
  }
}
