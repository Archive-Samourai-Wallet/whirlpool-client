package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;

public interface UtxoConfigSupplier {
  UtxoConfigPersisted getUtxoConfigPersisted(WhirlpoolUtxo whirlpoolUtxo);

  void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String hash, int index);

  void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String txid);

  void onUtxoConfigChange();

  void onUtxoChanges(UtxoData utxoData, PoolSupplier poolSupplier, Tx0ParamService tx0ParamService);
}
