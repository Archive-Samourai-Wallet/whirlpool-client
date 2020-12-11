package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.data.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.LoadableSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolSupplier extends ExpirableSupplier<PoolData> implements LoadableSupplier {
  private static final Logger log = LoggerFactory.getLogger(PoolSupplier.class);

  private final ServerApi serverApi;

  public PoolSupplier(int refreshPoolsDelay, ServerApi serverApi) {
    super(refreshPoolsDelay, null, log);
    this.serverApi = serverApi;
  }

  @Override
  protected PoolData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    try {
      PoolsResponse poolsResponse = serverApi.fetchPools();
      return new PoolData(poolsResponse);
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }

  public Collection<Pool> getPools() {
    return getValue().getPools();
  }

  public Pool findPoolById(String poolId) {
    return getValue().findPoolById(poolId);
  }

  public Collection<Pool> findPoolsForPremix(long utxoValue, boolean liquidity) {
    // find eligible pools
    List<Pool> poolsAccepted = new ArrayList<Pool>();
    for (Pool pool : getPools()) {
      if (pool.checkInputBalance(utxoValue, liquidity)) {
        poolsAccepted.add(pool);
      }
    }
    return poolsAccepted;
  }
}
