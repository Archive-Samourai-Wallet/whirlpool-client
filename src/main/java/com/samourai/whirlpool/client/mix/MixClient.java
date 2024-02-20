package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.ProtocolException;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.whirlpool.RpcSessionClient;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.WhirlpoolErrorCode;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import com.samourai.whirlpool.protocol.soroban.payload.beans.BlameReason;
import com.samourai.whirlpool.protocol.soroban.payload.beans.MixStatus;
import com.samourai.whirlpool.protocol.soroban.payload.mix.*;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputRequest;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputResponse;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClient {
  // server health-check response
  private static final String HEALTH_CHECK_SUCCESS = "HEALTH_CHECK_SUCCESS";
  private static final Logger log = LoggerFactory.getLogger(MixClient.class);
  private static final int TIMEOUT_SWITCH_INPUT_MS = 900000; // 15min

  // mix settings
  private MixParams mixParams;
  private WhirlpoolClientListener listener;
  private WhirlpoolApiClient whirlpoolApiClient;

  private MixProcess mixProcess;
  private String mixId; // set by whirlpool()
  private Coordinator coordinator; // set by doRegisterInput

  public MixClient(
      WhirlpoolClientConfig config, MixParams mixParams, WhirlpoolClientListener listener) {
    this.mixParams = mixParams;
    this.listener = listener;
    this.whirlpoolApiClient = config.createWhirlpoolApiClient(mixParams.getCoordinatorSupplier());

    this.mixProcess =
        new MixProcess(
            config.getWhirlpoolNetwork().getParams(),
            mixParams.getPoolId(),
            mixParams.getDenomination(),
            mixParams.getMustMixBalanceMin(),
            mixParams.getMustMixBalanceMax(),
            mixParams.getPremixHandler(),
            mixParams.getPostmixHandler(),
            config.getClientCryptoService(),
            mixParams.getChainSupplier());
  }

  public void whirlpool() {
    try {
      // REGISTER_INPUT
      RegisterInputResponse registerInputResponse;
      try {
        registerInputResponse = doRegisterInput();
      } catch (SorobanErrorMessageException e) {
        log.error("Register input failed: " + e.getMessage());
        if (log.isDebugEnabled()) {
          log.debug("", e);
        }
        exitOnInputRejected(e.getMessage());
        return;
      } catch (TimeoutException e) {
        // cancel mixing this input and try later with another one
        if (log.isDebugEnabled()) {
          log.debug("Input was not selected for a mix, cancelling.");
        }
        stop(true);
        return;
      }

      mixId = registerInputResponse.mixId;

      // CONFIRM_INPUT
      listenerProgress(MixStep.CONFIRMING_INPUT);
      ConfirmInputRequest confirmInputRequest = mixProcess.confirmInput(registerInputResponse);
      ConfirmInputResponse confirmInputResponse =
          whirlpoolApiClient.confirmInput(confirmInputRequest, mixId, coordinator.getSender());
      listenerProgress(MixStep.CONFIRMED_INPUT);
      mixProcess.onConfirmInputResponse(confirmInputResponse);

      // complete the mix
      doMix();
    } catch (TimeoutException e) {
      if (log.isDebugEnabled()) {
        log.debug("MIX_TIMEOUT " + mixId);
        stop(true); // cancel mixing this input
      }
    } catch (InterruptedException e) { // when a loop is interrupted by rpcSession.done
      if (log.isDebugEnabled()) {
        log.debug("MIX_CANCEL " + mixId);
        stop(true); // cancel mixing this input
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error("MIX_ERROR " + mixId, e);
      }
      Exception notifiableException = NotifiableException.computeNotifiableException(e);
      failAndExit(MixFailReason.INTERNAL_ERROR, notifiableException.getMessage());
    }
  }

  protected RegisterInputResponse doRegisterInput() throws Exception {
    // select coordinator
    coordinator = mixParams.getCoordinatorSupplier().findCoordinatorByPoolId(mixParams.getPoolId());

    // update rpcSession's coordinator to use it's nodes (to avoid offline/laggy soroban nodes)
    ((RpcSessionClient) whirlpoolApiClient.getRpcSession()).setCoordinator(coordinator);

    // REGISTER_INPUT & wait for mix invite
    RegisterInputRequest registerInputRequest = mixProcess.registerInput();
    RegisterInputResponse registerInputResponse =
        whirlpoolApiClient.registerInput( // throws TimeoutException
            registerInputRequest,
            mixParams.getPoolId(),
            mixProcess.isLiquidity(),
            coordinator.getSender(),
            TIMEOUT_SWITCH_INPUT_MS);
    return registerInputResponse;
  }

  protected void doMix() throws Exception {
    long resendFrequency =
        whirlpoolApiClient
            .getSorobanApp()
            .getEndpointMix_ALL(mixId, coordinator.getSender())
            .getResendFrequencyWhenNoReplyMs();
    MixStatus mixStatus = MixStatus.CONFIRM_INPUT;
    while (!isDone()) {
      // fetch mix status
      AbstractMixStatusResponse mixStatusResponse =
          whirlpoolApiClient.mixStatus(mixId, coordinator.getSender());

      if (!mixStatusResponse.getMixStatus().equals(mixStatus)) {
        // mix progress
        mixStatus = mixStatusResponse.getMixStatus();

        if (log.isDebugEnabled()) {
          log.debug("MIX_PROGRESS " + mixId + " " + mixStatus);
        }

        switch (mixStatus) {
          case REGISTER_OUTPUT:
            listenerProgress(MixStep.REGISTERING_OUTPUT);
            doRegisterOutput((MixStatusResponseRegisterOutput) mixStatusResponse);
            listenerProgress(MixStep.REGISTERED_OUTPUT);
            break;

          case SIGNING:
            listenerProgress(MixStep.SIGNING);
            SigningRequest signingRequest =
                mixProcess.signing((MixStatusResponseSigning) mixStatusResponse);
            whirlpoolApiClient.signing(signingRequest, mixId, coordinator.getSender());
            listenerProgress(MixStep.SIGNED);

            // mix completed on our side, wait for mix result
            try {
              AbstractMixStatusResponse mixResult = whirlpoolApiClient.mixResult(mixId, coordinator.getSender());
              switch (mixResult.getMixStatus()) {
                case SUCCESS:
                  onMixSuccess();
                  return;

                case FAIL:
                  // mix failed
                  onMixFail(((MixStatusResponseFail) mixResult).getBlame());
                  return;
              }
            } catch (TimeoutException e) {
              // consider timeout as mix failure
              if (log.isDebugEnabled()) {
                log.warn("mixResult timeout for unknown reason for mixId=" + mixId);
              }
              onMixFail(null);
            }
            return;

          case REVEAL_OUTPUT:
            doRevealOutput(mixId);
            break;

          case FAIL:
            // mix failed
            onMixFail(((MixStatusResponseFail) mixStatusResponse).getBlame());
            return;
        }
      } /* else {
          if (log.isDebugEnabled()) {
            log.debug("MIX_LOOP " + mixId + " " + mixStatus);
          }
        }*/
      synchronized (this) {
        wait(resendFrequency);
      }
    }
  }

  protected void doRegisterOutput(MixStatusResponseRegisterOutput mixNotificationRegisterOutput)
      throws Exception {
    // this will increment unconfirmed postmix index
    RegisterOutputRequest registerOutputRequest =
        mixProcess.registerOutput(mixNotificationRegisterOutput);

    // use new identity to unlink from input
    WhirlpoolApiClient apiTemp = whirlpoolApiClient.createNewIdentity();

    while (!isDone()) {
      try {
        apiTemp.registerOutput(registerOutputRequest, mixId, coordinator.getSender());

        // confirm postmix index on REGISTER_OUTPUT success
        mixParams.getPostmixHandler().onRegisterOutput();
        return;
      } catch (SorobanErrorMessageException e) {
        if (e.getSorobanErrorMessage().errorCode == WhirlpoolErrorCode.INPUT_ALREADY_REGISTERED) {
          // loop and retry with next output...
          registerOutputRequest = mixProcess.registerOutput(mixNotificationRegisterOutput);
          if (log.isDebugEnabled()) {
            log.warn(
                "Output address already used, trying next one: "
                    + registerOutputRequest.receiveAddress);
          }
        } else {
          throw e; // unknown error
        }
      }
    }
  }

  protected void doRevealOutput(String mixId) throws Exception {
    if (mixId != null) {
      try {
        RevealOutputRequest revealOutputRequest = mixProcess.revealOutput();
        whirlpoolApiClient.revealOutput(revealOutputRequest, mixId, coordinator.getSender());
        listenerProgress(MixStep.FAIL);
      } catch (ProtocolException e) {
        // this can happen when we couldnt REGISTER_OUTPUT
        if (log.isDebugEnabled()) {
          log.error("REVEAL_OUTPUT failed", e);
        }
      }
    } else {
      if (log.isDebugEnabled()) {
        log.error("Failed to revealOutput, no mixId received yet");
      }
    }
    onMixFail(BlameReason.REGISTER_OUTPUT);
  }

  private void onMixSuccess() {
    // disconnect before notifying listener to avoid reconnecting before disconnect
    disconnect();
    // notify
    listenerProgress(MixStep.SUCCESS);
    listener.success(mixId, mixProcess.getReceiveUtxo(), mixProcess.getReceiveDestination());
  }

  protected void onMixFail(BlameReason blameReason) {
    String notifiableError =
        blameReason != null
            ? "Mix failed due to your behavior: " + blameReason.name()
            : "Mix failed due to a peer (not your fault)";
    failAndExit(MixFailReason.MIX_FAILED, notifiableError);
  }

  private void listenerProgress(MixStep mixStep) {
    this.listener.progress(mixId, mixStep);
  }

  public void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("MIX_DISCONECT from " + mixId);
    }
    mixParams.getRpcSession().exit();
  }

  private void failAndExit(MixFailReason reason, String notifiableError) {
    mixParams.getPostmixHandler().onMixFail();
    this.listener.fail(mixId, reason, notifiableError);
    disconnect();
  }

  public void stop(boolean cancel) {
    MixFailReason failReason = cancel ? MixFailReason.CANCEL : MixFailReason.STOP;
    failAndExit(failReason, null);
  }

  public void exitOnInputRejected(String notifiableError) {
    if (!HEALTH_CHECK_SUCCESS.equals(notifiableError)) {
      log.error("ERROR: input rejected: " + notifiableError);
    }
    failAndExit(MixFailReason.INPUT_REJECTED, notifiableError);
  }

  private boolean isDone() {
    return mixParams.getRpcSession().isDone();
  }
}
