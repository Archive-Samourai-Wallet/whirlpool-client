package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.event.UtxosChangeEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.BasicSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoSupplier extends BasicSupplier<UtxoData> {
  private static final Logger log = LoggerFactory.getLogger(UtxoSupplier.class);

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private WalletDataSupplier walletDataSupplier;

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public UtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      WalletDataSupplier walletDataSupplier) {
    super(log, null);
    this.previousUtxos = null;
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.walletDataSupplier = walletDataSupplier;
  }

  public void _setValue(WalletResponse walletResponse) {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }

    UtxoData utxoData =
        new UtxoData(
            walletSupplier, utxoConfigSupplier, walletResponse.unspent_outputs, previousUtxos);
    setValue(utxoData);
  }

  @Override
  protected void setValue(UtxoData utxoData) {
    // notify changes
    final WhirlpoolUtxoChanges utxoChanges = utxoData.getUtxoChanges();
    if (!utxoChanges.isEmpty()) {
      // notify utxoConfigSupplier
      utxoConfigSupplier.onUtxoChanges(utxoData);
    }

    // update previousUtxos
    Map<String, WhirlpoolUtxo> newPreviousUtxos = new LinkedHashMap<String, WhirlpoolUtxo>();
    newPreviousUtxos.putAll(utxoData.getUtxos());
    previousUtxos = newPreviousUtxos;

    // set new value
    super.setValue(utxoData);

    // notify
    if (!utxoChanges.isEmpty()) {
      eventService.post(new UtxosChangeEvent(utxoData));
    }
  }

  public void expire() {
    walletDataSupplier.expire();
  }

  public Collection<WhirlpoolUtxo> getUtxos() {
    return getValue().getUtxos().values();
  }

  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(false, whirlpoolAccounts);
  }

  public Collection<WhirlpoolUtxo> findUtxos(
      final boolean excludeNoPool, final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(excludeNoPool, whirlpoolAccounts);
  }

  public long getBalance(WhirlpoolAccount whirlpoolAccount) {
    return getValue().getBalance(whirlpoolAccount);
  }

  public long getBalanceTotal() {
    return getValue().getBalanceTotal();
  }

  public WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex) {
    // find by key
    WhirlpoolUtxo whirlpoolUtxo = getValue().findByUtxoKey(utxoHash, utxoIndex);
    if (whirlpoolUtxo != null) {
      return whirlpoolUtxo;
    }
    log.warn("findUtxo(" + utxoHash + ":" + utxoIndex + "): not found");
    return null;
  }
}
