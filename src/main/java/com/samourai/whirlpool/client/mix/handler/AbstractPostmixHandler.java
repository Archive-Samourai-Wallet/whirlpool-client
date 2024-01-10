package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPostmixHandler implements IPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(AbstractPostmixHandler.class);

  protected IIndexHandler indexHandler;
  protected NetworkParameters params;

  protected MixDestination destination;

  public AbstractPostmixHandler(IIndexHandler indexHandler, NetworkParameters params) {
    this.indexHandler = indexHandler;
    this.params = params;
  }

  protected abstract IndexRange getIndexRange();

  @Override
  public final MixDestination computeDestinationNext() throws Exception {
    // use "unconfirmed" index to avoid huge index gaps on multiple mix failures
    int index = ClientUtils.computeNextReceiveAddressIndex(getIndexHandler(), getIndexRange());
    this.destination = computeDestination(index);
    if (log.isDebugEnabled()) {
      log.debug(
          "Mixing to "
              + destination.getType()
              + " -> receiveAddress="
              + destination.getAddress()
              + ", path="
              + destination.getPath());
    }
    return destination;
  }

  @Override
  public void onMixFail() {
    if (destination != null) {
      // cancel unconfirmed postmix index if output was not registered yet
      indexHandler.cancelUnconfirmed(destination.getIndex());
    }
  }

  @Override
  public void onRegisterOutput() {
    // confirm postmix index on REGISTER_OUTPUT success
    indexHandler.confirmUnconfirmed(destination.getIndex());
  }

  @Override
  public IIndexHandler getIndexHandler() {
    return indexHandler;
  }
}
