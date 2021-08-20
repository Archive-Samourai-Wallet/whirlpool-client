package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.*;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingWhirlpoolClientListener extends AbstractWhirlpoolClientListener {
  private Logger log = LoggerFactory.getLogger(LoggingWhirlpoolClientListener.class);
  private String poolId;

  public LoggingWhirlpoolClientListener(String poolId, WhirlpoolClientListener notifyListener) {
    super(notifyListener);
    this.poolId = poolId;
  }

  public LoggingWhirlpoolClientListener(String poolId) {
    this(poolId, null);
  }

  public void setLogPrefix(String logPrefix) {
    log = ClientUtils.prefixLogger(log, logPrefix);
  }

  private String format(String log) {
    return " - [MIX] " + (poolId != null ? poolId + " " : "") + log;
  }

  @Override
  public void success(MixSuccess mixSuccess) {
    super.success(mixSuccess);
    MixDestination destination = mixSuccess.getDestination();
    logInfo(
        format(
            "⣿ WHIRLPOOL SUCCESS ⣿ txid: "
                + mixSuccess.getReceiveUtxo().getHash()
                + ", receiveAddress="
                + destination.getAddress()
                + ", path="
                + destination.getPath()
                + ", type="
                + destination.getType()));
  }

  @Override
  public void fail(MixFail mixFail) {
    super.fail(mixFail);
    MixFailReason failReason = mixFail.getMixFailReason();
    String message = failReason.getMessage();
    String notifiableError = mixFail.getError();
    if (notifiableError != null) {
      message += " ; " + notifiableError;
    }
    if (MixFailReason.CANCEL.equals(failReason)) {
      logInfo(format(message));
    } else {
      MixDestination destination = mixFail.getDestination();
      String destinationStr =
          (destination != null
              ? ", receiveAddress="
                  + destination.getAddress()
                  + ", path="
                  + destination.getPath()
                  + ", type="
                  + destination.getType()
              : "");
      logError(format("⣿ WHIRLPOOL FAILED ⣿ " + message + destinationStr));
    }
  }

  @Override
  public void progress(MixProgress mixProgress) {
    super.progress(mixProgress);

    MixStep step = mixProgress.getMixProgressDetail().getMixStep();
    String asciiProgress = renderProgress(step.getProgress());
    logInfo(format(asciiProgress + " " + step + " : " + step.getMessage()));
  }

  private String renderProgress(int progressPercent) {
    StringBuilder progress = new StringBuilder();
    for (int i = 0; i < 100; i += 10) {
      progress.append(i < progressPercent ? "▮" : "▯");
    }
    progress.append(" (" + progressPercent + "%)");
    return progress.toString();
  }

  protected void logInfo(String message) {
    log.info(message);
  }

  protected void logError(String message) {
    log.error(message);
  }
}
