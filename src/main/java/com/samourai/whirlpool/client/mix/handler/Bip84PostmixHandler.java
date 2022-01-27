package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandler extends AbstractPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(Bip84PostmixHandler.class);

  private BipWallet postmixWallet;
  private IndexRange indexRange;

  public Bip84PostmixHandler(
      NetworkParameters params, BipWallet postmixWallet, IndexRange indexRange) {
    super(postmixWallet.getIndexHandlerReceive(), params);
    this.postmixWallet = postmixWallet;
    this.indexRange = indexRange;
  }

  @Override
  protected MixDestination computeNextDestination() throws Exception {
    // index
    int index =
        ClientUtils.computeNextReceiveAddressIndex(
            postmixWallet.getIndexHandlerReceive(), this.indexRange);

    // address
    BipAddress receiveAddress = postmixWallet.getAddressAt(Chain.RECEIVE.getIndex(), index);

    String address = receiveAddress.getAddressString();
    String path = receiveAddress.getPathAddress();
    if (log.isDebugEnabled()) {
      log.debug("Mixing to POSTMIX -> receiveAddress=" + address + ", path=" + path);
    }
    return new MixDestination(DestinationType.POSTMIX, index, address, path);
  }
}
