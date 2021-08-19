package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.event.UtxosRequestEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.supplier.ExpirableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletResponseSupplier extends ExpirableSupplier<WalletResponse> {
  private static final Logger log = LoggerFactory.getLogger(WalletResponseSupplier.class);

  private WalletResponseDataSource dataSource;

  public WalletResponseSupplier(WhirlpoolWalletConfig config, WalletResponseDataSource dataSource)
      throws Exception {
    super(config.getRefreshUtxoDelay(), null, log);
    this.dataSource = dataSource;
  }

  @Override
  protected WalletResponse fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }

    // notify
    WhirlpoolEventService.getInstance().post(new UtxosRequestEvent());
    WalletResponse walletResponse = dataSource.fetchWalletResponse();
    return walletResponse;
  }

  @Override
  protected void setValue(WalletResponse value) throws Exception {
    super.setValue(value);

    // notify
    dataSource.setValue(value);
  }
}
