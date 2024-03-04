package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;

public interface DataSource {

  void open(CoordinatorSupplier coordinatorSupplier) throws Exception;

  void close() throws Exception;

  IPushTx getPushTx();

  ISweepBackend getSweepBackend();

  ISeenBackend getSeenBackend();

  WalletSupplier getWalletSupplier();

  UtxoSupplier getUtxoSupplier();

  PaynymSupplier getPaynymSupplier();

  DataSourceConfig getDataSourceConfig();
}
