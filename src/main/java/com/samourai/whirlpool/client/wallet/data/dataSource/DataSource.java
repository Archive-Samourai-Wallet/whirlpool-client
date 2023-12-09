package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;

public interface DataSource {

  void open() throws Exception;

  void close() throws Exception;

  IPushTx getPushTx();

  ISweepBackend getSweepBackend();

  ISeenBackend getSeenBackend();

  WalletSupplier getWalletSupplier();

  UtxoSupplier getUtxoSupplier();

  MinerFeeSupplier getMinerFeeSupplier();

  ChainSupplier getChainSupplier();

  CoordinatorSupplier getCoordinatorSupplier();

  PaynymSupplier getPaynymSupplier();

  Tx0PreviewService getTx0PreviewService();
}
