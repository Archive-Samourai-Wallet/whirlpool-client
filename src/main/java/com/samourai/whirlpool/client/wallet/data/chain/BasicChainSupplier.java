package com.samourai.whirlpool.client.wallet.data.chain;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.event.ChainBlockChangeEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicChainSupplier extends BasicSupplier<ChainData> implements ChainSupplier {
  private static final Logger log = LoggerFactory.getLogger(BasicChainSupplier.class);

  public BasicChainSupplier() {
    super(log);
  }

  @Override
  protected void validate(ChainData value) throws Exception {
    if (value == null
        || value.getLatestBlock() == null
        || value.getLatestBlock().height <= 0
        || value.getLatestBlock().time <= 0
        || StringUtils.isEmpty(value.getLatestBlock().hash)) {
      throw new Exception("Invalid ChainData");
    }
  }

  @Override
  protected void onValueChange(ChainData value) {
    WhirlpoolEventService.getInstance().post(new ChainBlockChangeEvent(value.getLatestBlock()));
  }

  @Override
  public void setValue(ChainData value) throws Exception { // make public
    super.setValue(value);
  }

  @Override
  public WalletResponse.InfoBlock getLatestBlock() {
    return getValue().getLatestBlock();
  }
}
