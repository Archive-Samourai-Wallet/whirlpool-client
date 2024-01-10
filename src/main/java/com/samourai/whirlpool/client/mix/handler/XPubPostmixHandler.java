package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.util.XPubUtil;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XPubPostmixHandler extends AbstractPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(XPubPostmixHandler.class);
  private static final XPubUtil xPubUtil = XPubUtil.getInstance();

  private String xPub;
  private int chain;

  public XPubPostmixHandler(
      IIndexHandler indexHandler, NetworkParameters params, String xPub, int chain) {
    super(indexHandler, params);
    this.xPub = xPub;
    this.chain = chain;
  }

  @Override
  protected IndexRange getIndexRange() {
    return IndexRange.FULL;
  }

  @Override
  public MixDestination computeDestination(int index) throws Exception {
    String address = xPubUtil.getAddressBech32(xPub, index, chain, params);
    String path = xPubUtil.getPath(index, chain);
    return new MixDestination(DestinationType.XPUB, index, address, path);
  }
}
