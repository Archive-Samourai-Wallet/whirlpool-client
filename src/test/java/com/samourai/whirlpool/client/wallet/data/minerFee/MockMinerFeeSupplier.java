package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFee;

public class MockMinerFeeSupplier extends MinerFeeSupplier {
  private static int mockFeeValue;
  private static boolean mockException;

  private final BackendApi mockBackendApi;

  public MockMinerFeeSupplier() {
    this(1, 999999, 123);
  }

  public MockMinerFeeSupplier(int feeMin, int feeMax, int feeFallback) {
    super(feeMin, feeMax, feeFallback);
    this.mockFeeValue = feeFallback;
    this.mockBackendApi =
        new BackendApi(null, "http://mock", null) {
          @Override
          public MinerFee fetchMinerFee() throws Exception {
            if (mockException) {
              throw new Exception("mocked backend failure");
            }
            return MinerFeeSupplier.mockMinerFee(mockFeeValue);
          }
        };
  }

  @Override
  protected MinerFee fetch() throws Exception {
    return mockBackendApi.fetchMinerFee();
  }

  public void setMockFeeValue(int mockFeeValue) {
    MockMinerFeeSupplier.mockFeeValue = mockFeeValue;
  }

  public void setMockException(boolean mockException) {
    MockMinerFeeSupplier.mockException = mockException;
  }
}
