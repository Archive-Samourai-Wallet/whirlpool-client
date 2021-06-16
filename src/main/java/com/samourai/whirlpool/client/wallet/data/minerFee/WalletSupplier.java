package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateIndexHandler;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletSupplier.class);
  private static final String EXTERNAL_INDEX_HANDLER = "external";

  private final WalletStateSupplier walletStateSupplier;
  private final Map<WhirlpoolAccount, Map<AddressType, BipWalletAndAddressType>> wallets;
  private final Map<String, BipWalletAndAddressType> walletsByPub;
  private final IIndexHandler externalIndexHandler;

  public WalletSupplier(
      WalletStatePersister persister,
      BackendApi backendApi,
      HD_Wallet bip44w,
      int externalIndexDefault) {

    this.walletStateSupplier = new WalletStateSupplier(persister, backendApi, this);

    // instanciate wallets
    this.wallets = new LinkedHashMap<WhirlpoolAccount, Map<AddressType, BipWalletAndAddressType>>();
    this.walletsByPub = new LinkedHashMap<String, BipWalletAndAddressType>();
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {

      Map<AddressType, BipWalletAndAddressType> walletsByAddressType =
          new LinkedHashMap<AddressType, BipWalletAndAddressType>();
      for (AddressType addressType : account.getAddressTypes()) {
        IIndexHandler indexHandler =
            walletStateSupplier.computeIndexHandler(account, addressType, Chain.RECEIVE);
        IIndexHandler indexChangeHandler =
            walletStateSupplier.computeIndexHandler(account, addressType, Chain.CHANGE);
        BipWalletAndAddressType bipWallet =
            new BipWalletAndAddressType(
                bip44w, account, indexHandler, indexChangeHandler, addressType);
        walletsByAddressType.put(addressType, bipWallet);
        String pub = bipWallet.getPub();
        walletsByPub.put(pub, bipWallet);
        if (log.isDebugEnabled()) {
          log.debug(" +WALLET (" + account + "/" + addressType + "): " + pub);
        }
      }
      wallets.put(account, walletsByAddressType);
    }
    this.externalIndexHandler =
        new WalletStateIndexHandler(
            walletStateSupplier, EXTERNAL_INDEX_HANDLER, externalIndexDefault);
  }

  public BipWalletAndAddressType getWallet(WhirlpoolAccount account, AddressType addressType) {
    if (!wallets.containsKey(account) || !wallets.get(account).containsKey(addressType)) {
      log.error("No wallet found for " + account + " / " + addressType);
      return null;
    }
    BipWalletAndAddressType wallet = wallets.get(account).get(addressType);
    return wallet;
  }

  public BipWalletAndAddressType getWalletByPub(String pub) {
    BipWalletAndAddressType bipWallet = walletsByPub.get(pub);
    if (bipWallet == null) {
      log.error("No wallet found for: " + pub);
      return null;
    }
    return bipWallet;
  }

  public String[] getPubs(boolean withIgnoredAccounts) {
    List<String> pubs = new LinkedList<String>();
    for (BipWalletAndAddressType bipWallet : walletsByPub.values()) {
      if (withIgnoredAccounts || bipWallet.getAccount().isActive()) {
        String pub = bipWallet.getPub();
        pubs.add(pub);
      }
    }
    return pubs.toArray(new String[] {});
  }

  public WalletStateSupplier getWalletStateSupplier() {
    return walletStateSupplier;
  }

  public IIndexHandler getExternalIndexHandler() {
    return externalIndexHandler;
  }
}
