package com.samourai.whirlpool.client.wallet.data;

import com.google.common.base.ExpiringMemoizingSupplierUtil;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.zeroleak.throwingsupplier.Throwing;
import com.zeroleak.throwingsupplier.ThrowingSupplier;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/** Supplier with expirable data. */
public abstract class ExpirableSupplier<D> extends BasicSupplier<D> implements LoadableSupplier {
  private static final int ATTEMPTS = 2;

  private final Integer refreshDelaySeconds; // null for non-expirable
  private final Supplier<Throwing<D, Exception>> supplier;

  protected abstract D fetch() throws Exception;

  public ExpirableSupplier(
      Integer refreshDelaySeconds, final D initialValueFallback, final Logger log) {
    super(log, initialValueFallback);
    this.refreshDelaySeconds = refreshDelaySeconds;
    ThrowingSupplier sup =
        new ThrowingSupplier<D, Exception>() {
          @Override
          public D getOrThrow() throws Exception {
            D result = fetch(); // throws on failure
            return result;
          }
        }.attempts(ATTEMPTS);
    this.supplier =
        refreshDelaySeconds != null
            ? Suppliers.memoizeWithExpiration(sup, refreshDelaySeconds, TimeUnit.SECONDS)
            : Suppliers.memoize(sup);
  }

  public void expire() {
    if (refreshDelaySeconds != null) {
      if (log.isDebugEnabled()) {
        log.debug("expire");
      }
      ExpiringMemoizingSupplierUtil.expire(this.supplier);
    } else {
      log.error("Cannot expire non-expirable supplier!");
    }
  }

  @Override
  public synchronized void load() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("load()");
    }
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
