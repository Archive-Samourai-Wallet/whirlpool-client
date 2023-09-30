package com.samourai.whirlpool.client.tx0;

import java.util.List;
import java.util.Optional;

public class Tx0PreviewResult {
  private List<Tx0Preview> tx0Previews;

  public Tx0PreviewResult(List<Tx0Preview> tx0Previews) {
    this.tx0Previews = tx0Previews;
  }

  public List<Tx0Preview> getList() {
    return tx0Previews;
  }

  public Optional<Tx0Preview> getByPoolId(String poolId) {
    return tx0Previews.stream()
        .filter(tx0Preview -> tx0Preview.getPool().getPoolId().equals(poolId))
        .findFirst();
  }

  @Override
  public String toString() {
    return tx0Previews.size() + " tx0Previews={" + tx0Previews.toString() + "}";
  }
}
