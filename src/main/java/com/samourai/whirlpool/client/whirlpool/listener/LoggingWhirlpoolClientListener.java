package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingWhirlpoolClientListener extends AbstractWhirlpoolClientListener {
  private Logger log = LoggerFactory.getLogger(LoggingWhirlpoolClientListener.class);

  public LoggingWhirlpoolClientListener(WhirlpoolClientListener notifyListener) {
    super(notifyListener);
  }

  public LoggingWhirlpoolClientListener() {
    super();
  }

  public void setLogPrefix(String logPrefix) {
    log = ClientUtils.prefixLogger(log, logPrefix);
  }

  private String format(String log) {
    return " - [MIX] " + log;
  }

  @Override
  public void success(MixSuccess mixSuccess) {
    super.success(mixSuccess);
    logInfo("⣿ WHIRLPOOL SUCCESS ⣿");

    logInfo(format("⣿ WHIRLPOOL SUCCESS ⣿ txid: " + mixSuccess.getReceiveUtxo().getHash()));
  }

  @Override
  public void fail() {
    super.fail();
    logError(format("⣿ WHIRLPOOL FAILED ⣿ Check logs for errors."));
  }

  @Override
  public void progress(MixStep step, String stepInfo, int stepNumber, int nbSteps) {
    super.progress(step, stepInfo, stepNumber, nbSteps);
    String asciiProgress = renderProgress(stepNumber, nbSteps);
    logInfo(format(asciiProgress + " " + step + " : " + stepInfo));
  }

  private String renderProgress(int stepNumber, int nbSteps) {
    StringBuilder progress = new StringBuilder();
    for (int i = 0; i < nbSteps; i++) {
      progress.append(i < stepNumber ? "▮" : "▯");
    }
    progress.append(" (" + stepNumber + "/" + nbSteps + ")");
    return progress.toString();
  }

  protected void logInfo(String message) {
    log.info(message);
  }

  protected void logError(String message) {
    log.error(message);
  }
}
