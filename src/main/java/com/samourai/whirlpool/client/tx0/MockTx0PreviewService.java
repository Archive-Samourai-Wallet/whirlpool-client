package com.samourai.whirlpool.client.tx0;

import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.soroban.api.WhirlpoolPartnerApiClient;
import io.reactivex.Single;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockTx0PreviewService extends Tx0PreviewService {
  private Logger log = LoggerFactory.getLogger(MockTx0PreviewService.class);

  private Collection<Tx0Data> mockTx0Datas = null;

  public MockTx0PreviewService(MinerFeeSupplier minerFeeSupplier, ITx0PreviewServiceConfig config) {
    super(minerFeeSupplier, config);
    this.mockTx0Datas = null;
  }

  @Override
  protected Single<Collection<Tx0Data>> fetchTx0Data(
      String partnerId, boolean cascading, WhirlpoolPartnerApiClient whirlpoolPartnerApiClient) {
    if (mockTx0Datas != null) {
      return Single.just(mockTx0Datas);
    }
    return super.fetchTx0Data(partnerId, cascading, whirlpoolPartnerApiClient);
  }

  public void setMockTx0Datas(Collection<Tx0Data> mockTx0Datas) {
    this.mockTx0Datas = mockTx0Datas;
  }
}
