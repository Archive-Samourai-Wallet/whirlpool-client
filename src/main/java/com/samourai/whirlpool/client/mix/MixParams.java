package com.samourai.whirlpool.client.mix;

import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

public class MixParams {
  private String poolId;
  private long denomination;
  private WhirlpoolUtxo whirlpoolUtxo;
  private IPremixHandler premixHandler;
  private IPostmixHandler postmixHandler;
  private ChainSupplier chainSupplier;

  public MixParams(
      String poolId,
      long denomination,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier) {
    this.poolId = poolId;
    this.denomination = denomination;
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
    this.chainSupplier = chainSupplier;
  }

  public MixParams(
      Pool pool,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier) {
    this(
        pool.getPoolId(),
        pool.getDenomination(),
        whirlpoolUtxo,
        premixHandler,
        postmixHandler,
        chainSupplier);
  }

  public MixParams(MixParams mixParams, IPremixHandler premixHandler) {
    this(
        mixParams.getPoolId(),
        mixParams.getDenomination(),
        mixParams.getWhirlpoolUtxo(),
        premixHandler,
        mixParams.getPostmixHandler(),
        mixParams.getChainSupplier());
  }

  public String getPoolId() {
    return poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public WhirlpoolUtxo getWhirlpoolUtxo() {
    return whirlpoolUtxo;
  }

  public IPremixHandler getPremixHandler() {
    return premixHandler;
  }

  public IPostmixHandler getPostmixHandler() {
    return postmixHandler;
  }

  public ChainSupplier getChainSupplier() {
    return chainSupplier;
  }
}
