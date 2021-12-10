package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.rest.PoolInfo;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;

public class MockPoolSupplier extends ExpirablePoolSupplier {
  private static PoolsResponse poolsResponse;

  public MockPoolSupplier(Tx0PreviewService tx0PreviewService, PoolInfo... pools) throws Exception {
    super(9999999, computeServerApi(), tx0PreviewService);
    mock(pools);
  }

  private static ServerApi computeServerApi() {
    return new ServerApi("http://mock", null, null) {

      @Override
      public PoolsResponse fetchPools() throws Exception {
        return poolsResponse;
      }
    };
  }

  public void mock(PoolInfo... pools) throws Exception {
    poolsResponse = new PoolsResponse(pools);
    setValue(new PoolData(poolsResponse, tx0PreviewService));
  }
}
