package com.samourai.whirlpool.client.wallet.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.io.File;
import org.slf4j.Logger;

public abstract class AbstractPersister<D extends PersistableData, P> {
  private final Logger log;

  private String fileName;
  private TypeReference<P> typePersisted;

  private final ObjectMapper mapper;
  private long lastWrite;

  protected abstract D getInitialValue() throws Exception;

  protected abstract D fromPersisted(P persisted) throws Exception;

  protected abstract P toPersisted(D data) throws Exception;

  public AbstractPersister(String fileName, TypeReference<P> typePersisted, Logger log) {
    this.log = log;
    this.typePersisted = typePersisted;
    this.fileName = fileName;

    this.mapper = new ObjectMapper();
    this.lastWrite = 0;
  }

  public synchronized D load() throws Exception {
    File file = getFile();

    // empty file => use initial value
    if (file.length() == 0) {
      if (log.isDebugEnabled()) {
        log.debug("Using initial value for: " + fileName);
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

  public synchronized void write(D data) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Writing " + fileName + " => " + data);
    }
    File file = getFile();

    // write json
    P persisted = toPersisted(data);
    ClientUtils.safeWriteValue(mapper, persisted, file);
    lastWrite = System.currentTimeMillis();
  }

  public synchronized void backup() throws Exception {
    File file = getFile();
    File fileBackup = new File(fileName + ".bak");
    if (log.isDebugEnabled()) {
      log.debug("Backup => " + fileBackup.getAbsolutePath());
    }
    Files.copy(file, fileBackup);
  }

  private File getFile() throws Exception {
    File file = new File(fileName);
    if (!file.exists()) {
      throw new Exception("File not found: " + fileName);
    }
    return file;
  }

  public long getLastWrite() {
    return lastWrite;
  }
}