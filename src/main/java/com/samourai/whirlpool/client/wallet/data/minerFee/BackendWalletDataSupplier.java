package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendWalletDataSupplier extends WalletDataSupplier {
  private static final Logger log = LoggerFactory.getLogger(BackendWalletDataSupplier.class);

  public BackendWalletDataSupplier(
      int refreshUtxoDelay, WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier)
      throws Exception {
    super(refreshUtxoDelay, config, bip44w, walletIdentifier);
  }

  @Override
  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] pubs = getWalletSupplier().getPubs(true);
    return getConfig().getBackendApi().fetchWallet(pubs);
  }
}
