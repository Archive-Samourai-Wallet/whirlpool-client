package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateIndexHandler;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletSupplier.class);

  private final Map<WhirlpoolAccount, Bip84Wallet> walletsByAccount;
  private final Map<String, WhirlpoolAccount> accountsByZpub;
  private final WhirlpoolAccount[] ignoredAccounts;

  private WalletStateSupplier walletStateSupplier;

  public WalletSupplier(
      int refreshUtxoDelay,
      WalletStatePersister persister,
      BackendApi backendApi,
      HD_Wallet hdWallet) {

    this.walletStateSupplier =
        new WalletStateSupplier(refreshUtxoDelay, persister, backendApi, this);

    // instanciate wallets
    this.walletsByAccount = new LinkedHashMap<WhirlpoolAccount, Bip84Wallet>();
    for (WhirlpoolAccount walletAccount : WhirlpoolAccount.values()) {
      IIndexHandler mainIndexHandler =
          computeIndexHandler(walletStateSupplier, walletAccount.getPersistKeyMain(), 0);
      IIndexHandler changeIndexHandler =
          computeIndexHandler(walletStateSupplier, walletAccount.getPersistKeyChange(), 0);

      Bip84Wallet wallet =
          new Bip84Wallet(
              hdWallet, walletAccount.getAccountIndex(), mainIndexHandler, changeIndexHandler);
      walletsByAccount.put(walletAccount, wallet);
    }
    this.ignoredAccounts = WhirlpoolAccount.getListByActive(false);
    this.accountsByZpub = computeAccountsByZpub(walletsByAccount);
  }

  private static WalletStateIndexHandler computeIndexHandler(
      WalletStateSupplier walletStateSupplier, String key, int defaultValue) {
    return new WalletStateIndexHandler(walletStateSupplier, key, defaultValue);
  }

  private static Map<String, WhirlpoolAccount> computeAccountsByZpub(
      Map<WhirlpoolAccount, Bip84Wallet> walletsByAccount) {
    Map<String, WhirlpoolAccount> accountsByZpub = new LinkedHashMap<String, WhirlpoolAccount>();
    for (WhirlpoolAccount account : walletsByAccount.keySet()) {
      Bip84Wallet wallet = walletsByAccount.get(account);
      String zpub = wallet.getZpub();
      accountsByZpub.put(zpub, account);
    }
    return accountsByZpub;
  }

  public Bip84Wallet getWallet(WhirlpoolAccount account) {
    Bip84Wallet wallet = walletsByAccount.get(account);
    if (wallet == null) {
      log.error("No wallet found for account: " + account);
      return null;
    }
    return wallet;
  }

  public WhirlpoolAccount getAccountByZpub(String zpub) {
    WhirlpoolAccount account = accountsByZpub.get(zpub);
    if (account == null) {
      log.error("No account found for zpub: " + zpub);
      return null;
    }
    return account;
  }

  private boolean isIgnoredAccount(WhirlpoolAccount account) {
    return ArrayUtils.contains(ignoredAccounts, account);
  }

  public String[] getZpubs(boolean withIgnoredAccounts) {
    List<String> zpubs = new LinkedList<String>();
    for (String zpub : accountsByZpub.keySet()) {
      WhirlpoolAccount account = accountsByZpub.get(zpub);
      if (withIgnoredAccounts || !isIgnoredAccount(account)) {
        zpubs.add(zpub);
      }
    }
    return zpubs.toArray(new String[] {});
  }

  public WalletStateSupplier getWalletStateSupplier() {
    return walletStateSupplier;
  }
}
