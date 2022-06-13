package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractFilePersister;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigPersisterFile
    extends AbstractFilePersister<UtxoConfigData, Map<String, UtxoConfigPersisted>> {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigPersisterFile.class);

  public UtxoConfigPersisterFile(String fileName) {
    super(fileName, new TypeReference<Map<String, UtxoConfigPersisted>>() {}, log);
  }

  @Override
  protected UtxoConfigData getInitialValue() {
    return new UtxoConfigData();
  }

  @Override
  protected UtxoConfigData fromPersisted(Map<String, UtxoConfigPersisted> persisted) {
    Map<String, UtxoConfigPersisted> utxoConfigs = new LinkedHashMap<String, UtxoConfigPersisted>();
    utxoConfigs.putAll(persisted);
    return new UtxoConfigData(utxoConfigs);
  }

  @Override
  protected synchronized Map<String, UtxoConfigPersisted> toPersisted(UtxoConfigData data) {
    Map<String, UtxoConfigPersisted> mapPersisted =
        new LinkedHashMap<String, UtxoConfigPersisted>();
    mapPersisted.putAll(data.getUtxoConfigs());
    return mapPersisted;
  }
}
