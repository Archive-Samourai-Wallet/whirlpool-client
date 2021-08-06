package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.WalletResponse;
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
      WalletSupplier walletSupplier,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      String utxoConfigFileName,
      WhirlpoolWalletConfig config) {
    super(refreshUtxoDelay, walletSupplier, utxoChangesListener, utxoConfigFileName, config);
    this.backendApi = config.getBackendApi();
  }

  @Override
  protected WalletResponse fetchWalletResponse() throws Exception {
    String[] utxoZpubs = walletSupplier.getZpubs(false);
    return backendApi.fetchWallet(utxoZpubs);
  }
}
