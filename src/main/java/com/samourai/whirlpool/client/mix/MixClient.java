package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.client.exception.UnexpectedTypePayloadSorobanException;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.exception.FailMixNotificationException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.RevealOutputNotificationException;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.WhirlpoolErrorCode;
import com.samourai.whirlpool.protocol.soroban.*;
import com.samourai.whirlpool.protocol.soroban.api.WhirlpoolPartnerApiClient;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClient {
  // server health-check response
  private static final String HEALTH_CHECK_SUCCESS = "HEALTH_CHECK_SUCCESS";
  private static final Logger log = LoggerFactory.getLogger(MixClient.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  private static final int NOTIFICATION_LOOP_FREQUENCY_MS = 5000;

  // server settings
  private WhirlpoolClientConfig config;

  // mix settings
  private MixParams mixParams;
  private WhirlpoolClientListener listener;

  private WhirlpoolPartnerApiClient
      whirlpoolPartnerApiClient; // set once RegisterInputResponse received

  private MixProcess mixProcess;
  private int requestNonce;
  private String mixId; // set by whirlpool()

  public MixClient(
      WhirlpoolClientConfig config, MixParams mixParams, WhirlpoolClientListener listener) {
    this.config = config;
    this.mixParams = mixParams;
    this.listener = listener;

    this.whirlpoolPartnerApiClient = null; // will be set by doRegisterInput()

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
    this.requestNonce = 0;
  }

  public void whirlpool() {
    try {
      // register input & wait for a mix
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
      }
      mixId = registerInputResponse.mixId;
      listenerProgress(MixStep.REGISTERED_INPUT);

      try {
        // complete the mix
        doMix(registerInputResponse);
      } catch (RevealOutputNotificationException e) {
        // reveal output on mix failure
        doRevealOutput(e.getRevealOutputNotification(), mixId);
        return;
      } catch (FailMixNotificationException e) {
        // mix failed
        onMixFail();
        return;
      }
    } catch (Exception e) {
      Exception notifiableException = NotifiableException.computeNotifiableException(e);
      exitOnProtocolError(notifiableException.getMessage());
    }
  }

  protected RegisterInputResponse doRegisterInput() throws Exception {
    RegisterInputRequest registerInputRequest;
    while (true) {
      try {
        // select coordinator
        whirlpoolPartnerApiClient =
            ClientUtils.getWhirlpoolPartnerApiClient(
                mixParams.getRpcSession(),
                mixParams.getCoordinatorSupplier(),
                mixParams.getPoolId(),
                config.getSorobanProtocolWhirlpool());

        // REGISTER_INPUT & wait for mix invite - throws SorobanErrorMessageException
        registerInputRequest = mixProcess.registerInput();
        return asyncUtil.blockingGet(whirlpoolPartnerApiClient.registerInput(registerInputRequest));
      } catch (TimeoutException e) {
        if (log.isDebugEnabled()) {
          log.debug("Input not registered yet, retrying...");
        }
        // continue looping
      }
    }
  }

  // throws FailMixNotification, RevealOutputNotification
  protected PushTxSuccessResponse doMix(RegisterInputResponse registerInputResponse)
      throws Exception {
    try {
      // CONFIRM_INPUT
      listenerProgress(MixStep.CONFIRMING_INPUT);
      ConfirmInputRequest confirmInputRequest = mixProcess.confirmInput(registerInputResponse);
      ConfirmInputResponse confirmInputResponse =
          asyncUtil.blockingGet(whirlpoolPartnerApiClient.confirmInput(confirmInputRequest, mixId));
      listenerProgress(MixStep.CONFIRMED_INPUT);
      mixProcess.onConfirmInputResponse(confirmInputResponse);

      // wait for REGISTER_OUTPUT
      RegisterOutputNotification registerOutputNotification =
          loopUntilReplyNotification(RegisterOutputNotification.class);
      listenerProgress(MixStep.REGISTERING_OUTPUT);

      // REGISTER_OUTPUT
      doRegisterOutput(registerOutputNotification);
      listenerProgress(MixStep.REGISTERED_OUTPUT);

      // wait for SIGN
      SigningNotification signingNotification =
          loopUntilReplyNotification(SigningNotification.class);
      listenerProgress(MixStep.SIGNING);

      // SIGN
      SigningRequest signingRequest = mixProcess.signing(signingNotification);
      asyncUtil.blockingAwait(whirlpoolPartnerApiClient.signing(signingRequest, mixId));
      listenerProgress(MixStep.SIGNED);

      // wait for SUCCESS
      PushTxSuccessResponse successMixNotification =
          loopUntilReplyNotification(PushTxSuccessResponse.class);
      onMixSuccess();
      return successMixNotification;
    } catch (UnexpectedTypePayloadSorobanException e) {
      e.getPayload()
          .readOn(
              FailMixNotification.class,
              o -> {
                throw new FailMixNotificationException(o);
              });
      e.getPayload()
          .readOn(
              RevealOutputNotification.class,
              o -> {
                throw new RevealOutputNotificationException(o);
              });
      throw e;
    }
  }

  protected <T extends SorobanPayload> T loopUntilReplyNotification(Class<T> type)
      throws Exception {
    String requestId = whirlpoolPartnerApiClient.getSorobanProtocol().getRequestId(requestNonce);
    T payload =
        asyncUtil.blockingGet(
            whirlpoolPartnerApiClient.loopUntilReplyTyped(
                requestId, NOTIFICATION_LOOP_FREQUENCY_MS, type));
    requestNonce++;
    return payload;
  }

  protected void doRegisterOutput(RegisterOutputNotification registerOutputNotification)
      throws Exception {
    // this will increment unconfirmed postmix index
    RegisterOutputRequest registerOutputRequest =
        mixProcess.registerOutput(registerOutputNotification);

    // use new identity to unlink from input
    WhirlpoolPartnerApiClient apiTemp = whirlpoolPartnerApiClient.createNewIdentity();

    while (true) {
      try {
        asyncUtil.blockingAwait(apiTemp.registerOutput(registerOutputRequest, mixId));

        // confirm postmix index on REGISTER_OUTPUT success
        mixParams.getPostmixHandler().onRegisterOutput();
        return;
      } catch (SorobanErrorMessageException e) {
        if (e.getSorobanErrorMessage().errorCode == WhirlpoolErrorCode.INPUT_ALREADY_REGISTERED) {
          // loop and retry with next output...
          registerOutputRequest = mixProcess.registerOutput(registerOutputNotification);
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

  protected void doRevealOutput(RevealOutputNotification revealOutputNotification, String mixId)
      throws Exception {
    if (mixId != null) {
      RevealOutputRequest revealOutputRequest = mixProcess.revealOutput();
      listenerProgress(MixStep.REVEALED_OUTPUT);
      asyncUtil.blockingAwait(whirlpoolPartnerApiClient.revealOutput(revealOutputRequest, mixId));
    } else {
      if (log.isDebugEnabled()) {
        log.warn("Cannot revealOutput, no mixId yet");
      }
    }
    onMixFail(); // TODO fail reason details?
  }

  private void onMixSuccess() {
    // disconnect before notifying listener to avoid reconnecting before disconnect
    disconnect();
    // notify
    listenerProgress(MixStep.SUCCESS);
    listener.success(mixProcess.getReceiveUtxo(), mixProcess.getReceiveDestination());
  }

  protected void onMixFail() {
    failAndExit(MixFailReason.MIX_FAILED, null);
  }

  private void listenerProgress(MixStep mixStep) {
    this.listener.progress(mixStep);
  }

  public void disconnect() {
    mixParams.getRpcSession().exit();
  }

  private void failAndExit(MixFailReason reason, String notifiableError) {
    mixParams.getPostmixHandler().onMixFail();
    this.listener.fail(reason, notifiableError);
    disconnect();
  }

  public void stop(boolean cancel) {
    MixFailReason failReason = cancel ? MixFailReason.CANCEL : MixFailReason.STOP;
    failAndExit(failReason, null);
  }

  public void exitOnProtocolError(String notifiableError) {
    log.error("ERROR: protocol error");
    failAndExit(MixFailReason.INTERNAL_ERROR, notifiableError);
  }

  public void exitOnInputRejected(String notifiableError) {
    if (!HEALTH_CHECK_SUCCESS.equals(notifiableError)) {
      log.error("ERROR: input rejected: " + notifiableError);
    }
    failAndExit(MixFailReason.INPUT_REJECTED, notifiableError);
  }
}
