package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public interface DataSourceFactory {

  DataSource createDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      String passphrase,
      WalletStateSupplier walletStateSupplier,
      UtxoConfigSupplier utxoConfigSupplier)
      throws Exception;
}
