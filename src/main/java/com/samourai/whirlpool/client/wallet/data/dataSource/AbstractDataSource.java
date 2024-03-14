package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.ExpirablePaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataSource implements DataSource {
  private static final Logger log = LoggerFactory.getLogger(AbstractDataSource.class);

  protected final WhirlpoolWallet whirlpoolWallet;
  protected final WalletStateSupplier walletStateSupplier;
  protected final WalletSupplier walletSupplier;
  protected final PaynymSupplier paynymSupplier;
  protected final DataSourceConfig dataSourceConfig;

  public AbstractDataSource(
      WhirlpoolWallet whirlpoolWallet,
      HD_Wallet bip44w,
      WalletStateSupplier walletStateSupplier,
      DataSourceConfig dataSourceConfig)
      throws Exception {
    this.whirlpoolWallet = whirlpoolWallet;
    this.walletStateSupplier = walletStateSupplier;
    this.walletSupplier = computeWalletSupplier(bip44w, walletStateSupplier, dataSourceConfig);
    this.paynymSupplier = computePaynymSupplier(whirlpoolWallet, walletStateSupplier);
    this.dataSourceConfig = dataSourceConfig;
  }

  protected void load(boolean initial) throws Exception {
    // load paynym
    paynymSupplier.load();
  }

  @Override
  public void open(CoordinatorSupplier coordinatorSupplier) throws Exception {
    // initialize for coordinatorSupplier
    coordinatorSupplier.load();
    getUtxoSupplier()._setCoordinatorSupplier(coordinatorSupplier);

    // load initial data (or fail)
    load(true);
  }

  protected WalletSupplierImpl computeWalletSupplier(
      HD_Wallet bip44w, WalletStateSupplier walletStateSupplier, DataSourceConfig dataSourceConfig)
      throws Exception {
    return new WalletSupplierImpl(
        dataSourceConfig.getBipFormatSupplier(),
        walletStateSupplier,
        bip44w,
        dataSourceConfig.getBipWalletSupplier());
  }

  protected PaynymSupplier computePaynymSupplier(
      WhirlpoolWallet whirlpoolWallet, WalletStateSupplier walletStateSupplier) {
    return ExpirablePaynymSupplier.create(
        whirlpoolWallet.getConfig(), whirlpoolWallet.getBip47Account(), walletStateSupplier);
  }

  protected WhirlpoolWallet getWhirlpoolWallet() {
    return whirlpoolWallet;
  }

  public WalletStateSupplier getWalletStateSupplier() {
    return walletStateSupplier;
  }

  @Override
  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  @Override
  public PaynymSupplier getPaynymSupplier() {
    return paynymSupplier;
  }

  @Override
  public DataSourceConfig getDataSourceConfig() {
    return dataSourceConfig;
  }
}
