package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.event.PoolsChangeEvent;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.soroban.PoolInfoSorobanMessage;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirablePoolSupplier extends ExpirableSupplier<PoolData> implements PoolSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirablePoolSupplier.class);

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final SorobanClientApi sorobanClientApi;
  private RpcClient rpcClient;
  protected final Tx0PreviewService tx0PreviewService;

  public ExpirablePoolSupplier(
      int refreshPoolsDelay,
      SorobanClientApi sorobanClientApi,
      RpcClient rpcClient,
      Tx0PreviewService tx0PreviewService) {
    super(refreshPoolsDelay, log);
    this.sorobanClientApi = sorobanClientApi;
    this.rpcClient = rpcClient;
    this.tx0PreviewService = tx0PreviewService;
  }

  @Override
  protected PoolData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    try {
      Collection<PoolInfoSorobanMessage> poolInfoSorobanMessages =
          AsyncUtil.getInstance().blockingGet(sorobanClientApi.fetchPools(rpcClient));
      return new PoolData(poolInfoSorobanMessages, tx0PreviewService);
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }

  @Override
  protected void validate(PoolData value) throws Exception {
    // nothing to do
  }

  @Override
  protected void onValueChange(PoolData value) throws Exception {
    eventService.post(new PoolsChangeEvent(value));
  }

  @Override
  public Collection<Pool> getPools() {
    return getValue().getPools();
  }

  @Override
  public Pool findPoolById(String poolId) {
    return getValue().findPoolById(poolId);
  }

  @Override
  public Collection<Pool> findPoolsForPremix(final long utxoValue, final boolean liquidity) {
    return getPools().stream()
        .filter(pool -> pool.isPremix(utxoValue, liquidity))
        .collect(Collectors.<Pool>toList());
  }

  @Override
  public Collection<Pool> findPoolsForTx0(final long utxoValue) {
    return getPools().stream()
        .filter(pool -> pool.isTx0Possible(utxoValue))
        .collect(Collectors.<Pool>toList());
  }
}
