package com.samourai.whirlpool.client.tx0;

import java.util.List;
import java.util.Optional;

public class Tx0Result {
  private List<Tx0> tx0s;

  public Tx0Result(List<Tx0> tx0s) {
    this.tx0s = tx0s;
  }

  public List<Tx0> getList() {
    return tx0s;
  }

  public Optional<Tx0> getByPoolId(String poolId) {
    return tx0s.stream().filter(Tx0 -> Tx0.getPool().getPoolId().equals(poolId)).findFirst();
  }

  @Override
  public String toString() {
    return tx0s.size() + " tx0s={" + tx0s.toString() + "}";
  }
}
