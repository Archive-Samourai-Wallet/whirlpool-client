package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.data.pool.PoolData;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.CoordinatorMessage;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.PoolInfo;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.SorobanInfo;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinatorData {
  private static final Logger log = LoggerFactory.getLogger(CoordinatorData.class);

  private Map<String, Coordinator> coordinatorsBySender;
  private Map<String, Coordinator> coordinatorsByPoolId;
  private PoolData poolData;

  public CoordinatorData(
      Collection<Pair<CoordinatorMessage, PaymentCode>> coordinatorMessages,
      Tx0PreviewService tx0PreviewService,
      boolean onion) {
    this.coordinatorsBySender = computeCoordinators(coordinatorMessages, onion);
    this.coordinatorsByPoolId = new LinkedHashMap<>();
    for (Coordinator coordinator : coordinatorsBySender.values()) {
      for (String poolId : coordinator.getPoolIds()) {
        if (!coordinatorsByPoolId.containsKey(poolId)) {
          coordinatorsByPoolId.put(poolId, coordinator);
        } else {
          log.error(
              "Ignoring duplicate coordinator for poolId="
                  + poolId
                  + ", sender="
                  + coordinator.getSender());
        }
      }
    }
    Collection<PoolInfo> poolInfos =
        coordinatorMessages.stream()
            .flatMap(message -> message.getLeft().pools.stream())
            .collect(Collectors.toList());
    this.poolData = new PoolData(poolInfos, tx0PreviewService);
    if (log.isDebugEnabled()) {
      Set<String> coordinatorIds = coordinatorsBySender.keySet();
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
      for (Coordinator coordinator : coordinatorsBySender.values()) {
        log.debug(" * " + coordinator.toString());
      }
    }
  }

  private static Map<String, Coordinator> computeCoordinators(
      Collection<Pair<CoordinatorMessage, PaymentCode>> coordinatorMessages, boolean onion) {
    return coordinatorMessages.stream()
        .map(
            message -> {
              Collection<String> poolIds =
                  message.getLeft().pools.stream()
                      .map(pool -> pool.poolId)
                      .collect(Collectors.toList());
              SorobanInfo sorobanInfo = message.getLeft().sorobanInfo;
              Collection<String> sorobanUrls =
                  onion ? sorobanInfo.urlsOnion : sorobanInfo.urlsClear;
              return new Coordinator(
                  message.getLeft().coordinator.name, message.getRight(), poolIds, sorobanUrls);
            })
        .collect(
            Collectors.toMap(
                coordinator -> coordinator.getSender().toString(), coordinator -> coordinator));
  }

  public Collection<Coordinator> getCoordinators() {
    return coordinatorsBySender.values();
  }

  public Coordinator findCoordinatorById(String coordinatorId) {
    return coordinatorsBySender.get(coordinatorId);
  }

  public Coordinator findCoordinatorByPoolId(String poolId) {
    return coordinatorsByPoolId.get(poolId);
  }

  public PoolData getPoolData() {
    return poolData;
  }
}
