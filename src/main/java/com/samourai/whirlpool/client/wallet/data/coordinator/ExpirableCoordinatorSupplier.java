package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.*;
import com.samourai.whirlpool.client.event.PoolsChangeEvent;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import com.samourai.whirlpool.client.wallet.data.pool.PoolData;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorMessage;
import com.samourai.whirlpool.protocol.soroban.api.WhirlpoolApiClient;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirableCoordinatorSupplier extends ExpirableSupplier<CoordinatorData>
    implements CoordinatorSupplier {
  private static final Logger log = LoggerFactory.getLogger(ExpirableCoordinatorSupplier.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private static final int COORDINATOR_ATTEMPTS = 5;

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final WhirlpoolApiClient whirlpoolApiClient;
  private WhirlpoolNetwork whirlpoolNetwork;
  protected final Tx0PreviewService tx0PreviewService;

  public ExpirableCoordinatorSupplier(
      int refreshPoolsDelay,
      WhirlpoolApiClient whirlpoolApiClient,
      WhirlpoolNetwork whirlpoolNetwork,
      Tx0PreviewService tx0PreviewService) {
    super(refreshPoolsDelay, log);
    this.whirlpoolApiClient = whirlpoolApiClient;
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.tx0PreviewService = tx0PreviewService;
  }

  protected boolean validateCoordinator(RegisterCoordinatorMessage payload) {
    // check coordinator.paymentCodeSignature against samourai signature
    if (!messageSignUtil.verifySignedMessage(
        whirlpoolNetwork.getSigningAddress(),
        payload.coordinator.paymentCode,
        payload.coordinator.paymentCodeSignature,
        whirlpoolNetwork.getParams())) {
      if (log.isDebugEnabled()) {
        log.warn("Could not trust coordinator: invalid coordinator.paymentCodeSignature");
      }
    }
    return true;
  }

  @Override
  protected CoordinatorData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching coordinators...");
    }
    try {
      Collection<RegisterCoordinatorMessage> registerCoordinatorMessages =
          asyncUtil.blockingGet(whirlpoolApiClient.fetchCoordinators()).stream()
              .filter(this::validateCoordinator)
              .collect(Collectors.toList());
      CoordinatorData coordinatorData =
          new CoordinatorData(registerCoordinatorMessages, tx0PreviewService);

      if (coordinatorData.getCoordinators().isEmpty()) {
        // immediately retry on another SorobanServer as current one it may be out-of-sync
        throw new HttpException("No Whirlpool coordinator found, retrying...");
      }
      return coordinatorData;
    } catch (HttpException e) { // TODO ?
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
  public <R> R withCoordinatorRandom(CallbackWithArg<Coordinator, R> callable) throws Exception {
    return Util.retryOnHttpException(() -> getCoordinatorRandom(), callable, COORDINATOR_ATTEMPTS);
  }

  //

  @Override
  public Coordinator getCoordinatorRandom() {
    if (getCoordinators().isEmpty()) {
      throw new RuntimeException(
          "No Whirlpool coordinator found, please retry later or check for upgrade");
    }
    Coordinator coordinator = RandomUtil.getInstance().next(getCoordinators());
    if (log.isDebugEnabled()) {
      log.debug("Using coordinator: " + coordinator.getCoordinatorId());
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
  public Coordinator findCoordinatorByPoolId(String poolId) {
    return getValue().findCoordinatorByPoolId(poolId);
  }

  @Override
  public Coordinator findCoordinatorByPoolIdOrThrow(String poolId) throws NotifiableException {
    Coordinator c = findCoordinatorByPoolId(poolId);
    if (c == null) {
      throw new NotifiableException(
          "No coordinator available for pool "
              + poolId
              + ", please retry later or check for upgrade");
    }
    return c;
  }
}
