package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisterFile;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersisterFile;
import java.io.File;

public class FileDataPersisterFactory extends AbstractDataPersisterFactory {

  public FileDataPersisterFactory() {
    super();
  }

  @Override
  protected WalletStatePersisterFile computeWalletStatePersister(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    String walletStateFileName =
        computeFileIndex(whirlpoolWallet.getWalletIdentifier()).getAbsolutePath();
    return new WalletStatePersisterFile(walletStateFileName);
  }

  @Override
  protected UtxoConfigPersisterFile computeUtxoConfigPersister(WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    String utxoConfigFileName =
        computeFileUtxos(whirlpoolWallet.getWalletIdentifier()).getAbsolutePath();
    return new UtxoConfigPersisterFile(utxoConfigFileName);
  }

  protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-state-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFile(String fileName) throws NotifiableException {
    return ClientUtils.createFile(fileName); // use current directory
  }
}
