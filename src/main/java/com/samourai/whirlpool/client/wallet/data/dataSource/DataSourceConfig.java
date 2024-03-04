package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWalletSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceConfig {
  private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
  protected final MinerFeeSupplier minerFeeSupplier;
  protected final ChainSupplier chainSupplier;
  protected final BipFormatSupplier bipFormatSupplier;
  protected final BipWalletSupplier bipWalletSupplier;

  public DataSourceConfig(
      MinerFeeSupplier minerFeeSupplier,
      ChainSupplier chainSupplier,
      BipFormatSupplier bipFormatSupplier,
      BipWalletSupplier bipWalletSupplier) {
    this.minerFeeSupplier = minerFeeSupplier;
    this.chainSupplier = chainSupplier;
    this.bipFormatSupplier = bipFormatSupplier;
    this.bipWalletSupplier = bipWalletSupplier;
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public ChainSupplier getChainSupplier() {
    return chainSupplier;
  }

  public BipFormatSupplier getBipFormatSupplier() {
    return bipFormatSupplier;
  }

  public BipWalletSupplier getBipWalletSupplier() {
    return bipWalletSupplier;
  }
}
