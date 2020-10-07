package com.samourai.whirlpool.client.wallet.data;

import org.slf4j.Logger;

/** Supplier with static data. */
public abstract class BasicSupplier<D> extends AbstractSupplier<D> {
  private D value;
  private Long lastUpdate;

  public BasicSupplier(final Logger log, D initialValue) {
    super(log);
    this.value = null;
    this.lastUpdate = null;
    if (initialValue != null) {
      setValue(initialValue);
    }
  }

  protected void setValue(D value) {
    this.value = value;
    this.lastUpdate = System.currentTimeMillis();
  }

  @Override
  public D getValue() {
    return value;
  }

  @Override
  public Long getLastUpdate() {
    return lastUpdate;
  }
}
