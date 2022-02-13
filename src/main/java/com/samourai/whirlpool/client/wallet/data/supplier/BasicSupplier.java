package com.samourai.whirlpool.client.wallet.data.supplier;

import org.slf4j.Logger;

/** Supplier with static data. */
public abstract class BasicSupplier<D> {
  protected final Logger log;
  private D value;
  private Long lastUpdate;

  public BasicSupplier(final Logger log) {
    this.log = log;
    this.value = null;
    this.lastUpdate = null;
  }

  protected void setValue(D value) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("setValue");
    }
    // validate
    validate(value);
    D oldValue = getValue();

    // set
    this.value = value;
    this.lastUpdate = System.currentTimeMillis();

    // notify
    if (oldValue == null || !oldValue.equals(value)) {
      onValueChange(value);
    }
  }

  protected abstract void validate(D value) throws Exception;

  protected abstract void onValueChange(D value) throws Exception;

  public D getValue() {
    return value;
  }

  public Long getLastUpdate() {
    return lastUpdate;
  }
}
