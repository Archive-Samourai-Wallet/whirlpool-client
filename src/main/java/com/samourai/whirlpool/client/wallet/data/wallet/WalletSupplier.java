package com.samourai.whirlpool.client.wallet.data.wallet;

import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.AddressType;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

public interface WalletSupplier {
  BipWalletAndAddressType getWallet(WhirlpoolAccount account, AddressType addressType);

  BipWalletAndAddressType getWalletByPub(String pub);

  String[] getPubs(boolean withIgnoredAccounts);
}
