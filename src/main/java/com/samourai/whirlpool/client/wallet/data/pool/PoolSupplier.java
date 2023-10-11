package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;

public interface PoolSupplier {
  Collection<Pool> getPools(); // sorted by denomination desc

  Pool findPoolById(String poolId);

  Collection<Pool> findPoolsByMaxId(String maxPoolId); // sorted by denomination desc

  Collection<Pool> findPoolsForPremix(long utxoValue, boolean liquidity);

  Collection<Pool> findPoolsForTx0(long utxoValue);
}
