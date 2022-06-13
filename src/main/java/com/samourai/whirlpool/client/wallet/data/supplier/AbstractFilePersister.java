package com.samourai.whirlpool.client.wallet.data.supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.wallet.util.SystemUtil;
import java.io.File;
import org.slf4j.Logger;

public abstract class AbstractFilePersister<D extends PersistableData, P> implements IPersister<D> {
  private final Logger log;

  private String fileName;
  private TypeReference<P> typePersisted;

  private final ObjectMapper mapper;

  protected abstract D getInitialValue() throws Exception;

  protected abstract D fromPersisted(P persisted) throws Exception;

  protected abstract P toPersisted(D data) throws Exception;

  public AbstractFilePersister(String fileName, TypeReference<P> typePersisted, Logger log) {
    this.log = log;
    this.typePersisted = typePersisted;
    this.fileName = fileName;

    this.mapper = new ObjectMapper();
  }

  @Override
  public synchronized D read() throws Exception {
    File file = getFile();

    // empty file => use initial value
    if (file.length() == 0) {
      if (log.isDebugEnabled()) {
        log.debug("File " + fileName + " not present => using initial value");
      }
      return getInitialValue();
    }

    // read json
    P persisted = mapper.readValue(file, typePersisted);
    D data = fromPersisted(persisted);
    if (log.isDebugEnabled()) {
      log.debug("Loading " + fileName + " => " + data);
    }
    return data;
  }

  @Override
  public synchronized void write(D data) throws Exception {
    doWrite(data);
  }

  protected void doWrite(D data) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Writing " + fileName + " => " + data);
    }
    File file = getFile();

    // write json
    P persisted = toPersisted(data);
    SystemUtil.safeWriteValue(file, mapper, persisted);
  }

  private File getFile() throws Exception {
    File file = new File(fileName);
    if (!file.exists()) {
      throw new Exception("File not found: " + fileName);
    }
    return file;
  }
}
