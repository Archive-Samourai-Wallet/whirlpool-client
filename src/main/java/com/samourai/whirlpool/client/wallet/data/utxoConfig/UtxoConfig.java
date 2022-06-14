package com.samourai.whirlpool.client.wallet.data.utxoConfig;

public interface UtxoConfig {
  int getMixsDone();

  void setMixsDone(int mixsDone);

  boolean isBlocked();

  void setBlocked(boolean blocked);

  String getNote();

  void setNote(String note);
}
