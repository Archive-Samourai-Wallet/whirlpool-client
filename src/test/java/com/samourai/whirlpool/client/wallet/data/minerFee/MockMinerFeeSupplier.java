package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFee;

public class MockMinerFeeSupplier extends MinerFeeSupplier {
  private static int mockFeeValue;
  private static boolean mockException;

  public MockMinerFeeSupplier() {
    this(1, 999999, 123);
  }

  public MockMinerFeeSupplier(int feeMin, int feeMax, int feeFallback) {
    super(9999999, computeBackendApi(), feeMin, feeMax, feeFallback);
  }

  private static BackendApi computeBackendApi() {
    return new BackendApi(null, "http://mock", null) {
      @Override
      public MinerFee fetchMinerFee() throws Exception {
        if (mockException) {
          throw new Exception("mocked backend failure");
        }
        return MinerFeeSupplier.mockMinerFee(mockFeeValue);
      }
    };
  }

  public void setMockFeeValue(int mockFeeValue) {
    MockMinerFeeSupplier.mockFeeValue = mockFeeValue;
  }

  public void setMockException(boolean mockException) {
    MockMinerFeeSupplier.mockException = mockException;
  }
}
