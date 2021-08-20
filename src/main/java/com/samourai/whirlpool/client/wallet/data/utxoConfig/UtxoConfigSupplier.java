package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;

public interface UtxoConfigSupplier {
  UtxoConfigPersisted getUtxoConfig(WhirlpoolUtxo whirlpoolUtxo);

  void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String txid);

  void saveUtxoConfig(UtxoConfigPersisted utxoConfigPersisted);

  void onUtxoChanges(UtxoData utxoData);
}
