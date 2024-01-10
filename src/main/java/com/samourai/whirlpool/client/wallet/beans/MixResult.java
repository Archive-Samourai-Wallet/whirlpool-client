package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.utxo.UtxoRef;
import com.samourai.whirlpool.client.mix.handler.DestinationType;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;

public class MixResult {
  private long time;
  private boolean success;
  private String poolId;
  private long amount;
  private boolean liquidity;
  private UtxoRef destinationUtxo;
  private String destinationAddress;
  private DestinationType destinationType;
  private String destinationPath;
  private UtxoRef failUtxo;
  private MixFailReason failReason;
  private String failError;

  protected MixResult(
      long time,
      boolean success,
      String poolId,
      long amount,
      boolean liquidity,
      UtxoRef destinationUtxo,
      MixDestination mixDestination,
      UtxoRef failUtxo,
      MixFailReason failReason,
      String failError) {
    this.time = time;
    this.success = success;
    this.poolId = poolId;
    this.amount = amount;
    this.liquidity = liquidity;
    this.destinationUtxo = destinationUtxo;
    this.destinationAddress = mixDestination != null ? mixDestination.getAddress() : null;
    this.destinationType = mixDestination != null ? mixDestination.getType() : null;
    this.destinationPath = mixDestination != null ? mixDestination.getPath() : null;
    this.failUtxo = failUtxo;
    this.failReason = failReason;
    this.failError = failError;
  }

  // success
  public MixResult(
      long time,
      String poolId,
      long amount,
      boolean liquidity,
      UtxoRef destinationUtxo,
      MixDestination mixDestination) {
    this(time, true, poolId, amount, liquidity, destinationUtxo, mixDestination, null, null, null);
  }

  // fail
  public MixResult(
      long time,
      String poolId,
      long amount,
      boolean liquidity,
      UtxoRef failUtxo,
      MixFailReason failReason,
      String failError) {
    this(time, false, poolId, amount, liquidity, null, null, failUtxo, failReason, failError);
  }

  public long getTime() {
    return time;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getPoolId() {
    return poolId;
  }

  public long getAmount() {
    return amount;
  }

  public boolean isLiquidity() {
    return liquidity;
  }

  public UtxoRef getDestinationUtxo() {
    return destinationUtxo;
  }

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public DestinationType getDestinationType() {
    return destinationType;
  }

  public String getDestinationPath() {
    return destinationPath;
  }

  public UtxoRef getFailUtxo() {
    return failUtxo;
  }

  public MixFailReason getFailReason() {
    return failReason;
  }

  public String getFailError() {
    return failError;
  }
}