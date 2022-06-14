package com.samourai.whirlpool.client.wallet.data.utxoConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignore mixsTarget & poolId for backward-compatibility
@JsonIgnoreProperties(ignoreUnknown = true)
public class UtxoConfigPersisted implements UtxoConfig {
  private int mixsDone;
  private Long expired;
  private boolean blocked;
  private String note;

  public UtxoConfigPersisted() {
    this(0, null, false, null);
  }

  public UtxoConfigPersisted(int mixsDone) {
    this(mixsDone, null, false, null);
  }

  public UtxoConfigPersisted(int mixsDone, Long expired, boolean blocked, String note) {
    this.mixsDone = mixsDone;
    this.expired = expired;
    this.blocked = blocked;
    this.note = note;
  }

  public UtxoConfigPersisted copy() {
    UtxoConfigPersisted copy =
        new UtxoConfigPersisted(this.mixsDone, this.expired, this.blocked, this.note);
    return copy;
  }

  public int getMixsDone() {
    return mixsDone;
  }

  public void setMixsDone(int mixsDone) {
    this.mixsDone = mixsDone;
  }

  public Long getExpired() {
    return expired;
  }

  public void setExpired(Long expired) {
    this.expired = expired;
  }

  public boolean isBlocked() {
    return blocked;
  }

  public void setBlocked(boolean blocked) {
    this.blocked = blocked;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  @Override
  public String toString() {
    return "mixsDone="
        + mixsDone
        + ", expired="
        + (expired != null ? expired : "null")
        + ", blocked="
        + blocked
        + ", note="
        + (note != null ? note : "null");
  }
}
