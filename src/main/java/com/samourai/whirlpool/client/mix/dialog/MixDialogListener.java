package com.samourai.whirlpool.client.mix.dialog;

import com.samourai.whirlpool.protocol.soroban.ConfirmInputResponse;

public interface MixDialogListener {

  void onConfirmInputResponse(ConfirmInputResponse confirmInputResponse) throws Exception;

  void exitOnProtocolError(String notifiableError);

  void exitOnProtocolVersionMismatch(String serverProtocolVersion);

  void exitOnInputRejected(String notifiableError);

  void exitOnDisconnected(String error);
}
