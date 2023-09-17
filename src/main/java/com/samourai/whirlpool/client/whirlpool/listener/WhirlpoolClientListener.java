package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;

public interface WhirlpoolClientListener {
  void success(UtxoDetail receiveUtxo);

  void fail(MixFailReason reason, String notifiableError);

  void progress(MixStep mixStep);
}
