package com.samourai.whirlpool.client.whirlpool.beans;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Coordinator {
  private final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private String coordinatorId;
  private String urlClear;
  private String urlOnion;
  private Collection<String> poolIds;

  public Coordinator(
      String coordinatorId, String urlClear, String urlOnion, Collection<String> poolIds) {
    this.coordinatorId = coordinatorId;
    this.urlClear = urlClear;
    this.urlOnion = urlOnion;
    this.poolIds = poolIds;
  }

  public String getCoordinatorId() {
    return coordinatorId;
  }

  public String getUrlClear() {
    return urlClear;
  }

  public String getUrlOnion() {
    return urlOnion;
  }

  public Collection<String> getPoolIds() {
    return poolIds;
  }
}
