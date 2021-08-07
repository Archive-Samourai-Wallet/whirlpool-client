package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendWalletDataSupplier extends WalletDataSupplier {
  private static final Logger log = LoggerFactory.getLogger(BackendWalletDataSupplier.class);

  private final BackendApi backendApi;

  public BackendWalletDataSupplier(
      int refreshUtxoDelay,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      WhirlpoolWalletConfig config,
      HD_Wallet bip84w,
      String walletIdentifier)
      throws Exception {
    super(refreshUtxoDelay, utxoChangesListener, config, bip84w, walletIdentifier);
    this.backendApi = config.getBackendApi();
  }

  @Override
  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] utxoZpubs = walletSupplier.getZpubs(false);
    return backendApi.fetchWallet(utxoZpubs);
  }
}
