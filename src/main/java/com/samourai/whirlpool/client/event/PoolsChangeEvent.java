package com.samourai.whirlpool.client.event;

import com.samourai.whirlpool.client.wallet.beans.WhirlpoolEvent;
import com.samourai.whirlpool.client.wallet.data.pool.PoolData;

public class PoolsChangeEvent extends WhirlpoolEvent {
  private PoolData poolData;

  public PoolsChangeEvent(PoolData poolData) {
    super();
    this.poolData = poolData;
  }

  public PoolData getPoolData() {
    return poolData;
  }
}
