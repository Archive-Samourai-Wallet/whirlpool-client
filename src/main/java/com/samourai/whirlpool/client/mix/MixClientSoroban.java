package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.dialog.SorobanErrorMessageException;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.RejectedInputException;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.WhirlpoolProtocolSoroban;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClientSoroban {
  private static final Logger log = LoggerFactory.getLogger(MixClientSoroban.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private WhirlpoolClientConfig config;
  private MixParams mixParams;
  private SorobanClientApi sorobanClientApi;
  private boolean done;

  public MixClientSoroban(
      WhirlpoolClientConfig config, MixParams mixParams, SorobanClientApi sorobanClientApi) {
    this.config = config;
    this.mixParams = mixParams;
    this.sorobanClientApi = sorobanClientApi;
    this.done = false;
  }

  public InviteMixSorobanMessage registerInputAndWaitInviteMix(RegisterInputRequest request)
      throws Exception {
    RpcSession rpcSession = mixParams.getRpcSession();
    while (true) {
      if (done) {
        throw new NotifiableException("Terminating");
      }

      // find coordinator
      Coordinator coordinator =
          mixParams.getCoordinatorSupplier().findCoordinatorByPoolId(request.poolId);
      if (coordinator == null) {
        throw new NotifiableException(
            "No coordinator available for pool " + request.poolId + ", please retry later");
      }
      PaymentCode paymentCodeCoordinator = coordinator.getPaymentCode();

      // REGISTER_INPUT
      asyncUtil.blockingAwait(
          rpcSession.withSorobanClient(
              sorobanClient -> {
                if (log.isDebugEnabled()) {
                  log.debug("=> withRpcClientEncrypted.call registerInput ");
                }
                return sorobanClientApi.registerInput(
                    sorobanClient, request, paymentCodeCoordinator);
              }));

      if (log.isDebugEnabled()) {
        log.debug("=> registerInput success, waitInviteMix...");
      }

      // wait for mix invite or input rejection
      try {
        asyncUtil.blockingAwait(
            rpcSession.withSorobanClient(
                sorobanClient -> {
                  return asyncUtil.blockingGet(
                      sorobanClientApi.waitInviteMix(
                          sorobanClient,
                          rpcSession.getRpcWallet(),
                          config.getBip47Util(),
                          paymentCodeCoordinator,
                          getWhirlpoolProtocolSoroban().getRegisterInputFrequencyMs()));
                }));
      } catch (SorobanErrorMessageException e) {
        // otherwise it's an error response => input rejected
        throw new RejectedInputException(e.getSorobanErrorMessage().message);
      } catch (TimeoutException e) {
        // no mix invite received yet
        return null;
      }
    }
  }

  public void exit() {
    done = true;
  }

  public WhirlpoolProtocolSoroban getWhirlpoolProtocolSoroban() {
    return sorobanClientApi.getWhirlpoolProtocolSoroban();
  }
}
