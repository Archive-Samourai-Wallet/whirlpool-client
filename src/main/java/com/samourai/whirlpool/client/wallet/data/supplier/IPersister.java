package com.samourai.whirlpool.client.wallet.data.supplier;

public interface IPersister<D extends PersistableData> {
  D read() throws Exception;

  void write(D data) throws Exception;
}
