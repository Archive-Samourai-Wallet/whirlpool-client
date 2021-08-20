package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;

public interface DataSource {

  void open() throws Exception;

  void close() throws Exception;

  void pushTx(String txHex) throws Exception;

  TxsResponse fetchTxs(String[] zpubs, int page, int count) throws Exception;

  WalletSupplier getWalletSupplier();

  UtxoSupplier getUtxoSupplier();

  MinerFeeSupplier getMinerFeeSupplier();

  ChainSupplier getChainSupplier();

  PoolSupplier getPoolSupplier();

  Tx0ParamService getTx0ParamService();
}
