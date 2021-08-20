package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractPersistableSupplier;
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

  @Override
  public UtxoConfigPersisted getTx(String txHash) {
    String key = computeUtxoConfigKey(txHash);
    return getValue().getUtxoConfig(key);
  }

  @Override
  public UtxoConfigPersisted getUtxo(String hash, int index) {
    String key = computeUtxoConfigKey(hash, index);
    return getValue().getUtxoConfig(key);
  }

  @Override
  public void saveTx(String txHash, UtxoConfigPersisted utxoConfigPersisted) {
    String key = computeUtxoConfigKey(txHash);
    getValue().add(key, utxoConfigPersisted);
  }

  @Override
  public void saveUtxo(String hash, int index, UtxoConfigPersisted utxoConfigPersisted) {
    String key = computeUtxoConfigKey(hash, index);
    getValue().add(key, utxoConfigPersisted);
  }

  @Override
  public void clean(Collection<WhirlpoolUtxo> existingUtxos) {
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
    int nbCleaned = getValue().cleanup(validKeys);
    if (log.isDebugEnabled()) {
      log.debug(nbCleaned + " utxoConfig cleaned");
    }
  }

  protected String computeUtxoConfigKey(String hash, int index) {
    return ClientUtils.sha256Hash(ClientUtils.utxoToKey(hash, index));
  }

  private String computeUtxoConfigKey(String utxoHash) {
    return ClientUtils.sha256Hash(utxoHash);
  }
}
