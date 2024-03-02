package com.samourai.whirlpool.client.mix;

import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;

public class MixParams {
  private String poolId;
  private long denomination;
  private long mustMixBalanceMin;
  private long mustMixBalanceMax;
  private WhirlpoolUtxo whirlpoolUtxo;
  private IPremixHandler premixHandler;
  private IPostmixHandler postmixHandler;
  private ChainSupplier chainSupplier;
  private CoordinatorSupplier coordinatorSupplier;
  private WhirlpoolApiClient whirlpoolApiClient; // temporary identity for Soroban

  public MixParams(
      String poolId,
      long denomination,
      long mustMixBalanceMin,
      long mustMixBalanceMax,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier,
      CoordinatorSupplier coordinatorSupplier,
      WhirlpoolApiClient whirlpoolApiClient) {
    this.poolId = poolId;
    this.denomination = denomination;
    this.mustMixBalanceMin = mustMixBalanceMin;
    this.mustMixBalanceMax = mustMixBalanceMax;
    this.whirlpoolUtxo = whirlpoolUtxo;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
    this.chainSupplier = chainSupplier;
    this.coordinatorSupplier = coordinatorSupplier;
    this.whirlpoolApiClient = whirlpoolApiClient;
  }

  public MixParams(
      Pool pool,
      WhirlpoolUtxo whirlpoolUtxo,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ChainSupplier chainSupplier,
      CoordinatorSupplier coordinatorSupplier,
      WhirlpoolApiClient whirlpoolApiClient) {
    this(
        pool.getPoolId(),
        pool.getDenomination(),
        pool.getPremixValueMin(),
        pool.getPremixValueMax(),
        whirlpoolUtxo,
        premixHandler,
        postmixHandler,
        chainSupplier,
        coordinatorSupplier,
        whirlpoolApiClient);
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

  public CoordinatorSupplier getCoordinatorSupplier() {
    return coordinatorSupplier;
  }

  public WhirlpoolApiClient getWhirlpoolApiClient() {
    return whirlpoolApiClient;
  }
}
