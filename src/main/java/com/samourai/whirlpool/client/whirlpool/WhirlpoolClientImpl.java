package com.samourai.whirlpool.client.whirlpool;

import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolClientImpl implements WhirlpoolClient {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolClientImpl.class);

  private WhirlpoolClientConfig config;

  private MixClient mixClient;

  public WhirlpoolClientImpl(WhirlpoolClientConfig config) {
    this.config = config;
    if (log.isDebugEnabled()) {
      log.debug("+whirlpoolClient");
    }
  }

  @Override
  public void whirlpool(final MixParams mixParams, WhirlpoolClientListener listener) {
    Thread mixThread =
        new Thread(
            new Runnable() {
              @Override
              public synchronized void run() {
                try {
                  mixClient = new MixClient(config, mixParams, listener);
                  mixClient.whirlpool();
                } catch (Exception e) {
                  if (log.isDebugEnabled()) {
                    log.error("", e);
                  }
                }
              }
            },
            "whirlpoolClient-" + System.currentTimeMillis());
    mixThread.setDaemon(true);
    mixThread.start();
  }

  @Override
  public void stop(MixFailReason failReason) {
    if (mixClient != null) {
      mixClient.failAndExit(failReason, null);
    }
  }
}
