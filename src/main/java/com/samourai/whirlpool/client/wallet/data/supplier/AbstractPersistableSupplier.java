package com.samourai.whirlpool.client.wallet.data.supplier;

import org.slf4j.Logger;

public abstract class AbstractPersistableSupplier<D extends PersistableData>
    extends BasicSupplier<D> {

  private AbstractPersister<D, ?> persister;

  public AbstractPersistableSupplier(
      final D fallbackValue, AbstractPersister<D, ?> persister, Logger log) throws Exception {
    super(log, fallbackValue);
    this.persister = persister;
  }

  public void load() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("load()");
    }
    setValue(getPersistedValue());
  }

  protected D getPersistedValue() throws Exception {
    return persister.load();
  }

  public boolean persist(boolean force) throws Exception {
    D value = getValue();

    // check for local modifications
    if (!force && value.getLastChange() <= persister.getLastWrite()) {
      return false;
    }

    persister.write(value);
    return true;
  }
}
