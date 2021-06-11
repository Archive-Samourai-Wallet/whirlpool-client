package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletStateSupplier extends AbstractPersistableSupplier<WalletStateData> {
  private static final Logger log = LoggerFactory.getLogger(WalletStateSupplier.class);
  private static final int INITWALLET_RETRY = 3;
  private static final int INIT_BIP84_RETRY_TIMEOUT = 3000;

  private BackendApi backendApi;
  private WalletSupplier walletSupplier;
  private boolean synced; // postmix counters sync

  public WalletStateSupplier(
      WalletStatePersister persister, BackendApi backendApi, WalletSupplier walletSupplier) {
    super(null, persister, log);
    this.backendApi = backendApi;
    this.walletSupplier = walletSupplier;
    this.synced = true;
  }

  public void _setValue(WalletResponse walletResponse) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }

    WalletStateData currentValue = getValue();
    if (currentValue == null) {
      // should never happen
      throw new Exception("Cannot _setValue(), no value loaded yet!");
    }

    // update indexs from wallet backend
    Map<String, WalletResponse.Address> addressesMap = walletResponse.getAddressesMap();
    for (String pub : addressesMap.keySet()) {
      WalletResponse.Address address = addressesMap.get(pub);
      BipWalletAndAddressType bipWallet = walletSupplier.getWalletByPub(pub);
      if (bipWallet != null) {
        // important: udpate indexs directly on currentValue to prevent index reuse
        String persistKeyReceive =
            computePersistKey(bipWallet.getAccount(), bipWallet.getAddressType(), Chain.RECEIVE);
        String persistKeyChange =
            computePersistKey(bipWallet.getAccount(), bipWallet.getAddressType(), Chain.CHANGE);
        currentValue.updateIndexs(address, persistKeyReceive, persistKeyChange);
      } else {
        log.error("No wallet found for: " + pub);
      }
    }
  }

  protected String computePersistKey(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    return account.name() + "_" + addressType.getPurpose() + "_" + chain.getIndex();
  }

  @Override
  public void load() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("load()");
    }
    WalletStateData currentValue = getValue();
    WalletStateData newValue;
    if (currentValue != null) {
      throw new Exception("Cannot load(), value already loaded!");
    }

    // FIRST LOAD
    // read indexs from file
    newValue = super.getPersistedValue();

    boolean isInitialized = newValue.isInitialized();

    // initialize wallets
    if (!isInitialized) {
      String[] activePubs = walletSupplier.getPubs(false);
      for (String pub : activePubs) {
        initWallet(pub);
      }
      newValue.setInitialized();

      // when wallet is not initialized, counters are not synced
      this.synced = false;
    }
    setValue(newValue);
  }

  private void initWallet(String pub) throws Exception {
    for (int i = 0; i < INITWALLET_RETRY; i++) {
      log.info(" • Initializing wallet");
      try {
        // backendApi.initBip84(zpub); // TODO zeroleak
        return; // success
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("", e);
        }
        log.error(
            " x Initializing wallet failed, retrying... ("
                + (i + 1)
                + "/"
                + INITWALLET_RETRY
                + ")");
        Thread.sleep(INIT_BIP84_RETRY_TIMEOUT);
      }
    }
    throw new NotifiableException("Unable to initialize Bip84 wallet");
  }

  public IIndexHandler computeIndexHandler(
      WhirlpoolAccount account, AddressType addressType, Chain chain) {
    String persistKey = computePersistKey(account, addressType, chain);
    return new WalletStateIndexHandler(this, persistKey, 0);
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
