package com.samourai.whirlpool.client.wallet.orchestrator;

import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.wallet.data.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistOrchestrator extends AbstractOrchestrator {
  private final Logger log = LoggerFactory.getLogger(PersistOrchestrator.class);
  private final Collection<AbstractPersistableSupplier> suppliers;

  public PersistOrchestrator(int loopDelay, Collection<AbstractSupplier> suppliers) {
    super(loopDelay, 0, null);
    this.suppliers = filterPersistableSuppliers(suppliers);
  }

  private static Collection<AbstractPersistableSupplier> filterPersistableSuppliers(
      Collection<AbstractSupplier> suppliers) {
    List<AbstractPersistableSupplier> list = new LinkedList<AbstractPersistableSupplier>();
    for (AbstractSupplier supplier : suppliers) {
      if (supplier instanceof AbstractPersistableSupplier) {
        list.add((AbstractPersistableSupplier) supplier);
      }
    }
    return list;
  }

  @Override
  protected void runOrchestrator() {
    try {
      persist(false);
      setLastRun();
    } catch (Exception e) {
      log.error("", e);
    }
  }

  @Override
  public synchronized void stop() {
    super.stop();

    // persist before exit
    try {
      persist(false);
    } catch (Exception e) {
      log.error("", e);
    }
  }

  public void backup() throws Exception {
    // backup or fail
    for (AbstractPersistableSupplier supplier : suppliers) {
      supplier.backup();
    }
  }

  public void persistInitialData() throws Exception {
    // forced persist or fail
    persist(true);
  }

  private void persist(boolean force) throws Exception {
    // persist or fail
    for (AbstractPersistableSupplier supplier : suppliers) {
      supplier.persist(force);
    }
  }
}
