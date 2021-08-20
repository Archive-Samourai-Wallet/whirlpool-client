package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import java.util.Collection;

public interface UtxoConfigSupplier {
  UtxoConfigPersisted getTx(String txHash);

  UtxoConfigPersisted getUtxo(String utxoHash, int utxoIndex);

  void saveTx(String txHash, UtxoConfigPersisted utxoConfigPersisted);

  void saveUtxo(String utxoHash, int utxoIndex, UtxoConfigPersisted utxoConfigPersisted);

  void clean(Collection<WhirlpoolUtxo> existingUtxos);
}
