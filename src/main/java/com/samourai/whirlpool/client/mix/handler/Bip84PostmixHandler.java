package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandler extends AbstractPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(Bip84PostmixHandler.class);

  private Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private Bip84Wallet postmixWallet;
  private boolean mobile;

  public Bip84PostmixHandler(Bip84Wallet postmixWallet, boolean mobile) {
    super(postmixWallet.getIndexHandler());
    this.postmixWallet = postmixWallet;
    this.mobile = mobile;
  }

  @Override
  protected int computeNextReceiveAddressIndex() {
    return ClientUtils.computeNextReceiveAddressIndex(postmixWallet.getIndexHandler(), this.mobile);
  }

  @Override
  protected String getAddressAt(int receiveAddressIndex, NetworkParameters params) {
    HD_Address receiveAddress =
        postmixWallet.getAddressAt(Bip84Wallet.CHAIN_RECEIVE, receiveAddressIndex);

    String bech32Address = bech32Util.toBech32(receiveAddress, params);
    if (log.isDebugEnabled()) {
      log.debug(
          "receiveAddress=" + bech32Address + ", path=" + receiveAddress.toJSON().get("path"));
    }
    return bech32Address;
  }
}
