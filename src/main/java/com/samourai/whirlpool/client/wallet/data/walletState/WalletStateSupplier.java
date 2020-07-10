package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.MultiAddrResponse;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WalletStateSupplier extends AbstractPersistableSupplier<WalletStateData> {
  private static final Logger log = LoggerFactory.getLogger(WalletStateSupplier.class);
  private static final int INIT_BIP84_RETRY = 3;
  private static final int INIT_BIP84_RETRY_TIMEOUT = 3000;

  private BackendApi backendApi;
  private boolean synced; // postmix counters sync

  protected WalletStateSupplier(
      int refreshUtxoDelay, WalletStatePersister persister, BackendApi backendApi) {
    super(refreshUtxoDelay, null, persister, log);
    this.backendApi = backendApi;
    this.synced = true;
  }

  protected abstract WalletSupplier getWalletSupplier();

  @Override
  protected WalletStateData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    String[] activeZpubs = getWalletSupplier().getZpubs(false);

    WalletStateData currentValue = getValue();
    WalletStateData newValue;
    if (currentValue == null) {
      // FIRST LOAD

      // read indexs from file
      newValue = super.fetch();

      boolean isInitialized = newValue.isInitialized();

      // initialize wallets
      if (!isInitialized) {
        for (String zpub : activeZpubs) {
          initBip84(zpub);
        }
        newValue.setInitialized();

        // when wallet is not initialized, counters are not synced
        this.synced = false;
      }
    } else {
      newValue = currentValue.copy();
    }

    // refresh indexs from wallet backend
    Map<String, MultiAddrResponse.Address> addressesByZpub = backendApi.fetchAddresses(activeZpubs);
    for (String zpub : addressesByZpub.keySet()) {
      MultiAddrResponse.Address address = addressesByZpub.get(zpub);
      WhirlpoolAccount account = getWalletSupplier().getAccountByZpub(zpub);
      if (account != null) {
        newValue.updateIndexs(account, address);
      } else {
        log.error("No account found for zpub: " + zpub);
      }
    }

    return newValue;
  }

  private void initBip84(String zpub) throws Exception {
    for (int i = 0; i < INIT_BIP84_RETRY; i++) {
      log.info(" • Initializing bip84 wallet");
      try {
        backendApi.initBip84(zpub);
        return; // success
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("", e);
        }
        log.error(
            " x Initializing bip84 wallet failed, retrying... ("
                + (i + 1)
                + "/"
                + INIT_BIP84_RETRY
                + ")");
        Thread.sleep(INIT_BIP84_RETRY_TIMEOUT);
      }
    }
    throw new NotifiableException("Unable to initialize Bip84 wallet");
  }

  protected int get(String key, int defaultValue) {
    return getValue().get(key, defaultValue);
  }

  protected int getAndIncrement(String key, int defaultValue) {
    return getValue().getAndIncrement(key, defaultValue);
  }

  protected void set(String key, int value) {
    getValue().set(key, value);
    if (log.isDebugEnabled()) {
      log.debug("set: [" + key + "]=" + value);
    }
  }

  public boolean isSynced() {
    return synced;
  }

  public void setSynced(boolean synced) {
    this.synced = synced;
  }
}
