package com.samourai.whirlpool.client.mix;

import com.samourai.whirlpool.client.mix.dialog.MixDialogListener;
import com.samourai.whirlpool.client.mix.dialog.MixSession;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.listener.MixClientListener;
import com.samourai.whirlpool.client.mix.listener.MixClientListenerHandler;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.RegisterOutputRequest;
import com.samourai.whirlpool.protocol.websocket.messages.*;
import com.samourai.whirlpool.protocol.websocket.notifications.ConfirmInputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RegisterOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RevealOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.SigningMixStatusNotification;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClient {
  // non-static logger to prefix it with stomp sessionId
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // server settings
  private WhirlpoolClientConfig config;

  // mix settings
  private MixParams mixParams;
  private MixClientListenerHandler listener;

  private ClientCryptoService clientCryptoService;
  private WhirlpoolProtocol whirlpoolProtocol;
  private String logPrefix;
  private MixSession mixSession;
  private boolean done;

  public MixClient(WhirlpoolClientConfig config) {
    this(config, new ClientCryptoService(), new WhirlpoolProtocol());
  }

  public MixClient(
      WhirlpoolClientConfig config,
      ClientCryptoService clientCryptoService,
      WhirlpoolProtocol whirlpoolProtocol) {
    this.config = config;
    this.clientCryptoService = clientCryptoService;
    this.whirlpoolProtocol = whirlpoolProtocol;
  }

  public void whirlpool(MixParams mixParams, MixClientListener listener) {
    this.mixParams = mixParams;
    this.listener = new MixClientListenerHandler(listener);
    connect();
  }

  private void listenerProgress(MixStep mixClientStatus) {
    this.listener.progress(mixClientStatus);
  }

  private void connect() {
    if (this.mixSession != null) {
      log.warn("connect() : already connected");
      return;
    }

    listenerProgress(MixStep.CONNECTING);
    mixSession =
        new MixSession(
            computeMixDialogListener(), whirlpoolProtocol, config, mixParams.getPoolId());
    if (logPrefix != null) {
      mixSession.setLogPrefix(logPrefix);
    }
    mixSession.connect();
  }

  private void disconnect() {
    if (mixSession != null) {
      mixSession.disconnect();
      mixSession = null;
    }
  }

  private void failAndExit() {
    this.listener.progress(MixStep.FAIL);
    this.listener.fail();
    exit();
  }

  public void exit() {
    disconnect();
    done = true;
  }

  public boolean isDone() {
    return done;
  }

  public void setLogPrefix(String logPrefix) {
    this.logPrefix = logPrefix;
    if (mixSession != null) {
      mixSession.setLogPrefix(logPrefix);
    }
    log = ClientUtils.prefixLogger(log, logPrefix);
  }

  private MixProcess computeMixProcess() {
    return new MixProcess(
        config,
        mixParams.getPoolId(),
        mixParams.getDenomination(),
        mixParams.getPremixHandler(),
        mixParams.getPostmixHandler(),
        clientCryptoService);
  }

  private MixDialogListener computeMixDialogListener() {
    return new MixDialogListener() {
      MixProcess mixProcess = computeMixProcess();

      @Override
      public void onConnected() {
        listenerProgress(MixStep.CONNECTED);
      }

      @Override
      public void onFail() {
        failAndExit();
      }

      @Override
      public void exitOnProtocolError() {
        log.error("ERROR: protocol error, this may be a bug");
        failAndExit();
      }

      @Override
      public void exitOnResponseError(String notifiableError) {
        log.error("ERROR: " + notifiableError);
        failAndExit();
      }

      @Override
      public void exitOnDisconnected() {
        // failed to connect or connexion lost
        log.error("Disconnected");
        failAndExit();
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
      public void postRegisterOutput(
          RegisterOutputMixStatusNotification registerOutputMixStatusNotification,
          String registerOutputUrl)
          throws Exception {
        listenerProgress(MixStep.REGISTERING_OUTPUT);
        RegisterOutputRequest registerOutputRequest =
            mixProcess.registerOutput(registerOutputMixStatusNotification);

        // POST request through a different identity for mix privacy
        if (log.isDebugEnabled()) {
          log.debug(
              "POST " + registerOutputUrl + ": " + ClientUtils.toJsonString(registerOutputRequest));
        }
        config.getHttpClient().postJsonOverTor(registerOutputUrl, registerOutputRequest);
        listenerProgress(MixStep.REGISTERED_OUTPUT);
      }

      @Override
      public void onSuccess() {
        exit(); // disconnect before notifying listener to avoid reconnecting before disconnect
        listener.progress(MixStep.SUCCESS);
        listener.success(mixProcess.computeMixSuccess(), computeNextMixParams());
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

      protected MixParams computeNextMixParams() {
        IPremixHandler nextPremixHandler = mixProcess.computeNextPremixHandler();
        return new MixParams(mixParams, nextPremixHandler);
      }
    };
  }
}
