package com.samourai.whirlpool.client.whirlpool;

import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import java.util.Collection;

public class RpcSessionClient extends RpcSession {
  private Coordinator coordinator;

  public RpcSessionClient(RpcSession copy) {
    super(copy);
  }

  public void setCoordinator(Coordinator coordinator) {
    this.coordinator = coordinator;
  }

  @Override
  public Collection<String> getSorobanUrlsUp() {
    if (coordinator != null) {
      return coordinator.getSorobanNodeUrls();
    }
    return super.getSorobanUrlsUp();
  }
}
