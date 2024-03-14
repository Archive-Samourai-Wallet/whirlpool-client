package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.supplier.IPersister;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigPersistableSupplier extends AbstractPersistableSupplier<UtxoConfigData>
    implements UtxoConfigSupplier {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigPersistableSupplier.class);

  public UtxoConfigPersistableSupplier(IPersister<UtxoConfigData> persister) {
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
  public synchronized void setMixsDone(String hash, int index, int mixsDone) {
    applyUtxoConfig(
        hash,
        index,
        utxoConfigPersisted -> {
          utxoConfigPersisted.setMixsDone(mixsDone);
          return null;
        });
  }

  protected synchronized void applyUtxoConfig(
      String hash, int index, CallbackWithArg<UtxoConfigPersisted, Void> callback) {
    String key = computeUtxoConfigKey(hash, index);
    UtxoConfigPersisted utxoConfigPersisted = getUtxo(hash, index);
    if (utxoConfigPersisted == null) {
      utxoConfigPersisted = new UtxoConfigPersisted();
      if (log.isDebugEnabled()) {
        log.debug("+utxoConfig: " + hash + ":" + index + " => " + utxoConfigPersisted);
      }
    }

    try {
      callback.apply(utxoConfigPersisted);
    } catch (Exception e) {
      log.error("", e);
    }
    getValue().add(key, utxoConfigPersisted);
  }

  @Override
  public synchronized void clean(Collection<WhirlpoolUtxo> existingUtxos) {
    List<String> validKeys =
        existingUtxos.stream()
            .map(
                whirlpoolUtxo -> {
                  UnspentOutput utxo = whirlpoolUtxo.getUtxo();
                  return computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
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
