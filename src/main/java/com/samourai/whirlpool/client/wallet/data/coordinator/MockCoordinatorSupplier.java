package com.samourai.whirlpool.client.wallet.data.coordinator;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanServerDex;
import com.samourai.wallet.util.Pair;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.CoordinatorMessage;
import com.samourai.whirlpool.protocol.soroban.payload.coordinators.SorobanInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class MockCoordinatorSupplier extends ExpirableCoordinatorSupplier {

  private static final String SOROBAN_COORDINATORS =
      "{\"typePayload\":\"com.samourai.whirlpool.protocol.soroban.coordinators.CoordinatorMessage\",\"coordinator\":{\"coordinatorId\":\"test\",\"paymentCode\":\"PM8TJbDtnHjxaFrFUeFcyRDrR3yhedohPPMqdkcy56vrU6gcyPykqyYBpA8Uk85uVwhKgnqj6W9TkPMJmBSYPZw7VPTAZKYe2CXZMoq6J9mdMDLchCdE\",\"paymentCodeSignature\":\"H+yIuNtW+fT5RvOrC1oMejEXG0FCdKRhyb9DU2O8Nm3SHt1VWzO8tG19/LnTlx0PsZzYXYC5rXQBS7qEMecuytI=\"},\"pools\":[{\"poolId\":\"0.01btc\",\"denomination\":1000000,\"feeValue\":42500,\"premixValue\":1000262,\"premixValueMin\":1000102,\"premixValueMax\":1008000,\"tx0MaxOutputs\":70,\"anonymitySet\":5},{\"poolId\":\"0.001btc\",\"denomination\":100000,\"feeValue\":5000,\"premixValue\":100262,\"premixValueMin\":100102,\"premixValueMax\":108000,\"tx0MaxOutputs\":25,\"anonymitySet\":5},{\"poolId\":\"0.05btc\",\"denomination\":5000000,\"feeValue\":148750,\"premixValue\":5000262,\"premixValueMin\":5000102,\"premixValueMax\":5008000,\"tx0MaxOutputs\":70,\"anonymitySet\":5},{\"poolId\":\"0.5btc\",\"denomination\":50000000,\"feeValue\":1487500,\"premixValue\":50000262,\"premixValueMin\":50000102,\"premixValueMax\":50008000,\"tx0MaxOutputs\":70,\"anonymitySet\":5}]}";

  public MockCoordinatorSupplier(Tx0PreviewService tx0PreviewService, WhirlpoolWalletConfig config)
      throws Exception {
    super(9999999, null, tx0PreviewService, config);

    CoordinatorMessage coordinatorMessage =
        ClientUtils.fromJson(SOROBAN_COORDINATORS, CoordinatorMessage.class);
    mock(coordinatorMessage);
  }

  @Override
  protected CoordinatorData fetch() throws Exception {
    // do nothing
    return getValue();
  }

  public void mock(CoordinatorMessage... coordinatorMessages) throws Exception {
    Collection<Pair<CoordinatorMessage, PaymentCode>> pairs =
        Arrays.stream(coordinatorMessages)
            .map(
                m ->
                    Pair.of(
                        m,
                        // TODO
                        new PaymentCode(
                            "PM8TJbDtnHjxaFrFUeFcyRDrR3yhedohPPMqdkcy56vrU6gcyPykqyYBpA8Uk85uVwhKgnqj6W9TkPMJmBSYPZw7VPTAZKYe2CXZMoq6J9mdMDLchCdE")))
            .collect(Collectors.toList());
    SorobanServerDex sorobanServerDex =
        SorobanServerDex.get(config.getSamouraiNetwork().getParams());
    for (Pair<CoordinatorMessage, PaymentCode> pair : pairs) {
      pair.getLeft().sorobanInfo =
          new SorobanInfo(
              sorobanServerDex.getSorobanUrlsClear(), sorobanServerDex.getSorobanUrlsOnion());
    }
    setValue(new CoordinatorData(pairs, tx0PreviewService, config.isTorOnionCoordinator()));
  }
}
