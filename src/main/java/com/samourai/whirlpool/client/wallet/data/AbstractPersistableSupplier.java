package com.samourai.whirlpool.client.wallet.data;

import org.slf4j.Logger;

public abstract class AbstractPersistableSupplier<D extends PersistableData>
    extends AbstractSupplier<D> {

  private AbstractPersister<D, ?> persister;

  public AbstractPersistableSupplier(
      Integer refreshDelaySeconds,
      final D fallbackValue,
      AbstractPersister<D, ?> persister,
      Logger log) {
    super(refreshDelaySeconds, fallbackValue, log);
    this.persister = persister;
  }

  @Override
  protected D fetch() throws Exception {
    D data = persister.load();
    return data;
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

  public void backup() throws Exception {
    persister.backup();
  }
}
