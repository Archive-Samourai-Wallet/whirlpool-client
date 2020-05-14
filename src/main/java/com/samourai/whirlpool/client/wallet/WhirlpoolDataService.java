package com.samourai.whirlpool.client.wallet;

import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolDataService {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolDataService.class);

  private final WhirlpoolWalletConfig config;
  private final MinerFeeSupplier minerFeeSupplier;
  private final PoolSupplier poolSupplier;

  public WhirlpoolDataService(WhirlpoolWalletConfig config) {
    this.config = config;
    this.minerFeeSupplier =
        new MinerFeeSupplier(
            config.getRefreshFeeDelay(),
            config.getBackendApi(),
            config.getFeeMin(),
            config.getFeeMax(),
            config.getFeeFallback());
    this.poolSupplier = new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi());
  }

  public AbstractSupplier[] getSuppliers() {
    return new AbstractSupplier[] {minerFeeSupplier, poolSupplier};
  }

  public WhirlpoolWalletConfig getConfig() {
    return config;
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }
}
