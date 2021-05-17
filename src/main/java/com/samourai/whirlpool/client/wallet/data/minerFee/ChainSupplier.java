package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.event.ChainBlockChangeEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.BasicSupplier;
import org.apache.commons.lang3.StringUtils;
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
        || walletResponse.info.latest_block.height <= 0
        || walletResponse.info.latest_block.time <= 0
        || StringUtils.isEmpty(walletResponse.info.latest_block.hash)) {
      throw new Exception("Invalid walletResponse.info.latest_block");
    }

    ChainData oldValue = getValue();

    ChainData value = new ChainData(walletResponse.info.latest_block);
    super.setValue(value);

    // notify new blocks
    if (oldValue == null || oldValue.getLatestBlock().height != value.getLatestBlock().height) {
      WhirlpoolEventService.getInstance().post(new ChainBlockChangeEvent(value.getLatestBlock()));
    }
  }

  public int getLatestBlockHeight() {
    return getValue().getLatestBlock().height;
  }
}
