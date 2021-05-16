package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.data.BasicSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainSupplier extends BasicSupplier<ChainData> {
  private static final Logger log = LoggerFactory.getLogger(ChainSupplier.class);

  public ChainSupplier() {
    super(log, null);
  }

  protected void _setValue(WalletResponse walletResponse) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }

    // validate
    if (walletResponse == null
        || walletResponse.info == null
        || walletResponse.info.latest_block == null
        || walletResponse.info.latest_block.height <= 0) {
      throw new Exception("Invalid walletResponse.info.latest_block");
    }

    ChainData value = new ChainData(walletResponse.info.latest_block);
    super.setValue(value);
  }

  public int getLatestBlockHeight() {
    return getValue().getLatestBlock().height;
  }
}
