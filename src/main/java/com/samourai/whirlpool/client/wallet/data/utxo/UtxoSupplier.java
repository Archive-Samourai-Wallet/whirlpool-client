package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import java.util.Collection;

public interface UtxoSupplier extends UtxoProvider {

  Collection<WhirlpoolUtxo> findUtxos(WhirlpoolAccount... whirlpoolAccounts);

  Collection<WhirlpoolUtxo> findUtxos(
      BipFormat bipFormat, final WhirlpoolAccount... whirlpoolAccounts);

  Collection<WhirlpoolUtxo> findUtxosByAddress(String address);

  Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount);

  long getBalance(WhirlpoolAccount whirlpoolAccount);

  long getBalanceTotal();

  WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex);

  UtxoData getValue();

  Long getLastUpdate();

  void refresh() throws Exception;

  boolean isMixableUtxo(BipUtxo unspentOutput, BipWallet bipWallet);
}
