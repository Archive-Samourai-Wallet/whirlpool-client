package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolPoolByBalanceMinDescComparator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.PoolInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolData {
  private static final Logger log = LoggerFactory.getLogger(PoolData.class);

  private final Map<String, Pool> poolsById;

  public PoolData(Collection<PoolInfo> poolInfos, Tx0PreviewService tx0PreviewService) {
    this.poolsById = computePools(poolInfos, tx0PreviewService);
  }

  private static Map<String, Pool> computePools(
      Collection<PoolInfo> poolInfos, final Tx0PreviewService tx0PreviewService) {

    // biggest balanceMin first
    List<Pool> poolsOrdered =
        poolInfos.stream()
            .map(
                poolInfo -> {
                  Pool pool = new Pool();
                  pool.setPoolId(poolInfo.poolId);
                  pool.setDenomination(poolInfo.denomination);
                  pool.setFeeValue(poolInfo.feeValue);
                  pool.setPremixValue(poolInfo.premixValue);
                  pool.setPremixValueMin(poolInfo.premixValueMin);
                  pool.setPremixValueMax(poolInfo.premixValueMax);
                  pool.setTx0MaxOutputs(poolInfo.tx0MaxOutputs);
                  pool.setAnonymitySet(poolInfo.anonymitySet);
                  return pool;
                })
            .sorted(new WhirlpoolPoolByBalanceMinDescComparator())
            .collect(Collectors.toList());

    // compute & set tx0PreviewMin
    Tx0PreviewConfig tx0PreviewConfig = new Tx0PreviewConfig(Tx0FeeTarget.MIN, Tx0FeeTarget.MIN);
    final Tx0Previews tx0PreviewsMin =
        tx0PreviewService.tx0PreviewsMinimal(poolsOrdered, tx0PreviewConfig);
    for (Pool pool : poolsOrdered) {
      Tx0Preview tx0PreviewMin = tx0PreviewsMin.getTx0Preview(pool.getPoolId());
      pool.setTx0PreviewMin(tx0PreviewMin);
    }

    // map by id
    Map<String, Pool> poolsById = new LinkedHashMap<String, Pool>();
    for (Pool pool : poolsOrdered) {
      poolsById.put(pool.getPoolId(), pool);
    }
    return poolsById;
  }

  public Collection<Pool> getPools() {
    return poolsById.values();
  }

  public Pool findPoolById(String poolId) {
    return poolsById.get(poolId);
  }
}
