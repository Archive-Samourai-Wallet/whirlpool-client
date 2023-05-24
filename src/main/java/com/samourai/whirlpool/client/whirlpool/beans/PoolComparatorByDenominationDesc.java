package com.samourai.whirlpool.client.whirlpool.beans;

import java.util.Comparator;

public class PoolComparatorByDenominationDesc implements Comparator<Pool> {
  @Override
  public int compare(Pool o1, Pool o2) {
    return o1.getDenomination() < o2.getDenomination() ? 1 : -1;
  }
}
