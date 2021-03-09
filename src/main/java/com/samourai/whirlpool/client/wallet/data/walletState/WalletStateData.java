package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.PersistableData;
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

  protected WalletStateData copy() {
    return new WalletStateData(this.items);
  }

  protected void updateIndexs(WhirlpoolAccount account, WalletResponse.Address address) {
    updateIndexs(address.account_index, account.getPersistKeyMain());
    updateIndexs(address.change_index, account.getPersistKeyChange());
  }

  private synchronized void updateIndexs(int apiIndex, String key) {
    Integer currentIndex = items.get(key);
    if (currentIndex == null || currentIndex < apiIndex) {
      // update index
      if (log.isDebugEnabled()) {
        log.debug(
            key
                + ": apiIndex="
                + apiIndex
                + ", localIndex="
                + (currentIndex != null ? currentIndex : "null")
                + " => updating");
      }
      set(key, apiIndex);
    } else {
      // index unchanged
      if (log.isTraceEnabled()) {
        log.trace(
            key
                + ": apiIndex="
                + apiIndex
                + ", localIndex="
                + (currentIndex != null ? currentIndex : "null")
                + " => unchanged");
      }
    }
  }

  public boolean isInitialized() {
    return get(INDEX_INITIALIZED, 0) == 1;
  }

  public void setInitialized() {
    set(INDEX_INITIALIZED, 1);
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
