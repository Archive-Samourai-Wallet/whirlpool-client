package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPostmixHandler implements IPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(AbstractPostmixHandler.class);

  private Integer receiveIndex;
  protected IIndexHandler indexHandler;

  public AbstractPostmixHandler(IIndexHandler indexHandler) {
    this.indexHandler = indexHandler;
    this.receiveIndex = null;
  }

  protected abstract int computeNextReceiveAddressIndex();

  protected abstract String getAddressAt(int index, NetworkParameters params);

  @Override
  public synchronized String computeReceiveAddress(NetworkParameters params) throws Exception {
    // use "unconfirmed" index to avoid huge index gaps on multiple mix failures
    this.receiveIndex = computeNextReceiveAddressIndex();
    String receiveAddress = getAddressAt(this.receiveIndex, params);
    return receiveAddress;
  }

  @Override
  public void onMixFail() {
    if (receiveIndex != null) {
      indexHandler.cancelUnconfirmed(receiveIndex);
    }
  }

  @Override
  public void onRegisterOutput() {
    // confirm receive address even when REGISTER_OUTPUT fails, to avoid 'ouput already registered'
    indexHandler.confirmUnconfirmed(receiveIndex);
  }
}
