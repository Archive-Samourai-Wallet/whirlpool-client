package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicUtxoSupplier extends BasicSupplier<UtxoData>
    implements UtxoProvider, UtxoSupplier {
  private static final Logger log = LoggerFactory.getLogger(BasicUtxoSupplier.class);

  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final DataSourceConfig dataSourceConfig;
  private final NetworkParameters params;
  private CoordinatorSupplier coordinatorSupplier;
  private Consumer<UtxoData> utxoChangesListener; // may be null

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public BasicUtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      DataSourceConfig dataSourceConfig,
      NetworkParameters params) {
    super(log);
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.dataSourceConfig = dataSourceConfig;
    this.params = params;
    this.coordinatorSupplier = null; // will be set by init()
    this.utxoChangesListener = null;
    this.previousUtxos = null;
  }

  @Override
  public void _setCoordinatorSupplier(CoordinatorSupplier coordinatorSupplier) {
    this.coordinatorSupplier = coordinatorSupplier;
  }

  @Override
  public void _setUtxoChangeListener(Consumer<UtxoData> utxoChangesListener) {
    this.utxoChangesListener = utxoChangesListener;
  }

  public abstract void refresh() throws Exception;

  @Override
  protected void validate(UtxoData value) {
    // nothing to do
  }

  @Override
  protected void onValueChange(UtxoData value) throws Exception {
    if (!value.getUtxoChanges().isEmpty()) {
      if (utxoChangesListener != null) {
        utxoChangesListener.accept(value);
      }
    }
  }

  @Override
  public WhirlpoolUtxo computeWhirlpoolUtxo(UnspentOutput utxo, int latestBlockHeight)
      throws Exception {
    // find account
    SamouraiAccount samouraiAccount;
    String path;
    ECKey ecKey;
    String xpub = (utxo.xpub != null ? utxo.xpub.m : null);
    if (utxo.hasPath()) {
      // HD_WALLET
      BipWallet bipWallet = walletSupplier.getWalletByXPub(xpub);
      if (bipWallet == null) {
        throw new Exception("Unknown wallet for: " + xpub);
      }
      samouraiAccount = bipWallet.getAccount();
      path = bipWallet.getDerivation().getPathAddress(utxo, params);
      ecKey = bipWallet.getAddressAt(utxo).getHdAddress().getECKey();
    } else {
      // BIP47
      samouraiAccount = SamouraiAccount.DEPOSIT;
      path = null;
      ecKey = ECKey.fromPrivate(_getPrivKeyBip47(utxo));
    }

    // add missing
    BipFormat bipFormat = getBipFormatSupplier().findByAddress(utxo.addr, params);
    Integer blockHeight = ClientUtils.computeBlockHeight(utxo.confirmations, latestBlockHeight);
    return new WhirlpoolUtxo(
        utxo, samouraiAccount, ecKey, path, params, bipFormat, utxoConfigSupplier, blockHeight);
  }

  @Override
  public synchronized void setValue(UtxoData utxoData) throws Exception {
    utxoData.init(walletSupplier, utxoConfigSupplier, this, coordinatorSupplier, previousUtxos);

    // update previousUtxos
    Map<String, WhirlpoolUtxo> newPreviousUtxos = new LinkedHashMap<String, WhirlpoolUtxo>();
    newPreviousUtxos.putAll(utxoData.getUtxos());
    previousUtxos = newPreviousUtxos;

    // set new value
    super.setValue(utxoData);
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(final SamouraiAccount... samouraiAccounts) {
    return getValue().findUtxos(samouraiAccounts);
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(
      final BipFormat bipFormat, final SamouraiAccount... samouraiAccounts) {
    return findUtxos(samouraiAccounts).stream()
        .filter(whirlpoolUtxo -> whirlpoolUtxo.getBipFormat() == bipFormat)
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxosByAddress(String address) {
    return getValue().findUtxosByAddress(address);
  }

  @Override
  public Collection<WalletResponse.Tx> findTxs(SamouraiAccount samouraiAccount) {
    return getValue().findTxs(samouraiAccount);
  }

  @Override
  public long getBalance(SamouraiAccount samouraiAccount) {
    return getValue().getBalance(samouraiAccount);
  }

  @Override
  public long getBalanceTotal() {
    return getValue().getBalanceTotal();
  }

  @Override
  public WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex) {
    // find by key
    WhirlpoolUtxo whirlpoolUtxo = getValue().findByUtxoKey(utxoHash, utxoIndex);
    if (whirlpoolUtxo != null) {
      return whirlpoolUtxo;
    }
    log.warn("findUtxo(" + utxoHash + ":" + utxoIndex + "): not found");
    return null;
  }

  // UtxoSupplier

  @Override
  public String getNextAddressChange(
      SamouraiAccount account, BipFormat bipFormat, boolean increment) {
    BipWallet bipWallet = walletSupplier.getWallet(account, bipFormat);
    return bipWallet.getNextAddressChange().getAddressString();
  }

  @Override
  public Collection<UTXO> getUtxos(SamouraiAccount account, BipFormat bipFormat) {
    return toUTXOs(findUtxos(bipFormat, account));
  }

  @Override
  public Collection<UTXO> getUtxos(SamouraiAccount account) {
    return toUTXOs(findUtxos(account));
  }

  @Override
  public byte[] _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new Exception("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    return whirlpoolUtxo.getECKey().getPrivKeyBytes();
  }

  @Override
  public byte[] _getPrivKeyBip47(UnspentOutput unspentOutput) throws Exception {
    throw new Exception("_getPrivKeyBip47() is not implemented yet");
  }

  @Override
  public boolean isMixableUtxo(UnspentOutput unspentOutput, SamouraiAccount samouraiAccount) {
    // don't mix BADBANK utxos
    if (SamouraiAccount.BADBANK.equals(samouraiAccount)) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring non-mixable utxo (BADBANK): " + unspentOutput);
      }
      return false;
    }

    // don't mix PREMIX/POSTMIX changes
    if (SamouraiAccount.PREMIX.equals(samouraiAccount)
        || SamouraiAccount.POSTMIX.equals(samouraiAccount)) {
      // ignore change utxos
      if (unspentOutput.getPath() != null) {
        int chainIndex = unspentOutput.computePathChainIndex();
        if (chainIndex == Chain.CHANGE.getIndex()) {
          if (log.isDebugEnabled()) {
            log.debug("Ignoring non-mixable utxo (PREMIX/POSTMIX change): " + unspentOutput);
          }
          return false;
        }
      }
    }
    return true;
  }

  private Collection<UTXO> toUTXOs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    // group utxos by script = same address
    Map<String, UTXO> utxoByScript = new LinkedHashMap<>();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      MyTransactionOutPoint outPoint = whirlpoolUtxo.getUtxo().computeOutpoint(params);
      String script = whirlpoolUtxo.getUtxo().script;

      UTXO utxo = utxoByScript.get(script);
      if (utxo == null) {
        utxo = new UTXO(whirlpoolUtxo.getUtxo().getPath(), whirlpoolUtxo.getUtxo().xpub.m);
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

  protected WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  @Override
  public BipFormatSupplier getBipFormatSupplier() {
    return dataSourceConfig.getBipFormatSupplier();
  }

  public NetworkParameters getParams() {
    return params;
  }
}
