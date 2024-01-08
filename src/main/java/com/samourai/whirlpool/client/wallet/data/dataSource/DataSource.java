package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;

public interface DataSource {

  void open() throws Exception;

  void close() throws Exception;

  String pushTx(String txHex) throws Exception;

  ISeenBackend getSeenBackend();

  WalletSupplier getWalletSupplier();

  UtxoSupplier getUtxoSupplier();

  MinerFeeSupplier getMinerFeeSupplier();

  ChainSupplier getChainSupplier();

  PoolSupplier getPoolSupplier();

  PaynymSupplier getPaynymSupplier();

  Tx0PreviewService getTx0PreviewService();
}
