package com.samourai.whirlpool.client.wallet.data.chain;

import com.samourai.wallet.api.backend.beans.WalletResponse;

public class ChainData {
  private WalletResponse.InfoBlock latestBlock;

  public ChainData(WalletResponse.InfoBlock latestBlock) {
    this.latestBlock = latestBlock;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChainData)) return false;

    ChainData c = (ChainData) o;
    return (latestBlock != null
        && c.latestBlock != null
        && latestBlock.height == c.latestBlock.height);
  }

  public WalletResponse.InfoBlock getLatestBlock() {
    return latestBlock;
  }
}
