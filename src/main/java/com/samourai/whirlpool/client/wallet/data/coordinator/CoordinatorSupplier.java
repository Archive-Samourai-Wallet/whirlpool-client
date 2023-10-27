package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import java.util.Collection;

public interface CoordinatorSupplier extends PoolSupplier {
  Collection<Coordinator> getCoordinators();

  Coordinator getCoordinatorRandom();

  Coordinator findCoordinatorById(String coordinatorId);

  Coordinator findCoordinatorByPoolId(String poolId);

  <R> R withCoordinator(CallbackWithArg<Coordinator, R> callable) throws Exception;

  void refresh() throws Exception;
}
