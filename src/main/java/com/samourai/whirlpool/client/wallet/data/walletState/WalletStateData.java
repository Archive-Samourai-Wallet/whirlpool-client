package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.whirlpool.client.wallet.data.supplier.PersistableData;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStateData extends PersistableData {
  private static final Logger log = LoggerFactory.getLogger(WalletStateData.class);
  private static final String INDEX_INITIALIZED = "init";
  private static final String INDEX_NYM_CLAIMED = "nymClaimed";

  private Map<String, Integer> items;

  // used by Sparrow
  public WalletStateData() {
    this(new LinkedHashMap<String, Integer>());
  }

  // used by Sparrow
  public WalletStateData(Map<String, Integer> indexes) {
    super();
    this.items = new LinkedHashMap<String, Integer>();
    this.items.putAll(indexes);
  }

  public boolean isInitialized() {
    return getBoolean(INDEX_INITIALIZED, false);
  }

  public void setInitialized(boolean value) {
    setBoolean(INDEX_INITIALIZED, value);
  }

  public boolean isNymClaimed() {
    return getBoolean(INDEX_NYM_CLAIMED, false);
  }

  public void setNymClaimed(boolean value) {
    setBoolean(INDEX_NYM_CLAIMED, value);
  }

  public Map<String, Integer> getItems() {
    return items;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return get(key, defaultValue ? 1 : 0) == 1;
  }

  public void setBoolean(String key, boolean value) {
    set(key, value ? 1 : 0);
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
    int currentIndex = get(key, 0);
    if (currentIndex > value) {
      if (log.isDebugEnabled()) {
        log.debug("Rollbacking [" + key + "]: " + currentIndex + " => " + value);
      }
    }

    items.put(key, value);
    setLastChange();
  }

  @Override
  public String toString() {
    // used by android whirlpool debug screen
    return "items=" + items.toString();
  }
}
