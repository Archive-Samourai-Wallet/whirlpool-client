package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.listener.*;
import com.samourai.whirlpool.protocol.beans.Utxo;

public interface WhirlpoolClientListener {
  void success(Utxo receiveUtxo);

  void fail(MixFailReason reason, String notifiableError);

  void progress(MixStep mixStep);
}
