package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.protocol.beans.Utxo;

public interface WhirlpoolClientListener {
  void success(String mixId, Utxo receiveUtxo, MixDestination receiveDestination);

  void fail(String mixId, MixFailReason reason, String notifiableError);

  void progress(String mixId, MixStep mixStep);
}
