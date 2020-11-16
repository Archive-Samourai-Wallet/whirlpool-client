package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.util.XPubUtil;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XPubPostmixHandler extends AbstractPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(XPubPostmixHandler.class);
  private static final XPubUtil xPubUtil = XPubUtil.getInstance();

  private String xPub;
  private int chain;
  private int startIndex;

  public XPubPostmixHandler(String xPub, int chain, int startIndex, IIndexHandler indexHandler) {
    super(indexHandler);
    this.xPub = xPub;
    this.chain = chain;
    this.startIndex = startIndex;
  }

  @Override
  protected int computeNextReceiveAddressIndex() {
    int index = indexHandler.getAndIncrementUnconfirmed();
    index = Math.max(index, startIndex);
    return index;
  }

  @Override
  protected String getAddressAt(int receiveAddressIndex, NetworkParameters params) {
    String bech32Address = xPubUtil.getAddressBech32(xPub, receiveAddressIndex, chain, params);
    log.info(
        "Mixing to external xPub -> receiveAddress="
            + bech32Address
            + ", path="
            + xPubUtil.getPath(receiveAddressIndex, chain));
    return bech32Address;
  }
}
