package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import java.util.Collection;

public interface CoordinatorSupplier extends PoolSupplier {
  Collection<Coordinator> getCoordinators();

  Coordinator getCoordinatorRandom();

  Coordinator findCoordinatorByPoolId(String poolId);

  Coordinator findCoordinatorByPoolIdOrThrow(String poolId) throws NotifiableException;

  <R> R withCoordinatorRandom(CallbackWithArg<Coordinator, R> callable) throws Exception;

  void refresh() throws Exception;

  void load() throws Exception;
}
