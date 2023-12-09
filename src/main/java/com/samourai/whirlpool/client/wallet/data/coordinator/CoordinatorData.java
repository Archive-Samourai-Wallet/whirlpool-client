package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.data.pool.PoolData;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorMessage;
import com.samourai.whirlpool.protocol.soroban.beans.PoolInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinatorData {
  private static final Logger log = LoggerFactory.getLogger(CoordinatorData.class);

  private Map<String, Coordinator> coordinatorsById;
  private Map<String, Coordinator> coordinatorsByPoolId;
  private PoolData poolData;

  public CoordinatorData(
      Collection<RegisterCoordinatorMessage> registerCoordinatorMessages,
      Tx0PreviewService tx0PreviewService) {
    this.coordinatorsById = computeCoordinators(registerCoordinatorMessages);
    this.coordinatorsByPoolId = new LinkedHashMap<>();
    for (Coordinator coordinator : coordinatorsById.values()) {
      for (String poolId : coordinator.getPoolIds()) {
        if (!coordinatorsByPoolId.containsKey(poolId)) {
          coordinatorsByPoolId.put(poolId, coordinator);
        } else {
          log.error(
              "Ignoring duplicate coordinator for poolId="
                  + poolId
                  + ", coordinatorId="
                  + coordinator.getCoordinatorId());
        }
      }
    }
    Collection<PoolInfo> poolInfos =
        registerCoordinatorMessages.stream()
            .flatMap(message -> message.pools.stream())
            .collect(Collectors.toList());
    this.poolData = new PoolData(poolInfos, tx0PreviewService);
    if (log.isDebugEnabled()) {
      Set<String> coordinatorIds = coordinatorsById.keySet();
      Set<String> poolIds =
          poolData.getPools().stream().map(pool -> pool.getPoolId()).collect(Collectors.toSet());
      log.debug(
          "CoordinatorData: "
              + coordinatorIds.size()
              + " coordinators "
              + coordinatorIds.toString()
              + ", "
              + poolIds.size()
              + " pools "
              + poolIds.toString());
      for (Coordinator coordinator : coordinatorsById.values()) {
        log.debug(" * " + coordinator.toString());
      }
    }
  }

  private static Map<String, Coordinator> computeCoordinators(
      Collection<RegisterCoordinatorMessage> registerCoordinatorMessages) {
    return registerCoordinatorMessages.stream()
        .map(
            message -> {
              Collection<String> poolIds =
                  message.pools.stream().map(pool -> pool.poolId).collect(Collectors.toList());
              return new Coordinator(
                  message.coordinator.coordinatorId,
                  new PaymentCode(message.coordinator.paymentCode),
                  poolIds);
            })
        .collect(
            Collectors.toMap(
                coordinator -> coordinator.getCoordinatorId(), coordinator -> coordinator));
  }

  public Collection<Coordinator> getCoordinators() {
    return coordinatorsById.values();
  }

  public Coordinator findCoordinatorById(String coordinatorId) {
    return coordinatorsById.get(coordinatorId);
  }

  public Coordinator findCoordinatorByPoolId(String poolId) {
    return coordinatorsByPoolId.get(poolId);
  }

  public PoolData getPoolData() {
    return poolData;
  }
}
