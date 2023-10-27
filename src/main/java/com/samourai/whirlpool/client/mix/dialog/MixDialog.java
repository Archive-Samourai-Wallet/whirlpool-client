package com.samourai.whirlpool.client.mix.dialog;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.WhirlpoolEndpoint;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.MixMessage;
import com.samourai.whirlpool.protocol.websocket.messages.*;
import com.samourai.whirlpool.protocol.websocket.notifications.*;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixDialog {
  private static final Logger log = LoggerFactory.getLogger(MixDialog.class);
  private static final int REGISTER_OUTPUT_ATTEMPTS = 10;
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private MixDialogListener listener;
  private MixSession mixSession;
  private InviteMixSorobanMessage inviteMixSorobanMessage;
  private String mixId;

  // mix data
  private MixStatus mixStatus;
  private boolean gotConfirmInputResponse; // will get it after CONFIRM_INPUT

  // computed values
  private Set<MixStatus> mixStatusCompleted = new HashSet<MixStatus>();
  private boolean done;

  public MixDialog(
      MixDialogListener listener,
      MixSession mixSession,
      InviteMixSorobanMessage inviteMixSorobanMessage) {
    this.listener = listener;
    this.mixSession = mixSession;
    this.inviteMixSorobanMessage = inviteMixSorobanMessage;
    this.mixId = inviteMixSorobanMessage.mixId;
  }

  public synchronized void onPrivateReceived(MixMessage mixMessage) {
    if (done) {
      log.info("Ignoring mixMessage (done): " + ClientUtils.toJsonString(mixMessage));
      return;
    }
    if (log.isTraceEnabled()) {
      String mixMessageStr = "";
      try {
        mixMessageStr = JSONUtils.getInstance().getObjectMapper().writeValueAsString(mixMessage);
      } catch (Exception e) {
        log.error("", e);
      }
      log.trace("onPrivateReceived: " + mixMessageStr);
    }
    try {
      Class payloadClass = mixMessage.getClass();
      if (ErrorResponse.class.isAssignableFrom(payloadClass)) {
        String errorMessage = ((ErrorResponse) mixMessage).message;
        exitOnResponseError(errorMessage);
      } else {
        if (!mixMessage.mixId.equals(mixId)) {
          log.error("Invalid mixId: expected=" + mixId + ", actual=" + mixMessage.mixId);
          throw new Exception("Invalid mixId");
        }

        if (MixStatusNotification.class.isAssignableFrom(mixMessage.getClass())) {
          onMixStatusNotificationChange((MixStatusNotification) mixMessage);
        } else if (ConfirmInputResponse.class.isAssignableFrom(payloadClass)) {
          this.gotConfirmInputResponse = true;
          listener.onConfirmInputResponse((ConfirmInputResponse) mixMessage);
        } else {
          log.error(
              "Unexpected mixMessage, registeredInput=true: "
                  + ClientUtils.toJsonString(mixMessage));
        }
      }
    } catch (NotifiableException e) {
      log.error("onPrivateReceived NotifiableException: " + e.getMessage());
      exitOnResponseError(e.getMessage());
    } catch (Exception e) {
      log.error("onPrivateReceived Exception", e);
      exitOnPrivateReceivedException(e);
    }
  }

  private void exitOnPrivateReceivedException(Throwable e) {
    log.error("Protocol error", e);
    String message = ClientUtils.getHttpResponseBody(e);
    if (message == null) {
      message = e.getClass().getName();
    }
    String notifiableError = "onPrivate: " + message;
    listener.exitOnProtocolError(notifiableError);
    done = true;
  }

  private void exitOnResponseError(String error) {
    listener.exitOnInputRejected(error);
    done = true;
  }

  private void exitOnDisconnected(String error) {
    listener.exitOnDisconnected(error);
    done = true;
  }

  private void onMixStatusNotificationChange(MixStatusNotification notification) throws Exception {

    // check status consistency
    if (mixStatusCompleted.contains(notification.status)) {
      throw new Exception("mixStatus already completed: " + notification.status);
    }
    if (mixStatus != null && notification.status.equals(mixStatus)) {
      throw new Exception("Duplicate mixStatusNotification: " + mixStatus);
    }
    this.mixStatus = notification.status;

    if (MixStatus.FAIL.equals(notification.status)) {
      done = true;
      listener.onMixFail();
      return;
    }

    if (!mixStatusCompleted.contains(MixStatus.CONFIRM_INPUT) || !gotConfirmInputResponse) {
      // not confirmed in time, missed the mix
      log.info(" x Missed the mix");
      done = true;
      listener.onMixFail();
      return;
    }

    if (MixStatus.REGISTER_OUTPUT.equals(notification.status)) {
      doRegisterOutput((RegisterOutputMixStatusNotification) notification); // async
      mixStatusCompleted.add(MixStatus.REGISTER_OUTPUT);
    } else {
      if (MixStatus.REVEAL_OUTPUT.equals(notification.status)) {
        if (mixStatusCompleted.contains(MixStatus.SIGNING)) {
          // don't reveal output if already signed
          throw new Exception("not revealing output as we already signed");
        }
        RevealOutputRequest revealOutputRequest =
            listener.revealOutput((RevealOutputMixStatusNotification) notification);
        mixSession.send(WhirlpoolEndpoint.WS_REVEAL_OUTPUT, revealOutputRequest);
        mixStatusCompleted.add(MixStatus.REVEAL_OUTPUT);

      } else {

        if (mixStatusCompleted.contains(MixStatus.REVEAL_OUTPUT)) {
          // we shouldn't have REVEAL_OUTPUT
          throw new Exception("not continuing as we revealed output");
        }
        if (!mixStatusCompleted.contains(MixStatus.REGISTER_OUTPUT)) {
          // we should already have REGISTER_OUTPUT
          throw new Exception("not continuing as we didn't REGISTER_OUTPUT");
        }

        if (MixStatus.SIGNING.equals(notification.status)) {
          SigningRequest signingRequest =
              listener.signing((SigningMixStatusNotification) notification);
          mixSession.send(WhirlpoolEndpoint.WS_SIGNING, signingRequest);
          mixStatusCompleted.add(MixStatus.SIGNING);
        } else {
          if (!mixStatusCompleted.contains(MixStatus.SIGNING)) {
            // we should already have SIGNED
            throw new Exception("not continuing as we didn't SIGN");
          }
          if (MixStatus.SUCCESS.equals(notification.status)) {
            listener.onMixSuccess();
            done = true;
            return;
          } else {
            throw new Exception("Unexpected MixStatus: " + notification.status);
          }
        }
      }
    }
  }

  public void confirmInput() throws Exception {
    ConfirmInputRequest confirmInputRequest = listener.confirmInput(inviteMixSorobanMessage);
    mixSession.send(WhirlpoolEndpoint.WS_CONFIRM_INPUT, confirmInputRequest);
    mixStatusCompleted.add(MixStatus.CONFIRM_INPUT);
  }

  private void doRegisterOutput(
      RegisterOutputMixStatusNotification registerOutputMixStatusNotification) throws Exception {

    asyncUtil
        .runIOAsyncCompletable(() -> doRegisterOutputAttempts(registerOutputMixStatusNotification))
        .subscribe(
            new CompletableObserver() {
              @Override
              public void onSubscribe(Disposable disposable) {}

              @Override
              public void onComplete() {
                if (log.isDebugEnabled()) {
                  log.debug("postRegisterOutput onComplete!");
                }
              }

              @Override
              public void onError(Throwable throwable) {
                // registerOutput failed
                try {
                  throw ClientUtils.wrapRestError(throwable);
                } catch (NotifiableException e) {
                  log.error("onPrivateReceived NotifiableException: " + e.getMessage());
                  exitOnResponseError(e.getMessage());
                } catch (HttpException e) {
                  log.error("onPrivateReceived HttpException: " + e.getMessage());
                  exitOnDisconnected(e.getMessage());
                } catch (Throwable e) {
                  log.error("onPrivateReceived Exception", e);
                  exitOnPrivateReceivedException(e);
                }
              }
            });
  }

  private void doRegisterOutputAttempts(
      RegisterOutputMixStatusNotification registerOutputMixStatusNotification) throws Exception {
    int attempt = 0;
    while (true) {
      try {
        if (log.isDebugEnabled()) {
          log.debug("registerOutput[" + attempt + "]");
        }
        asyncUtil.blockingAwait(
            listener.postRegisterOutput(
                registerOutputMixStatusNotification, mixSession.getServerApi()));
        return; // success
      } catch (Exception e) {
        if (attempt >= REGISTER_OUTPUT_ATTEMPTS) {
          throw e; // all attempts failed
        }
        if (log.isDebugEnabled()) {
          log.error(
              "postRegisterOutput["
                  + attempt
                  + "/"
                  + REGISTER_OUTPUT_ATTEMPTS
                  + "] failed, retrying... "
                  + e.getMessage());
        }
        attempt++; // continue next attempt
      }
    }
  }

  protected boolean gotConfirmInputResponse() {
    return gotConfirmInputResponse;
  }

  public String getMixId() {
    return mixId;
  }
}
