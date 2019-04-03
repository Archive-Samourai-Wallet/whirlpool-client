package com.samourai.whirlpool.client.utils;

import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;

public class MultiClientListener extends LoggingWhirlpoolClientListener {
  // indice 0 is always null as currentMix starts from 1
  private MixStatus mixStatus;
  private MixStep mixStep;
  private MultiClientManager multiClientManager;

  public MultiClientListener(MultiClientManager multiClientManager) {
    this.multiClientManager = multiClientManager;
  }

  @Override
  public void success(MixSuccess mixSuccess) {
    super.success(mixSuccess);
    mixStatus = MixStatus.SUCCESS;
    notifyMultiClientManager();
  }

  @Override
  public void progress(MixStep step, String stepInfo, int stepNumber, int nbSteps) {
    super.progress(step, stepInfo, stepNumber, nbSteps);
    mixStep = step;
  }

  @Override
  public void fail() {
    super.fail();
    mixStatus = MixStatus.FAIL;
    notifyMultiClientManager();
  }

  private void notifyMultiClientManager() {
    synchronized (multiClientManager) {
      multiClientManager.notify();
    }
  }

  public MixStatus getMixStatus() {
    return mixStatus;
  }

  public MixStep getMixStep() {
    return mixStep;
  }
}
