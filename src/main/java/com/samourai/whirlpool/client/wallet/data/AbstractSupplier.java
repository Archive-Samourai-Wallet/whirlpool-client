package com.samourai.whirlpool.client.wallet.data;

import org.slf4j.Logger;

public abstract class AbstractSupplier<D> {
  protected final Logger log;

  public AbstractSupplier(final Logger log) {
    this.log = log;
  }

  protected abstract D getValue();

  public abstract Long getLastUpdate();
}
