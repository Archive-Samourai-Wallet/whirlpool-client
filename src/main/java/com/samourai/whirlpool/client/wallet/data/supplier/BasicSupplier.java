package com.samourai.whirlpool.client.wallet.data.supplier;

import org.slf4j.Logger;

/** Supplier with static data. */
public abstract class BasicSupplier<D> {
  protected final Logger log;
  private D value;
  private D mockValue; // forced value for tests
  private Long lastUpdate;

  public BasicSupplier(final Logger log) {
    this.log = log;
    this.value = null;
    this.mockValue = null;
    this.lastUpdate = null;
  }

  protected synchronized void setValue(D value) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("setValue");
    }
    // validate
    validate(value);
    D oldValue = this.value;

    // set
    this.value = mockValue != null ? mockValue : value;
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

  public void _mockValue(D mockValue) throws Exception {
    this.mockValue = mockValue;
    setValue(mockValue);
  }
}
