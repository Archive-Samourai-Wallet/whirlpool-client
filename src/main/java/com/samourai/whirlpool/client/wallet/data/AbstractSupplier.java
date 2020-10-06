package com.samourai.whirlpool.client.wallet.data;

import com.google.common.base.ExpiringMemoizingSupplierUtil;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.zeroleak.throwingsupplier.Throwing;
import com.zeroleak.throwingsupplier.ThrowingSupplier;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public abstract class AbstractSupplier<D> {
  private final Logger log;
  private static final int ATTEMPTS = 2;

  private final Integer refreshDelaySeconds; // null for non-expirable
  private final Supplier<Throwing<D, Exception>> supplier;
  private D value;

  private Long lastUpdate;

  protected abstract D fetch() throws Exception;

  public AbstractSupplier(
      Integer refreshDelaySeconds, final D initialValueFallback, final Logger log) {
    this.refreshDelaySeconds = refreshDelaySeconds;
    ThrowingSupplier sup =
        new ThrowingSupplier<D, Exception>() {
          @Override
          public D getOrThrow() throws Exception {
            D result = fetch(); // throws on failure
            lastUpdate = System.currentTimeMillis();
            return result;
          }
        }.attempts(ATTEMPTS);
    this.supplier =
        refreshDelaySeconds != null
            ? Suppliers.memoizeWithExpiration(sup, refreshDelaySeconds, TimeUnit.SECONDS)
            : Suppliers.memoize(sup);
    this.value = initialValueFallback;
    this.log = log;
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

  public synchronized void load() throws Exception {
    try {
      // reload value if expired
      D supplierValue = supplier.get().getOrThrow();
      if (supplierValue != value) {
        _setValue(supplierValue);
      }
    } catch (Exception e) {
      // fallback to last known value
      if (this.value == null) {
        log.error("load() failure", e);
        throw e;
      } else {
        log.warn("load() failure => last value fallback", e);
      }
    }
  }

  // for tests
  public void _setValue(D value) {
    this.value = value;
  }

  protected D getValue() {
    return value;
  }

  public Long getLastUpdate() {
    return lastUpdate;
  }
}
