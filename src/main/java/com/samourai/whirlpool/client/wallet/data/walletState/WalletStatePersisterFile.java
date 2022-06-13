package com.samourai.whirlpool.client.wallet.data.walletState;

import com.fasterxml.jackson.core.type.TypeReference;
import com.samourai.whirlpool.client.wallet.data.supplier.AbstractFilePersister;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStatePersisterFile
    extends AbstractFilePersister<WalletStateData, Map<String, Integer>> {
  private static final Logger log = LoggerFactory.getLogger(WalletStatePersisterFile.class);

  public WalletStatePersisterFile(String fileName) {
    super(fileName, new TypeReference<Map<String, Integer>>() {}, log);
  }

  @Override
  protected WalletStateData getInitialValue() {
    return new WalletStateData();
  }

  @Override
  protected WalletStateData fromPersisted(Map<String, Integer> persisted) {
    Map<String, Integer> items = new LinkedHashMap<String, Integer>();
    items.putAll(persisted);
    return new WalletStateData(items);
  }

  @Override
  protected Map<String, Integer> toPersisted(WalletStateData data) {
    Map<String, Integer> mapPersisted = new LinkedHashMap<String, Integer>();
    mapPersisted.putAll(data.getItems());
    return mapPersisted;
  }
}
