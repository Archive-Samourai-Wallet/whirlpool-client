package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.XPubPostmixHandler;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.bitcoinj.core.NetworkParameters;

public class ExternalDestination {
  private String xpub; // null when postmixHandlerCustom is set
  private IPostmixHandler postmixHandlerCustom; // may be null
  private IPostmixHandler postmixHandler; // set by getPostmixHandlerCustomOrDefault()
  private int chain;
  private int mixs;
  private int mixsRandomFactor;

  public ExternalDestination(String xpub, int chain, int mixs, int mixsRandomFactor) {
    this.xpub = xpub;
    this.postmixHandlerCustom = null;
    this.chain = chain;
    this.mixs = mixs;
    this.mixsRandomFactor = mixsRandomFactor;
  }

  // used by Sparrow
  public ExternalDestination(
      IPostmixHandler postmixHandlerCustom, int chain, int mixs, int mixsRandomFactor) {
    this.xpub = null;
    this.postmixHandlerCustom = postmixHandlerCustom;
    this.chain = chain;
    this.mixs = mixs;
    this.mixsRandomFactor = mixsRandomFactor;
  }

  public String getXpub() {
    return xpub;
  }

  // used by Sparrow
  public IPostmixHandler getPostmixHandlerCustom() {
    return postmixHandlerCustom;
  }

  public IPostmixHandler getPostmixHandlerCustomOrDefault(WhirlpoolWallet whirlpoolWallet) {
    if (postmixHandler == null) {
      if (postmixHandlerCustom != null) {
        // custom PostmixHandler
        postmixHandler = postmixHandlerCustom;
      } else {
        // default PostmixHandler
        IIndexHandler indexHandlerExternal =
            whirlpoolWallet.getWalletStateSupplier().getIndexHandlerExternal();
        NetworkParameters params = whirlpoolWallet.getConfig().getSamouraiNetwork().getParams();
        postmixHandler = new XPubPostmixHandler(indexHandlerExternal, params, xpub, chain);
      }
    }
    return postmixHandler;
  }

  public int getChain() {
    return chain;
  }

  public int getMixs() {
    return mixs;
  }

  public boolean useRandomDelay() {
    // 0 => never
    if (mixsRandomFactor == 0) {
      return false;
    }
    // random
    return RandomUtil.getInstance().random(1, mixsRandomFactor) == 1;
  }

  @Override
  public String toString() {
    return "xpub="
        + ClientUtils.maskString(xpub)
        + ", chain="
        + chain
        + ", mixs="
        + mixs
        + ", mixsRandomFactor="
        + mixsRandomFactor;
  }
}
