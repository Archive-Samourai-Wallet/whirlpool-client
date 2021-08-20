package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;

public interface DataSourceFactory {

  DataSource createDataSource(
      WhirlpoolWalletConfig config,
      HD_Wallet bip44w,
      String walletIdentifier,
      DataPersister dataPersister)
      throws Exception;
}
