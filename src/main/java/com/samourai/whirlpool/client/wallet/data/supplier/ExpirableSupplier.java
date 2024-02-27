package com.samourai.whirlpool.client.wallet.data.supplier;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.samourai.whirlpool.client.utils.supplier.Throwing;
import com.samourai.whirlpool.client.utils.supplier.ThrowingSupplier;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/** Supplier with expirable data. */
public abstract class ExpirableSupplier<D> extends BasicSupplier<D> {
  private static final int ATTEMPTS = 2;

  private final Integer refreshDelaySeconds; // null for non-expirable
  private final ThrowingSupplier throwingSupplier;
  private Supplier<Throwing<D, Exception>> supplier;

  protected abstract D fetch() throws Exception;

  public ExpirableSupplier(Integer refreshDelaySeconds, final Logger log) {
    super(log);
    this.refreshDelaySeconds = refreshDelaySeconds;
    this.throwingSupplier =
        new ThrowingSupplier<D, Exception>() {
          @Override
          public D getOrThrow() throws Exception {
            D result = fetch(); // throws on failure
            return result;
          }
        }.attempts(ATTEMPTS);
    resetSupplier();
  }

  private synchronized void resetSupplier() {
    // don't use ExpiringMemoizingSupplierUtil.expire() to avoid dependencies issues
    this.supplier =
        refreshDelaySeconds != null
            ? Suppliers.memoizeWithExpiration(
                throwingSupplier, refreshDelaySeconds, TimeUnit.SECONDS)
            : Suppliers.memoize(throwingSupplier);
  }

  // value will be reloaded on next load(), not on getValue()!
  protected synchronized void expire() {
    if (refreshDelaySeconds != null) {
      if (log.isDebugEnabled()) {
        log.debug("expire");
      }
      resetSupplier();
    } else {
      log.error("Cannot expire non-expirable supplier!");
    }
  }

  public synchronized void refresh() throws Exception {
    expire();
    load();
  }

  public synchronized void load() throws Exception {
    D currentValue = getValue();
    try {
      // reload value if expired
      D supplierValue = supplier.get().getOrThrow();
      if (supplierValue != currentValue) {
        setValue(supplierValue);
      }
    } catch (Exception e) {
      // fallback to last known value
      if (currentValue == null) {
        log.error("load() failure", e);
        throw e;
      } else {
        log.warn("load() failure => last value fallback", e);
      }
    }
  }
}
