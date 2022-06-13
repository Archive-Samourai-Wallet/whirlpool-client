package com.samourai.whirlpool.client.wallet.data.supplier;

import com.samourai.whirlpool.client.wallet.data.dataPersister.PersistableSupplier;
import org.slf4j.Logger;

public abstract class AbstractPersistableSupplier<D extends PersistableData>
    extends BasicSupplier<D> implements PersistableSupplier {

  private IPersister<D> persister;
  private long lastWrite;

  public AbstractPersistableSupplier(IPersister<D> persister, Logger log) {
    super(log);
    this.persister = persister;
    this.lastWrite = 0;
  }

  public void load() throws Exception {
    // load only once
    D currentValue = getValue();
    if (currentValue != null) {
      throw new Exception("Cannot load(), value already loaded!");
    }

    // first load
    D initialValue = persister.read();
    setValue(initialValue);
  }

  public boolean persist(boolean force) throws Exception {
    D value = getValue();

    // check for local modifications
    if (!force && value.getLastChange() <= lastWrite) {
      return false;
    }

    persister.write(value);
    lastWrite = System.currentTimeMillis();
    return true;
  }
}
