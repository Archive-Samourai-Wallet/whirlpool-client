package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;

public interface DataPersisterFactory {

  DataPersister createDataPersister(
      WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier) throws Exception;
}
