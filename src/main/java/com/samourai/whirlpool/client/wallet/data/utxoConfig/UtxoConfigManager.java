package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigManager {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigManager.class);
  private UtxoConfigSupplier utxoConfigSupplier;

  public UtxoConfigManager(UtxoConfigSupplier utxoConfigSupplier) {
    this.utxoConfigSupplier = utxoConfigSupplier;
  }

  public synchronized void onUtxoChanges(UtxoData utxoData) {
    // create new utxoConfigs
    createUtxoConfigs(utxoData.getUtxoChanges().getUtxosAdded());

    // cleanup utxoConfigs
    if (!utxoData.getUtxos().isEmpty() && utxoData.getUtxoChanges().getUtxosRemoved().size() > 0) {
      utxoConfigSupplier.clean(utxoData.getUtxos().values());
    }
  }

  private void createUtxoConfigs(Collection<WhirlpoolUtxo> whirlpoolUtxosAdded) {
    int nbCreated = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxosAdded) {
      UnspentOutput utxo = whirlpoolUtxo.getUtxo();

      // look for missing utxoConfig
      if (utxoConfigSupplier.getUtxo(utxo.tx_hash, utxo.tx_output_n) == null) {
        // create missing utxoConfig
        UtxoConfigPersisted utxoConfigByTx = utxoConfigSupplier.getTx(utxo.tx_hash);
        UtxoConfigPersisted newUtxoConfig =
            (utxoConfigByTx != null ? utxoConfigByTx.copy() : new UtxoConfigPersisted());

        // set mixsDone
        if (whirlpoolUtxo.isAccountPostmix()) {
          newUtxoConfig.incrementMixsDone();
        }

        utxoConfigSupplier.saveUtxo(utxo.tx_hash, utxo.tx_output_n, newUtxoConfig);
        nbCreated++;
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(nbCreated + " utxoConfig created");
    }
  }
}
