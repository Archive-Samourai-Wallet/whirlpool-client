package com.samourai.whirlpool.client.wallet.orchestrator;

import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.LoadableSupplier;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataOrchestrator extends AbstractOrchestrator {
  private final Logger log = LoggerFactory.getLogger(DataOrchestrator.class);
  private final Collection<AbstractSupplier> suppliers;

  public DataOrchestrator(int loopDelay, Collection<AbstractSupplier> suppliers) {
    super(loopDelay, 0, null);
    this.suppliers = suppliers;
  }

  @Override
  protected void runOrchestrator() {
    // process each supplier
    if (log.isDebugEnabled()) {
      log.debug("Refreshing data...");
    }
    for (AbstractSupplier supplier : suppliers) {
      if (supplier instanceof ExpirableSupplier) {
        // refresh data when expired or ignoring errors
        try {
          ((ExpirableSupplier) supplier).load();
        } catch (Exception e) {
          log.error("supplier.load failed", e);
        }
      }
    }
  }

  public void loadInitialData() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Loading initial data...");
    }

    // load initial data or fail
    for (AbstractSupplier supplier : suppliers) {
      if (supplier instanceof LoadableSupplier) {
        if (log.isDebugEnabled()) {
          log.debug("Loading: " + supplier.getClass().getName());
        }
        ((LoadableSupplier) supplier).load();
      }
    }
  }

  @Override
  public synchronized void notifyOrchestrator() {
    super.notifyOrchestrator();
  }
}
