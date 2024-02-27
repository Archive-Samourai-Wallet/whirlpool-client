package com.samourai.whirlpool.client.utils.supplier;

import java.util.Optional;
import java.util.function.Supplier;

public class Throwing<T, E extends Exception> extends Either<T, E> {

  public Throwing(T value) {
    super(value);
  }

  public Throwing(Exception e) {
    super(Optional.empty(), (E) e);
  }

  public T getOrThrow() throws Exception {
    return getValue().orElseThrow((Supplier<Exception>) () -> getFallback().get());
  }
}
