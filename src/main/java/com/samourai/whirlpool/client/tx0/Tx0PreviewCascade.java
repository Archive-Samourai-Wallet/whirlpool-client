package com.samourai.whirlpool.client.tx0;

import java.util.List;

public class Tx0PreviewCascade {
  private List<Tx0Preview> tx0Previews;

  public Tx0PreviewCascade(List<Tx0Preview> tx0Previews) {
    this.tx0Previews = tx0Previews;
  }

  public List<Tx0Preview> getTx0Previews() {
    return tx0Previews;
  }

  @Override
  public String toString() {
    return "tx0Previews={" + tx0Previews.toString() + "}";
  }
}
