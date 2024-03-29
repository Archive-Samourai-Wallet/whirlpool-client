package com.samourai.whirlpool.client.wallet.beans;

public enum WhirlpoolUtxoStatus {
  READY,
  STOP,

  TX0,
  TX0_FAILED,
  TX0_SUCCESS,

  MIX_QUEUE,
  MIX_STARTED,
  MIX_SUCCESS,
  MIX_FAILED;

  public static boolean isMixQueuePossible(WhirlpoolUtxoStatus utxoStatus) {
    return WhirlpoolUtxoStatus.MIX_FAILED.equals(utxoStatus)
        || WhirlpoolUtxoStatus.READY.equals(utxoStatus)
        || WhirlpoolUtxoStatus.STOP.equals(utxoStatus);
  }

  public static boolean isTx0Possible(WhirlpoolUtxoStatus utxoStatus) {
    return WhirlpoolUtxoStatus.isMixQueuePossible(utxoStatus)
        // when aggregating
        || WhirlpoolUtxoStatus.MIX_QUEUE.equals(utxoStatus)
        || WhirlpoolUtxoStatus.MIX_FAILED.equals(utxoStatus);
  }

  public static boolean isAutoTx0Possible(WhirlpoolUtxoStatus utxoStatus) {
    return utxoStatus != WhirlpoolUtxoStatus.TX0
        && utxoStatus != WhirlpoolUtxoStatus.TX0_SUCCESS
        && utxoStatus != WhirlpoolUtxoStatus.MIX_STARTED
        && utxoStatus != WhirlpoolUtxoStatus.MIX_SUCCESS;
  }
}
