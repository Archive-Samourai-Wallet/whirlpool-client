package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import java.util.Collection;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistableUtxoConfigSupplier extends AbstractPersistableSupplier<UtxoConfigData>
    implements UtxoConfigSupplier {
  private static final Logger log = LoggerFactory.getLogger(PersistableUtxoConfigSupplier.class);

  public PersistableUtxoConfigSupplier(UtxoConfigPersister persister) throws Exception {
    super(null, persister, log);
  }

  private UtxoConfigPersisted newUtxoConfig(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();

    // find by tx hash (new PREMIX from TX0)
    String tx0Key = computeUtxoConfigKey(utxo.tx_hash);
    UtxoConfigPersisted utxoConfigByHash = getValue().getUtxoConfig(tx0Key);
    UtxoConfigPersisted utxoConfig =
        utxoConfigByHash != null ? utxoConfigByHash.copy() : new UtxoConfigPersisted();

    // set mixsDone
    if (whirlpoolUtxo.isAccountPostmix()) {
      utxoConfig.incrementMixsDone();
    }

    // log
    return utxoConfig;
  }

  @Override
  public UtxoConfigPersisted getUtxoConfig(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    String key = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);

    // get existing utxoConfig
    UtxoConfigPersisted utxoConfigPersisted = getValue().getUtxoConfig(key);
    return utxoConfigPersisted;
  }

  @Override
  public void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String txid) {
    UnspentOutput utxo = fromUtxo.getUtxo();
    String fromKey = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
    String toKey = computeUtxoConfigKey(txid);
    forwardUtxoConfig(fromKey, toKey);
  }

  protected void forwardUtxoConfig(String fromKey, String toKey) {
    UtxoConfigData data = getValue();
    UtxoConfigPersisted fromUtxoConfig = data.getUtxoConfig(fromKey);
    if (fromUtxoConfig != null) {
      UtxoConfigPersisted newUtxoConfig = fromUtxoConfig.copy();
      newUtxoConfig.setForwarding(System.currentTimeMillis());
      if (log.isDebugEnabled()) {
        log.debug("forwardUtxoConfig: " + fromKey + " -> " + toKey + ": " + newUtxoConfig);
      }
      data.add(toKey, newUtxoConfig);
    } else {
      log.warn("forwardUtxoConfig failed: no utxoConfig found for " + fromKey);
    }
  }

  @Override
  public void saveUtxoConfig(UtxoConfigPersisted utxoConfigPersisted) {
    getValue().setLastChange();
  }

  protected String computeUtxoConfigKey(String hash, int index) {
    return ClientUtils.sha256Hash(ClientUtils.utxoToKey(hash, index));
  }

  private String computeUtxoConfigKey(String utxoHash) {
    return ClientUtils.sha256Hash(utxoHash);
  }

  public synchronized void onUtxoChanges(UtxoData utxoData) {
    WhirlpoolUtxoChanges utxoChanges = utxoData.getUtxoChanges();

    // create new utxoConfigs
    int nbCreated = createUtxoConfigPersisted(utxoChanges.getUtxosAdded());

    // cleanup utxoConfigs
    int nbCleaned = 0;
    if (!utxoData.getUtxos().isEmpty() && utxoData.getUtxoChanges().getUtxosRemoved().size() > 0) {
      nbCleaned = cleanup(utxoData.getUtxos().values());
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "utxoConfig: "
              + nbCreated
              + " created, "
              + nbCleaned
              + " cleaned, "
              + getValue().toString()
              + " for "
              + utxoData);
    }
  }

  private int createUtxoConfigPersisted(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    int nbCreated = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      UnspentOutput utxo = whirlpoolUtxo.getUtxo();
      String key = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);

      // create new utxoConfig when missing
      UtxoConfigPersisted utxoConfigPersisted = getValue().getUtxoConfig(key);
      if (utxoConfigPersisted == null) {
        utxoConfigPersisted = newUtxoConfig(whirlpoolUtxo);
        getValue().add(key, utxoConfigPersisted);
        nbCreated++;
      } else {
        // clear forwarding status if any
        if (utxoConfigPersisted.getForwarding() != null) {
          utxoConfigPersisted.setForwarding(null);
        }
      }
    }
    return nbCreated;
  }

  private int cleanup(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    List<String> validKeys =
        StreamSupport.stream(whirlpoolUtxos)
            .map(
                new Function<WhirlpoolUtxo, String>() {
                  @Override
                  public String apply(WhirlpoolUtxo whirlpoolUtxo) {
                    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
                    return computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
                  }
                })
            .collect(Collectors.<String>toList());
    int nbCleaned = getValue().cleanup(validKeys);
    return nbCleaned;
  }
}
