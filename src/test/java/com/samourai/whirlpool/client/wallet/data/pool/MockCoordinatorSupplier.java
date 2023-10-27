package com.samourai.whirlpool.client.wallet.data.pool;

import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorData;
import com.samourai.whirlpool.client.wallet.data.coordinator.ExpirableCoordinatorSupplier;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorSorobanMessage;
import java.util.Arrays;

public class MockCoordinatorSupplier extends ExpirableCoordinatorSupplier {

  public MockCoordinatorSupplier(
      Tx0PreviewService tx0PreviewService,
      RegisterCoordinatorSorobanMessage... registerCoordinatorSorobanMessages)
      throws Exception {
    super(9999999, null, null, tx0PreviewService);
    mock(registerCoordinatorSorobanMessages);
  }

  @Override
  protected CoordinatorData fetch() throws Exception {
    // do nothing
    return getValue();
  }

  public void mock(RegisterCoordinatorSorobanMessage... registerCoordinatorSorobanMessages)
      throws Exception {
    setValue(
        new CoordinatorData(Arrays.asList(registerCoordinatorSorobanMessages), tx0PreviewService));
  }
}
