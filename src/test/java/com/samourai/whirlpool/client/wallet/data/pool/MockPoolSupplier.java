package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.protocol.soroban.PoolInfoSorobanMessage;
import java.util.Arrays;

public class MockPoolSupplier extends ExpirablePoolSupplier {

  public MockPoolSupplier(
      Tx0PreviewService tx0PreviewService,
      SorobanClientApi sorobanClientApi,
      PoolInfoSorobanMessage... poolInfoSorobanMessages)
      throws Exception {
    super(9999999, sorobanClientApi, null, tx0PreviewService);
    mock(poolInfoSorobanMessages);
  }

  @Override
  protected PoolData fetch() throws Exception {
    // do nothing
    return getValue();
  }

  public void mock(PoolInfoSorobanMessage... poolInfoSorobanMessages) throws Exception {
    setValue(new PoolData(Arrays.asList(poolInfoSorobanMessages), tx0PreviewService));
  }
}
