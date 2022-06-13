package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.ExternalDestination;
import com.samourai.whirlpool.client.wallet.data.supplier.IPersister;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigData;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateData;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public abstract class AbstractDataPersisterFactory implements DataPersisterFactory {

  public AbstractDataPersisterFactory() {}

  @Override
  public DataPersister createDataPersister(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
      throws Exception {
    int persistDelaySeconds = whirlpoolWallet.getConfig().getPersistDelaySeconds();
    WalletStateSupplier walletStateSupplier = computeWalletStateSupplier(whirlpoolWallet);
    UtxoConfigSupplier utxoConfigSupplier = computeUtxoConfigSupplier(whirlpoolWallet);
    return new OrchestratedDataPersister(
        whirlpoolWallet, persistDelaySeconds, walletStateSupplier, utxoConfigSupplier);
  }

  protected WalletStateSupplier computeWalletStateSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    IPersister<WalletStateData> persister = computeWalletStatePersister(whirlpoolWallet);
    ExternalDestination externalDestination = whirlpoolWallet.getConfig().getExternalDestination();
    return new WalletStatePersistableSupplier(persister, externalDestination);
  }

  protected UtxoConfigSupplier computeUtxoConfigSupplier(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    IPersister<UtxoConfigData> persister = computeUtxoConfigPersister(whirlpoolWallet);
    return new UtxoConfigPersistableSupplier(persister);
  }

  protected abstract IPersister<WalletStateData> computeWalletStatePersister(
      WhirlpoolWallet whirlpoolWallet) throws Exception;

  protected abstract IPersister<UtxoConfigData> computeUtxoConfigPersister(
      WhirlpoolWallet whirlpoolWallet) throws Exception;
}
