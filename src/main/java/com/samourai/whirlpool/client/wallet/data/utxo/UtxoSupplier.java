package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UtxoProvider;
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
import java8.util.function.Function;
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
      final boolean excludeNoPool, final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(excludeNoPool, whirlpoolAccounts);
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
    if (addressType == AddressType.SEGWIT_NATIVE) {
      return toUTXOs(findUtxos(account));
    }
    return new LinkedList<UTXO>(); // TODO zeroleak
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account) {
    return toUTXOs(findUtxos(account));
  }

  @Override
  public ECKey _getPrivKey(TransactionOutPoint outPoint, WhirlpoolAccount account)
      throws Exception {
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(outPoint);
    if (whirlpoolUtxo == null) {
      throw new Exception("Utxo not found: " + outPoint.toString());
    }
    HD_Address premixAddress = walletSupplier.getAddressAt(account, whirlpoolUtxo.getUtxo());
    return premixAddress.getECKey();
  }

  private Collection<UTXO> toUTXOs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    return StreamSupport.stream(whirlpoolUtxos)
        .map(
            new Function<WhirlpoolUtxo, UTXO>() {
              @Override
              public UTXO apply(WhirlpoolUtxo whirlpoolUtxo) {
                UTXO utxo = new UTXO();
                List<MyTransactionOutPoint> outs = new ArrayList<MyTransactionOutPoint>();
                outs.add(new MyTransactionOutPoint(params, whirlpoolUtxo.getUtxo()));
                utxo.setOutpoints(outs);
                return utxo;
              }
            })
        .collect(Collectors.<UTXO>toList());
  }
}
