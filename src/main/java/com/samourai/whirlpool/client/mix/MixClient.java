package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.ShutdownException;
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
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClientMix;
import com.samourai.whirlpool.protocol.soroban.payload.beans.BlameReason;
import com.samourai.whirlpool.protocol.soroban.payload.beans.MixStatus;
import com.samourai.whirlpool.protocol.soroban.payload.mix.*;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputRequest;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputResponse;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClient {
  // server health-check response
  private static final String HEALTH_CHECK_SUCCESS = "HEALTH_CHECK_SUCCESS";
  private static final Logger log = LoggerFactory.getLogger(MixClient.class);
  private static final int TIMEOUT_SWITCH_INPUT_MS = 1200000; // 20min

  // mix settings
  private WhirlpoolClientConfig config;
  private MixParams mixParams;
  private WhirlpoolClientListener listener;
  private MixProcess mixProcess;
  private String mixId; // set by whirlpool()
  private Coordinator coordinator; // set by doRegisterInput
  private boolean done;

  public MixClient(
      WhirlpoolClientConfig config, MixParams mixParams, WhirlpoolClientListener listener) {
    this.config = config;
    this.mixParams = mixParams;
    this.listener = listener;

    this.done = false;
  }

  public void whirlpool() {
    while (!done) {
      this.mixProcess =
          new MixProcess(
              config.getSamouraiNetwork().getParams(),
              mixParams.getPoolId(),
              mixParams.getDenomination(),
              mixParams.getMustMixBalanceMin(),
              mixParams.getMustMixBalanceMax(),
              mixParams.getPremixHandler(),
              mixParams.getPostmixHandler(),
              config.getClientCryptoService(),
              mixParams.getChainSupplier());
      mixAttempt();
    }
  }

  private void mixAttempt() {
    if (log.isDebugEnabled()) {
      log.debug("MIX_ATTEMPT " + mixParams.getWhirlpoolUtxo());
    }
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
          log.debug("Input was not selected for a mix, rotating.");
        }
        failAndExit(MixFailReason.ROTATE, null);
        return;
      }

      mixId = registerInputResponse.mixId;
      WhirlpoolApiClient clientApi = mixParams.getWhirlpoolApiClient();
      WhirlpoolApiClientMix mixApi =
          clientApi.createWhirlpoolApiClientMix(mixId, coordinator.getSender());

      // CONFIRM_INPUT
      ConfirmInputRequest confirmInputRequest = mixProcess.confirmInput(registerInputResponse);
      ConfirmInputResponse confirmInputResponse = mixApi.confirmInput(confirmInputRequest);
      listenerProgress(MixStep.CONFIRM_INPUT);
      mixProcess.onConfirmInputResponse(confirmInputResponse);

      // complete the mix
      doMix(mixApi);
    } catch (TimeoutException e) {
      // exit with done=false to restart with a new mix attempt
      if (log.isDebugEnabled()) {
        log.debug("MIX_TIMEOUT " + mixId + " " + mixParams.getWhirlpoolUtxo());
      }
    } catch (InterruptedException e) { // when a loop is interrupted by rpcSession.done
      // exit with done=false to restart with a new mix attempt
      if (log.isDebugEnabled()) {
        log.debug("MIX_INTERRUPTED " + mixId + " " + mixParams.getWhirlpoolUtxo());
      }
    } catch (ShutdownException e) {
      // silent exit on shutdown
      if (log.isDebugEnabled()) {
        log.debug("MIX_SHUTDOWN " + mixId + " " + mixParams.getWhirlpoolUtxo());
      }
    } catch (HttpException e) {
      if (log.isDebugEnabled()) {
        log.error("NETWORK_ERROR " + mixId + " " + mixParams.getWhirlpoolUtxo(), e);
      }
      Exception notifiableException = NotifiableException.computeNotifiableException(e);
      failAndExit(MixFailReason.NETWORK_ERROR, notifiableException.getMessage());
    } catch (Exception e) {
      if (e instanceof SorobanErrorMessageException) {
        // restart mix on "confirming input not found"
        if (((SorobanErrorMessageException) e).getSorobanErrorMessage().errorCode
            == WhirlpoolErrorCode.MIX_OVER) {
          if (log.isDebugEnabled()) {
            log.error(
                "MIX_RESTART " + mixId + " " + mixParams.getWhirlpoolUtxo() + " reason=MIX_OVER",
                e);
          }
          return; // exit with done=false to restart with a new mix attempt
        }
      }
      if (log.isDebugEnabled()) {
        log.error("MIX_ERROR " + mixId + " " + mixParams.getWhirlpoolUtxo(), e);
      }
      Exception notifiableException = NotifiableException.computeNotifiableException(e);
      failAndExit(MixFailReason.INTERNAL_ERROR, notifiableException.getMessage());
    }
  }

  protected RegisterInputResponse doRegisterInput() throws Exception {
    WhirlpoolApiClient whirlpoolApiClient = mixParams.getWhirlpoolApiClient();
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

  protected void doMix(WhirlpoolApiClientMix mixApi) throws Exception {
    WhirlpoolApiClient clientApi = mixParams.getWhirlpoolApiClient();
    long resendFrequency =
        clientApi
            .getSorobanApp()
            .getEndpointMixConfirmInput(mixId, coordinator.getSender())
            .getResendFrequencyWhenNoReplyMs();
    MixStatus mixStatus = MixStatus.CONFIRM_INPUT;
    while (!isDone()) {
      // check for mix result
      Optional<AbstractMixStatusResponse> mixResultOpt = mixApi.findMixResult();
      if (mixResultOpt.isPresent()) {
        // early mix failure
        onMixResult(mixResultOpt.get());
        return;
      }

      // fetch mix status
      AbstractMixStatusResponse mixStatusResponse = mixApi.waitMixStatus();

      if (!mixStatusResponse.mixStatus.equals(mixStatus)) {
        // mix progress
        mixStatus = mixStatusResponse.mixStatus;

        if (log.isDebugEnabled()) {
          log.debug("MIX_PROGRESS " + mixId + " " + mixStatus);
        }

        switch (mixStatus) {
          case REGISTER_OUTPUT:
            doRegisterOutput((MixStatusResponseRegisterOutput) mixStatusResponse);
            listenerProgress(MixStep.REGISTER_OUTPUT);
            break;

          case SIGNING:
            SigningRequest signingRequest =
                mixProcess.signing((MixStatusResponseSigning) mixStatusResponse);
            mixApi.signing(signingRequest);
            listenerProgress(MixStep.SIGN);

            // mix completed on our side, wait for mix result
            try {
              AbstractMixStatusResponse mixResult = mixApi.waitMixResult();
              onMixResult(mixResult);
              return;
            } catch (TimeoutException e) {
              // consider timeout as mix failure
              if (log.isDebugEnabled()) {
                log.warn("mixResult timeout for unknown reason for mixId=" + mixId);
              }
              onMixFail(null);
            }
            return;

          case REVEAL_OUTPUT:
            if (mixId != null) {
              try {
                RevealOutputRequest revealOutputRequest = mixProcess.revealOutput();
                mixApi.revealOutput(revealOutputRequest);
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
            return;

          case FAIL:
            // mix failed
            onMixFail(((MixStatusResponseFail) mixStatusResponse).blame);
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

  private void onMixResult(AbstractMixStatusResponse mixResult) {
    switch (mixResult.mixStatus) {
      case SUCCESS:
        onMixSuccess();
        break;

      case FAIL:
        // mix failed
        onMixFail(((MixStatusResponseFail) mixResult).blame);
        break;
    }
  }

  protected void doRegisterOutput(MixStatusResponseRegisterOutput mixNotificationRegisterOutput)
      throws Exception {
    // this will increment unconfirmed postmix index
    RegisterOutputRequest registerOutputRequest =
        mixProcess.registerOutput(mixNotificationRegisterOutput);

    // use new identity to unlink from input
    config.getHttpClientService().changeIdentity();
    WhirlpoolApiClient whirlpoolApiClient = mixParams.getWhirlpoolApiClient();
    WhirlpoolApiClient apiTemp = whirlpoolApiClient.createNewIdentity();

    while (!isDone()) {
      try {
        apiTemp.registerOutput(
            registerOutputRequest,
            mixNotificationRegisterOutput.inputsHash,
            coordinator.getSender());

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
            : "Mix failed due to another peer (not your fault)";
    failAndExit(MixFailReason.MIX_FAILED, notifiableError);
  }

  private void listenerProgress(MixStep mixStep) {
    this.listener.progress(mixId, mixStep);
  }

  private void disconnect() {
    if (!done) {
      done = true;
      if (log.isDebugEnabled()) {
        log.debug("MIX_DISCONECT " + mixParams.getWhirlpoolUtxo());
      }
      mixParams.getWhirlpoolApiClient().getRpcSession().exit();
    }
  }

  public void failAndExit(MixFailReason reason, String notifiableError) {
    mixParams.getPostmixHandler().onMixFail();
    this.listener.fail(mixId, reason, notifiableError);
    disconnect();
  }

  public void exitOnInputRejected(String notifiableError) {
    if (!HEALTH_CHECK_SUCCESS.equals(notifiableError)) {
      log.error("ERROR: input rejected: " + notifiableError);
    }
    failAndExit(MixFailReason.INPUT_REJECTED, notifiableError);
  }

  private boolean isDone() {
    return mixParams.getWhirlpoolApiClient().getRpcSession().isDone();
  }
}
