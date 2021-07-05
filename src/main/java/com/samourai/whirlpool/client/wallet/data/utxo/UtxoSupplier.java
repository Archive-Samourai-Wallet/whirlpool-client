package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.event.UtxosChangeEvent;
import com.samourai.whirlpool.client.event.UtxosResponseEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.BasicSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletDataSupplier;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;
import java.util.*;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoSupplier extends BasicSupplier<UtxoData> implements UtxoProvider {
  private static final Logger log = LoggerFactory.getLogger(UtxoSupplier.class);

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private WalletDataSupplier walletDataSupplier;
  private NetworkParameters params;

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public UtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      WalletDataSupplier walletDataSupplier,
      NetworkParameters params) {
    super(log, null);
    this.previousUtxos = null;
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.walletDataSupplier = walletDataSupplier;
    this.params = params;
  }

  public void _setValue(WalletResponse walletResponse) {
    if (log.isDebugEnabled()) {
      log.debug("_setValue");
    }

    UtxoData utxoData =
        new UtxoData(walletSupplier, utxoConfigSupplier, walletResponse, previousUtxos);
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
    eventService.post(new UtxosResponseEvent());
    if (!utxoChanges.isEmpty()) {
      eventService.post(new UtxosChangeEvent(utxoData));
    }
  }

  public void expire() {
    walletDataSupplier.expire();
  }

  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(false, whirlpoolAccounts);
  }

  public Collection<WhirlpoolUtxo> findUtxos(
      final AddressType addressType, final WhirlpoolAccount... whirlpoolAccounts) {
    return StreamSupport.stream(findUtxos(whirlpoolAccounts))
        .filter(
            new Predicate<WhirlpoolUtxo>() {
              @Override
              public boolean test(WhirlpoolUtxo whirlpoolUtxo) {
                return whirlpoolUtxo.getAddressType() == addressType;
              }
            })
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  public Collection<WhirlpoolUtxo> findUtxos(
      final boolean excludeNoPool, final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(excludeNoPool, whirlpoolAccounts);
  }

  public Collection<WhirlpoolUtxo> findUtxosByAddress(String address) {
    return getValue().findUtxosByAddress(address);
  }

  public Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount) {
    return getValue().findTxs(whirlpoolAccount);
  }

  public long getBalance(WhirlpoolAccount whirlpoolAccount) {
    return getValue().getBalance(whirlpoolAccount);
  }

  public long getBalanceTotal() {
    return getValue().getBalanceTotal();
  }

  public WhirlpoolUtxo findUtxo(TransactionOutPoint outPoint) {
    return findUtxo(outPoint.getHash().toString(), (int) outPoint.getIndex());
  }

  public WhirlpoolUtxo findUtxo(UnspentOutput unspentOutput) {
    return findUtxo(unspentOutput.tx_hash, unspentOutput.tx_output_n);
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

  // UtxoSUpplier

  @Override
  public String getChangeAddress(WhirlpoolAccount account, AddressType addressType) {
    // TODO zeroleak revert change index
    return walletSupplier
        .getWallet(account, addressType)
        .getNextChangeAddress()
        .getAddressString(addressType);
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account, AddressType addressType) {
    return toUTXOs(findUtxos(addressType, account));
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account) {
    return toUTXOs(findUtxos(account));
  }

  @Override
  public ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new Exception("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    HD_Address premixAddress = getAddress(whirlpoolUtxo);
    return premixAddress.getECKey();
  }

  private HD_Address getAddress(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    AddressType addressType = AddressType.findByAddress(utxo.addr, params);
    return walletSupplier.getWallet(whirlpoolUtxo.getAccount(), addressType).getAddressAt(utxo);
  }

  private Collection<UTXO> toUTXOs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    // group utxos by script = same address
    Map<String, UTXO> utxoByScript = new LinkedHashMap<String, UTXO>();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      MyTransactionOutPoint outPoint = whirlpoolUtxo.getUtxo().computeOutpoint(params);
      String script = whirlpoolUtxo.getUtxo().script;

      UTXO utxo = utxoByScript.get(script);
      if (utxo == null) {
        utxo = new UTXO();
        utxo.setPath(whirlpoolUtxo.getUtxo().getPath());
        utxoByScript.put(script, utxo);
      }
      utxo.getOutpoints().add(outPoint);
      if (utxo.getOutpoints().size() > 1) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Found "
                  + utxo.getOutpoints().size()
                  + " UTXO with same address: "
                  + utxo.getOutpoints());
        }
      }
    }
    return utxoByScript.values();
  }
}
