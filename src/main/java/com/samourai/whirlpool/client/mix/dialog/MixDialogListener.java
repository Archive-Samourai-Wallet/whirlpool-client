package com.samourai.whirlpool.client.mix.dialog;

import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.*;
import com.samourai.whirlpool.protocol.websocket.notifications.RegisterOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RevealOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.SigningMixStatusNotification;
import io.reactivex.Completable;

public interface MixDialogListener {

  void onConnected();

  void onConnectionFailWillRetry(int retryDelay);

  RegisterInputRequest registerInput() throws Exception;

  ConfirmInputRequest confirmInput(InviteMixSorobanMessage inviteMixSorobanMessage)
      throws Exception;

  Completable postRegisterOutput(
      RegisterOutputMixStatusNotification registerOutputMixStatusNotification, ServerApi serverApi)
      throws Exception;

  RevealOutputRequest revealOutput(
      RevealOutputMixStatusNotification revealOutputMixStatusNotification) throws Exception;

  SigningRequest signing(SigningMixStatusNotification signingMixStatusNotification)
      throws Exception;

  void onConfirmInputResponse(ConfirmInputResponse confirmInputResponse) throws Exception;

  void onMixSuccess();

  void onMixFail();

  void exitOnProtocolError(String notifiableError);

  void exitOnProtocolVersionMismatch(String serverProtocolVersion);

  void exitOnInputRejected(String notifiableError);

  void exitOnDisconnected(String error);
}
