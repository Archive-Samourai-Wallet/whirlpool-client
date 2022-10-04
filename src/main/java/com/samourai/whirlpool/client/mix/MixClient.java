package com.samourai.whirlpool.client.mix;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.dialog.MixDialogListener;
import com.samourai.whirlpool.client.mix.dialog.MixSession;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.RegisterOutputRequest;
import com.samourai.whirlpool.protocol.websocket.messages.*;
import com.samourai.whirlpool.protocol.websocket.notifications.ConfirmInputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RegisterOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RevealOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.SigningMixStatusNotification;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClient {
  // server health-check response
  private static final String HEALTH_CHECK_SUCCESS = "HEALTH_CHECK_SUCCESS";

  // non-static logger to prefix it with stomp sessionId
  private Logger log;

  // server settings
  private WhirlpoolClientConfig config;

  // mix settings
  private MixParams mixParams;
  private WhirlpoolClientListener listener;

  private ClientCryptoService clientCryptoService;
  private WhirlpoolProtocol whirlpoolProtocol;
  private String logPrefix;
  private MixSession mixSession;

  public MixClient(WhirlpoolClientConfig config, String logPrefix) {
    this(config, logPrefix, new ClientCryptoService(), new WhirlpoolProtocol());
  }

  public MixClient(
      WhirlpoolClientConfig config,
      String logPrefix,
      ClientCryptoService clientCryptoService,
      WhirlpoolProtocol whirlpoolProtocol) {
    this.log = LoggerFactory.getLogger(MixClient.class + "[" + logPrefix + "]");
    this.config = config;
    this.logPrefix = logPrefix;
    this.clientCryptoService = clientCryptoService;
    this.whirlpoolProtocol = whirlpoolProtocol;
  }

  public void whirlpool(MixParams mixParams, WhirlpoolClientListener listener) {
    this.mixParams = mixParams;
    this.listener = listener;
    connect();
  }

  private void listenerProgress(MixStep mixStep) {
    this.listener.progress(mixStep);
  }

  private void connect() {
    if (this.mixSession != null) {
      log.warn("connect() : already connected");
      return;
    }

    try {
      listenerProgress(MixStep.CONNECTING);
      mixSession =
          new MixSession(
              computeMixDialogListener(),
              whirlpoolProtocol,
              config,
              mixParams.getPoolId(),
              logPrefix);
      mixSession.connect();
    } catch (Exception e) {
      // httpClientRegisterOutput failed
      String error = NotifiableException.computeNotifiableException(e).getMessage();
      failAndExit(MixFailReason.INTERNAL_ERROR, error);
    }
  }

  public void disconnect() {
    if (mixSession != null) {
      mixSession.disconnect();
      mixSession = null;
    }
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

  private MixProcess computeMixProcess() {
    return new MixProcess(
        config.getNetworkParameters(),
        mixParams.getPoolId(),
        mixParams.getDenomination(),
        mixParams.getPremixHandler(),
        mixParams.getPostmixHandler(),
        clientCryptoService,
        mixParams.getChainSupplier());
  }

  private MixDialogListener computeMixDialogListener() {
    return new MixDialogListener() {
      MixProcess mixProcess = computeMixProcess();

      @Override
      public void onConnected() {
        listenerProgress(MixStep.CONNECTED);
      }

      @Override
      public void onConnectionFailWillRetry(int retryDelay) {
        listenerProgress(MixStep.CONNECTING);
      }

      @Override
      public void onConnectionLostWillRetry() {
        listenerProgress(MixStep.CONNECTING);
      }

      @Override
      public void onMixFail() {
        failAndExit(MixFailReason.MIX_FAILED, null);
      }

      @Override
      public void exitOnProtocolError(String notifiableError) {
        log.error("ERROR: protocol error");
        failAndExit(MixFailReason.INTERNAL_ERROR, notifiableError);
      }

      @Override
      public void exitOnProtocolVersionMismatch(String serverProtocolVersion) {
        log.error(
            "ERROR: protocol version mismatch: server="
                + serverProtocolVersion
                + ", client="
                + WhirlpoolProtocol.PROTOCOL_VERSION);
        failAndExit(MixFailReason.PROTOCOL_MISMATCH, serverProtocolVersion);
      }

      @Override
      public void exitOnInputRejected(String notifiableError) {
        if (!HEALTH_CHECK_SUCCESS.equals(notifiableError)) {
          log.error("ERROR: input rejected: " + notifiableError);
        }
        failAndExit(MixFailReason.INPUT_REJECTED, notifiableError);
      }

      @Override
      public void exitOnDisconnected(String error) {
        // failed to connect or connexion lost
        log.error("ERROR: Disconnected: " + error);
        failAndExit(MixFailReason.DISCONNECTED, null);
      }

      @Override
      public RegisterInputRequest registerInput(SubscribePoolResponse subscribePoolResponse)
          throws Exception {
        RegisterInputRequest registerInputRequest = mixProcess.registerInput(subscribePoolResponse);
        listenerProgress(MixStep.REGISTERED_INPUT);
        return registerInputRequest;
      }

      @Override
      public ConfirmInputRequest confirmInput(
          ConfirmInputMixStatusNotification confirmInputMixStatusNotification) throws Exception {
        listenerProgress(MixStep.CONFIRMING_INPUT);
        return mixProcess.confirmInput(confirmInputMixStatusNotification);
      }

      @Override
      public void onConfirmInputResponse(ConfirmInputResponse confirmInputResponse)
          throws Exception {
        listenerProgress(MixStep.CONFIRMED_INPUT);
        mixProcess.onConfirmInputResponse(confirmInputResponse);

        if (log.isDebugEnabled()) {
          log.debug("joined mixId=" + confirmInputResponse.mixId);
        }
      }

      @Override
      public Completable postRegisterOutput(
          RegisterOutputMixStatusNotification registerOutputMixStatusNotification,
          ServerApi serverApi)
          throws Exception {
        listenerProgress(MixStep.REGISTERING_OUTPUT);

        // this will increment unconfirmed postmix index
        RegisterOutputRequest registerOutputRequest =
            mixProcess.registerOutput(registerOutputMixStatusNotification);

        // send request
        Single<Optional<String>> result = serverApi.registerOutput(registerOutputRequest);
        Single chainedResult =
            result.doAfterSuccess(
                single -> {
                  // confirm postmix index on REGISTER_OUTPUT success
                  mixParams.getPostmixHandler().onRegisterOutput();
                  listenerProgress(MixStep.REGISTERED_OUTPUT);
                });
        return Completable.fromSingle(chainedResult);
      }

      @Override
      public void onMixSuccess() {
        // disconnect before notifying listener to avoid reconnecting before disconnect
        disconnect();
        // notify
        listenerProgress(MixStep.SUCCESS);
        listener.success(mixProcess.getReceiveUtxo());
      }

      @Override
      public RevealOutputRequest revealOutput(
          RevealOutputMixStatusNotification revealOutputMixStatusNotification) throws Exception {
        RevealOutputRequest revealOutputRequest =
            mixProcess.revealOutput(revealOutputMixStatusNotification);
        listenerProgress(MixStep.REVEALED_OUTPUT);
        return revealOutputRequest;
      }

      @Override
      public SigningRequest signing(SigningMixStatusNotification signingMixStatusNotification)
          throws Exception {
        listenerProgress(MixStep.SIGNING);
        SigningRequest signingRequest = mixProcess.signing(signingMixStatusNotification);
        listenerProgress(MixStep.SIGNED);
        return signingRequest;
      }

      @Override
      public void onResetMix() {
        if (log.isDebugEnabled()) {
          log.debug("reset mixProcess");
        }
        mixProcess = computeMixProcess();
      }
    };
  }
}
