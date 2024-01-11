package com.samourai.whirlpool.client.whirlpool;

import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.*;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.beans.Utxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolClientImpl implements WhirlpoolClient {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolClientImpl.class);

  private WhirlpoolClientConfig config;

  private boolean done;

  private MixClient mixClient;
  private Thread mixThread;
  private WhirlpoolClientListener listener;

  public WhirlpoolClientImpl(WhirlpoolClientConfig config) {
    this.config = config;
    if (log.isDebugEnabled()) {
      log.debug("+whirlpoolClient");
    }
  }

  @Override
  public void whirlpool(final MixParams mixParams, WhirlpoolClientListener listener) {
    this.listener = listener;

    this.mixThread =
        new Thread(
            new Runnable() {
              @Override
              public synchronized void run() {
                try {
                  runClient(mixParams);
                } catch (Exception e) {
                  return;
                }
                while (!done) {
                  try {
                    synchronized (mixThread) {
                      mixThread.wait();
                    }
                  } catch (Exception e) {
                  }
                }
              }
            },
            "whirlpoolClient-" + System.currentTimeMillis());
    this.mixThread.setDaemon(true);
    this.mixThread.start();
  }

  private void runClient(MixParams mixParams) throws Exception {
    WhirlpoolClientListener mixListener = computeMixListener();

    mixClient = new MixClient(config, mixParams, mixListener);
    mixClient.whirlpool();
  }

  private WhirlpoolClientListener computeMixListener() {
    return new WhirlpoolClientListener() {

      @Override
      public void success(Utxo receiveUtxo, MixDestination receiveDestination) {
        // done
        listener.success(receiveUtxo, receiveDestination);
        disconnect();
      }

      @Override
      public void fail(MixFailReason reason, String notifiableError) {
        listener.fail(reason, notifiableError);
        disconnect();
      }

      @Override
      public void progress(MixStep mixStep) {
        listener.progress(mixStep);
      }
    };
  }

  @Override
  public void stop(boolean cancel) {
    if (mixClient != null) {
      mixClient.stop(cancel);
    }
  }

  private void disconnect() {
    if (!done) {
      if (log.isDebugEnabled()) {
        log.debug("--whirlpoolClient");
      }
      done = true;
      if (mixClient != null) {
        mixClient.disconnect();
      }
      if (mixThread != null) {
        synchronized (mixThread) {
          mixThread.notify();
        }
      }
    }
  }

  public void _setListener(WhirlpoolClientListener listener) {
    this.listener = listener;
  }

  public WhirlpoolClientListener getListener() {
    return listener;
  }
}
