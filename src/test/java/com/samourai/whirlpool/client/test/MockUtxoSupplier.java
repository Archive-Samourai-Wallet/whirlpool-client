package com.samourai.whirlpool.client.test;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.MockUtxoProvider;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.core.NetworkParameters;

public class MockUtxoSupplier extends MockUtxoProvider {
  private BasicUtxoSupplier utxoSupplier;

  public MockUtxoSupplier(
      NetworkParameters params, WalletSupplier walletSupplier, BasicUtxoSupplier utxoSupplier)
      throws Exception {
    super(params, walletSupplier);
    this.utxoSupplier = utxoSupplier;
  }

  public MockUtxoSupplier(WhirlpoolWallet whirlpoolWallet) throws Exception {
    this(
        whirlpoolWallet.getConfig().getNetworkParameters(),
        whirlpoolWallet.getWalletSupplier(),
        (BasicUtxoSupplier) whirlpoolWallet.getUtxoSupplier());
  }

  public UTXO addUtxo(BIP_WALLET bip_wallet, long value) throws Exception {
    BipWallet bipWallet = getWalletSupplier().getWallet(bip_wallet);
    UTXO utxo = super.addUtxo(bipWallet, value);
    setValue();
    return utxo;
  }

  private void setValue() {
    List<BipUtxo> utxos = new LinkedList<>();
    for (WhirlpoolAccount whirlpoolAccount : WhirlpoolAccount.values()) {
      for (UTXO utxo : getUtxos(whirlpoolAccount)) {
        for (BipUtxo bipUtxo : utxo.toBipUtxos()) {
          utxos.add(bipUtxo);
        }
      }
    }
    UtxoData utxoData = new UtxoData(utxos.toArray(new BipUtxo[] {}), new WalletResponse.Tx[] {});
    try {
      utxoSupplier.setValue(utxoData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clear() {
    super.clear();
    setValue();
  }
}
