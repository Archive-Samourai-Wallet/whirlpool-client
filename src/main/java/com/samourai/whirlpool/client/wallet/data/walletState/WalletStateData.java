package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.whirlpool.client.wallet.data.supplier.PersistableData;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStateData extends PersistableData {
  private static final Logger log = LoggerFactory.getLogger(WalletStateData.class);
  private static final String INDEX_INITIALIZED = "init";

  private Map<String, Integer> items;

  protected WalletStateData() {
    this(new LinkedHashMap<String, Integer>());
  }

  protected WalletStateData(Map<String, Integer> indexes) {
    super();
    this.items = new LinkedHashMap<String, Integer>();
    this.items.putAll(indexes);
  }

  protected synchronized void setWalletIndex(String key, int value) {
    Integer currentIndex = items.get(key);
    if (currentIndex == null || currentIndex < value) {
      // update index
      if (log.isDebugEnabled()) {
        log.debug(
            key
                + ": apiIndex="
                + value
                + ", localIndex="
                + (currentIndex != null ? currentIndex : "null")
                + " => updating");
      }
      set(key, value);
    } else {
      // index unchanged
      if (log.isTraceEnabled()) {
        log.trace(
            key
                + ": apiIndex="
                + value
                + ", localIndex="
                + (currentIndex != null ? currentIndex : "null")
                + " => unchanged");
      }
    }
  }

  public boolean isInitialized() {
    return get(INDEX_INITIALIZED, 0) == 1;
  }

  public void setInitialized(boolean initialized) {
    set(INDEX_INITIALIZED, initialized ? 1 : 0);
  }

  protected Map<String, Integer> getItems() {
    return items;
  }

  protected int get(String key, int defaultValue) {
    if (!items.containsKey(key)) {
      return defaultValue;
    }
    return items.get(key);
  }

  protected synchronized int getAndIncrement(String key, int defaultValue) {
    int value = get(key, defaultValue);
    set(key, value + 1);
    return value;
  }

  protected synchronized void set(String key, int value) {
    items.put(key, value);
    setLastChange();
  }

  @Override
  public String toString() {
    // used by android whirlpool debug screen
    return "items=" + items.toString();
  }
}
