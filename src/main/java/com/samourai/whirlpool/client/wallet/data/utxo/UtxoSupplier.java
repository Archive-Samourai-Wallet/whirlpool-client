package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.AbstractSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoSupplier extends AbstractSupplier<UtxoData> {
  private static final Logger log = LoggerFactory.getLogger(UtxoSupplier.class);

  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final BackendApi backendApi;
  private final MessageListener<WhirlpoolUtxoChanges> utxoChangesListener;

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public UtxoSupplier(
      int refreshUtxoDelay,
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      BackendApi backendApi,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener) {
    super(refreshUtxoDelay, null, log);
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.backendApi = backendApi;
    this.previousUtxos = null;
    this.utxoChangesListener = utxoChangesListener;
  }

  protected List<UnspentResponse.UnspentOutput> fetchUtxos() throws Exception {
    String[] utxoZpubs = walletSupplier.getZpubs(false);
    List<UnspentResponse.UnspentOutput> utxos = backendApi.fetchUtxos(utxoZpubs);
    return utxos;
  }

  @Override
  protected UtxoData fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }

    // fetch utxos
    List<UnspentResponse.UnspentOutput> utxos = fetchUtxos();
    UtxoData utxoData = new UtxoData(walletSupplier, utxoConfigSupplier, utxos, previousUtxos);

    // notify changes
    final WhirlpoolUtxoChanges utxoChanges = utxoData.getUtxoChanges();
    if (!utxoChanges.isEmpty()) {
      // notify utxoConfigSupplier
      utxoConfigSupplier.onUtxoChanges(utxoData);

      // notify wallet
      utxoChangesListener.onMessage(utxoChanges);
    }

    // update previousUtxos
    Map<String, WhirlpoolUtxo> newPreviousUtxos = new LinkedHashMap<String, WhirlpoolUtxo>();
    newPreviousUtxos.putAll(utxoData.getUtxos());
    previousUtxos = newPreviousUtxos;
    return utxoData;
  }

  public Collection<WhirlpoolUtxo> getUtxos() {
    return getValue().getUtxos().values();
  }

  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(whirlpoolAccounts);
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
