package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicPersistableSupplier;
import java.util.Collection;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigPersistedSupplier extends BasicPersistableSupplier<UtxoConfigData>
    implements UtxoConfigSupplier {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigPersistedSupplier.class);

  public UtxoConfigPersistedSupplier(UtxoConfigPersister persister) {
    super(persister, log);
  }

  @Override
  protected void validate(UtxoConfigData value) {
    // nothing to do
  }

  @Override
  protected void onValueChange(UtxoConfigData value) {
    // nothing to do
  }

  @Override
  public UtxoConfigPersisted getUtxo(String hash, int index) {
    String key = computeUtxoConfigKey(hash, index);
    return getValue().getUtxoConfig(key);
  }

  @Override
  public synchronized void setUtxo(String hash, int index, int mixsDone) {
    String key = computeUtxoConfigKey(hash, index);
    UtxoConfigPersisted utxoConfigPersisted = getUtxo(hash, index);
    if (utxoConfigPersisted == null) {
      utxoConfigPersisted = new UtxoConfigPersisted(mixsDone, null);
      if (log.isDebugEnabled()) {
        log.debug("+utxoConfig: " + hash + ":" + index + " => " + utxoConfigPersisted);
      }
    } else {
      utxoConfigPersisted.setMixsDone(mixsDone);
    }
    getValue().add(key, utxoConfigPersisted);
  }

  @Override
  public synchronized void clean(Collection<WhirlpoolUtxo> existingUtxos) {
    List<String> validKeys =
        StreamSupport.stream(existingUtxos)
            .map(
                new Function<WhirlpoolUtxo, String>() {
                  @Override
                  public String apply(WhirlpoolUtxo whirlpoolUtxo) {
                    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
                    return computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
                  }
                })
            .collect(Collectors.<String>toList());
    getValue().cleanup(validKeys);
  }

  @Override
  public synchronized boolean persist(boolean force) throws Exception {
    // synchronized to avoid ConcurrentModificationException with setUtxo()
    return super.persist(force);
  }

  protected String computeUtxoConfigKey(String hash, int index) {
    return ClientUtils.sha256Hash(ClientUtils.utxoToKey(hash, index));
  }
}
