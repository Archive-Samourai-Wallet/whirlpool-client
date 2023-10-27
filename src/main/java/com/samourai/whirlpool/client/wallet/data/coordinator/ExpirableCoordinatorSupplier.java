package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.Util;
import com.samourai.whirlpool.client.event.PoolsChangeEvent;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.pool.PoolData;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorSorobanMessage;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirableCoordinatorSupplier extends ExpirableSupplier<CoordinatorData>
    implements CoordinatorSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirableCoordinatorSupplier.class);
  private static final int COORDINATOR_ATTEMPTS = 5;

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final SorobanClientApi sorobanClientApi;
  private RpcSession rpcSession;
  protected final Tx0PreviewService tx0PreviewService;

  public ExpirableCoordinatorSupplier(
      int refreshPoolsDelay,
      SorobanClientApi sorobanClientApi,
      RpcSession rpcSession,
      Tx0PreviewService tx0PreviewService) {
    super(refreshPoolsDelay, log);
    this.sorobanClientApi = sorobanClientApi;
    this.rpcSession = rpcSession;
    this.tx0PreviewService = tx0PreviewService;
  }

  @Override
  protected CoordinatorData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching coordinators...");
    }
    try {
      return rpcSession.withRpcClient(
          rpcClient -> {
            Collection<RegisterCoordinatorSorobanMessage> registerCoordinatorSorobanMessages =
                AsyncUtil.getInstance().blockingGet(sorobanClientApi.fetchCoordinators(rpcClient));
            CoordinatorData coordinatorData =
                new CoordinatorData(registerCoordinatorSorobanMessages, tx0PreviewService);

            if (coordinatorData.getCoordinators().isEmpty()) {
              // immediately retry on another SorobanServer as current one it may be out-of-sync
              throw new HttpException("No Whirlpool coordinator found, retrying...");
            }

            // close RpcSession after each cycle to use multiple SorobanServers randomly
            rpcSession.close();
            return coordinatorData;
          });
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }

  @Override
  protected void validate(CoordinatorData value) throws Exception {
    // nothing to do
  }

  @Override
  protected void onValueChange(CoordinatorData value) throws Exception {
    eventService.post(new PoolsChangeEvent(value.getPoolData()));
  }

  protected PoolData getPoolData() {
    return getValue().getPoolData();
  }

  //

  @Override
  public Collection<Pool> getPools() {
    return getPoolData().getPools();
  }

  @Override
  public Pool findPoolById(String poolId) {
    return getPoolData().findPoolById(poolId);
  }

  @Override
  public Collection<Pool> findPoolsByMaxId(String maxPoolId) {
    long highestPoolDenomination = findPoolById(maxPoolId).getDenomination();
    return getPools().stream()
        .filter(pool -> pool.getDenomination() <= highestPoolDenomination)
        .collect(Collectors.toList());
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

  @Override
  public <R> R withCoordinator(CallbackWithArg<Coordinator, R> callable) throws Exception {
    return Util.retryOnHttpException(() -> getCoordinatorRandom(), callable, COORDINATOR_ATTEMPTS);
  }

  //

  @Override
  public Coordinator getCoordinatorRandom() {
    if (getCoordinators().isEmpty()) {
      throw new RuntimeException("No Whirlpool coordinator found, please retry later");
    }
    Coordinator coordinator = RandomUtil.getInstance().next(getCoordinators());
    if (log.isDebugEnabled()) {
      System.err.println(
          "NEXT "
              + getCoordinators().size()
              + " = "
              + (coordinator != null ? "coordinator" : "null"));
      log.debug("Using coordinator: " + coordinator.getUrlClear());
    }
    return coordinator;
  }

  @Override
  public Collection<Coordinator> getCoordinators() {
    if (getValue().getCoordinators().isEmpty()) {
      try {
        refresh();
      } catch (Exception e) {
        log.error("", e);
      }
    }
    return getValue().getCoordinators();
  }

  @Override
  public Coordinator findCoordinatorById(String coordinatorId) {
    return getValue().findCoordinatorById(coordinatorId);
  }

  @Override
  public Coordinator findCoordinatorByPoolId(String poolId) {
    return getValue().findCoordinatorByPoolId(poolId);
  }
}
