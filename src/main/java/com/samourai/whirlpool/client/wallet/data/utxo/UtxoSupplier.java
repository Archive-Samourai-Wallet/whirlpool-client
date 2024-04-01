package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import java.util.Collection;
import java.util.function.Consumer;

public interface UtxoSupplier extends UtxoProvider {

  Collection<WhirlpoolUtxo> findUtxos(SamouraiAccount... samouraiAccounts);

  Collection<WhirlpoolUtxo> findUtxos(
      BipFormat bipFormat, final SamouraiAccount... samouraiAccounts);

  Collection<WhirlpoolUtxo> findUtxosByAddress(String address);

  Collection<WalletResponse.Tx> findTxs(SamouraiAccount samouraiAccount);

  long getBalance(SamouraiAccount samouraiAccount);

  long getBalanceTotal();

  WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex);

  UtxoData getValue();

  Long getLastUpdate();

  void _setCoordinatorSupplier(CoordinatorSupplier coordinatorSupplier);

  void _setUtxoChangeListener(Consumer<UtxoData> onUtxoChange);

  void refresh() throws Exception;

  boolean isMixableUtxo(UnspentOutput unspentOutput, SamouraiAccount samouraiAccount);

  WhirlpoolUtxo computeWhirlpoolUtxo(UnspentOutput utxo, int latestBlockHeight) throws Exception;
}
