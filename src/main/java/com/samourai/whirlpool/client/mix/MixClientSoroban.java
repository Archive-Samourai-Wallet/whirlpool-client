package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClientSoroban {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();
  private static final long REGISTER_INPUT_DELAY_MS = 30000;

  private SorobanClientApi sorobanClientApi;
  private RpcClientEncrypted rpcClient;
  private PaymentCode paymentCodeCoordinator;

  public MixClientSoroban(
      SorobanClientApi sorobanClientApi,
      RpcClientEncrypted rpcClient,
      PaymentCode paymentCodeCoordinator) {
    this.sorobanClientApi = sorobanClientApi;
    this.rpcClient = rpcClient;
    this.paymentCodeCoordinator = paymentCodeCoordinator;
  }

  private InviteMixSorobanMessage waitInviteMix(long timeoutMs) throws Exception {
    PaymentCode paymentCodeMine = rpcClient.getPaymentCode();
    String directory =
        WhirlpoolProtocol.getSorobanDirSharedNotify(paymentCodeCoordinator, paymentCodeMine);
    String payload =
        asyncUtil.blockingGet(
            rpcClient.receiveEncrypted(directory, timeoutMs, paymentCodeCoordinator));
    return jsonUtil.getObjectMapper().readValue(payload, InviteMixSorobanMessage.class);
  }

  public InviteMixSorobanMessage registerInputAndWaitInviteMix(RegisterInputRequest request)
      throws Exception {
    while (true) {
      // REGISTER_INPUT
      asyncUtil.blockingAwait(
          sorobanClientApi.registerInput(rpcClient, paymentCodeCoordinator, request));

      // watch for mix invite
      try {
        return waitInviteMix(REGISTER_INPUT_DELAY_MS);
      } catch (TimeoutException e) {
        // no mix invite received yet
      } catch (Exception e) {
        log.error("registerInputAndWaitForMix failed", e);
      }
    }
  }

  public void exit() {
    rpcClient.exit();
  }
}
