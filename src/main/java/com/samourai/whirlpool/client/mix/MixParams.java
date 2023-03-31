package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.RpcWallet;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

public class MixParams {
  private String poolId;
  private long denomination;
  private long mustMixBalanceMin;
  private long mustMixBalanceMax;
  private WhirlpoolUtxo whirlpoolUtxo;
  private IPremixHandler premixHandler;
  private IPostmixHandler postmixHandler;
  private ChainSupplier chainSupplier;
  private RpcWallet rpcWallet;

  public MixParams(
      String poolId,
      long denomination,
      long mustMixBalanceMin,
      long mustMixBalanceMax,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier,
      RpcWallet rpcWallet) {
    this.poolId = poolId;
    this.denomination = denomination;
    this.mustMixBalanceMin = mustMixBalanceMin;
    this.mustMixBalanceMax = mustMixBalanceMax;
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
    this.chainSupplier = chainSupplier;
    this.rpcWallet = rpcWallet;
  }

  public MixParams(
      Pool pool,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier,
      RpcWallet rpcWallet) {
    this(
        pool.getPoolId(),
        pool.getDenomination(),
        pool.getPremixValueMin(),
        pool.getPremixValueMax(),
        whirlpoolUtxo,
        premixHandler,
        postmixHandler,
        chainSupplier,
        rpcWallet);
  }

  public MixParams(MixParams mixParams, IPremixHandler premixHandler) {
    this(
        mixParams.getPoolId(),
        mixParams.getDenomination(),
        mixParams.getMustMixBalanceMin(),
        mixParams.getMustMixBalanceMax(),
        mixParams.getWhirlpoolUtxo(),
        premixHandler,
        mixParams.getPostmixHandler(),
        mixParams.getChainSupplier(),
        mixParams.getRpcWallet());
  }

  public String getPoolId() {
    return poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public long getMustMixBalanceMin() {
    return mustMixBalanceMin;
  }

  public long getMustMixBalanceMax() {
    return mustMixBalanceMax;
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

  public RpcWallet getRpcWallet() {
    return rpcWallet;
  }
}
