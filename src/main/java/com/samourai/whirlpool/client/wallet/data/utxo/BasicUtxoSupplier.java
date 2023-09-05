package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicUtxoSupplier extends BasicSupplier<UtxoData>
    implements UtxoProvider, UtxoSupplier {
  private static final Logger log = LoggerFactory.getLogger(BasicUtxoSupplier.class);

  private final WalletSupplier walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final ChainSupplier chainSupplier;
  private final PoolSupplier poolSupplier;
  private final BipFormatSupplier bipFormatSupplier;

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public BasicUtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      ChainSupplier chainSupplier,
      PoolSupplier poolSupplier,
      BipFormatSupplier bipFormatSupplier)
      throws Exception {
    super(log);
    this.previousUtxos = null;
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.chainSupplier = chainSupplier;
    this.poolSupplier = poolSupplier;
    this.bipFormatSupplier = bipFormatSupplier;
  }

  public abstract void refresh() throws Exception;

  protected void onUtxoChanges(UtxoData utxoData) {
    // overridable
  }

  @Override
  protected void validate(UtxoData value) {
    // nothing to do
  }

  @Override
  protected void onValueChange(UtxoData value) throws Exception {
    if (!value.getUtxoChanges().isEmpty()) {
      onUtxoChanges(value);
    }
  }

  @Override
  public synchronized void setValue(UtxoData utxoData) throws Exception {
    utxoData.init(
        walletSupplier,
        utxoConfigSupplier,
        this,
        poolSupplier,
        previousUtxos,
        chainSupplier.getLatestBlock().height);

    // update previousUtxos
    Map<String, WhirlpoolUtxo> newPreviousUtxos = new LinkedHashMap<String, WhirlpoolUtxo>();
    newPreviousUtxos.putAll(utxoData.getUtxos());
    previousUtxos = newPreviousUtxos;

    // set new value
    super.setValue(utxoData);
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(whirlpoolAccounts);
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(
      final BipFormat bipFormat, final WhirlpoolAccount... whirlpoolAccounts) {
    return findUtxos(whirlpoolAccounts).stream()
        .filter(whirlpoolUtxo -> whirlpoolUtxo.getBipWallet().getBipFormatDefault() == bipFormat)
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxosByAddress(String address) {
    return getValue().findUtxosByAddress(address);
  }

  @Override
  public Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount) {
    return getValue().findTxs(whirlpoolAccount);
  }

  @Override
  public long getBalance(WhirlpoolAccount whirlpoolAccount) {
    return getValue().getBalance(whirlpoolAccount);
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
      WhirlpoolAccount account, BipFormat bipFormat, boolean increment) {
    BipWallet bipWallet = walletSupplier.getWallet(account, bipFormat);
    return bipWallet.getNextAddressChange().getAddressString();
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account, BipFormat bipFormat) {
    return toUTXOs(findUtxos(bipFormat, account));
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account) {
    return toUTXOs(findUtxos(account));
  }

  protected byte[] _getPrivKeyBip47(WhirlpoolUtxo whirlpoolUtxo) throws Exception {
    // override this to support bip47
    throw new NotifiableException("No privkey found for utxo: " + whirlpoolUtxo);
  }

  // overidden by Sparrow
  protected byte[] _getPrivKey(WhirlpoolUtxo whirlpoolUtxo) throws Exception {
    if (!whirlpoolUtxo.getUtxo().hasPath()) {
      // bip47
      return _getPrivKeyBip47(whirlpoolUtxo);
    }
    return whirlpoolUtxo.getBipAddress().getHdAddress().getECKey().getPrivKeyBytes();
  }

  @Override
  public byte[] _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new Exception("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    return _getPrivKey(whirlpoolUtxo);
  }

  @Override
  public boolean isMixableUtxo(UnspentOutput unspentOutput, BipWallet bipWallet) {
    WhirlpoolAccount whirlpoolAccount = bipWallet.getAccount();

    // don't mix BADBANK utxos
    if (WhirlpoolAccount.BADBANK.equals(whirlpoolAccount)) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring non-mixable utxo (BADBANK): " + unspentOutput);
      }
      return false;
    }

    // don't mix PREMIX/POSTMIX changes
    if (WhirlpoolAccount.PREMIX.equals(whirlpoolAccount)
        || WhirlpoolAccount.POSTMIX.equals(whirlpoolAccount)) {
      // ignore change utxos
      if (unspentOutput.xpub != null && unspentOutput.xpub.path != null) {
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
    Map<String, UTXO> utxoByScript = new LinkedHashMap<String, UTXO>();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      NetworkParameters params = whirlpoolUtxo.getBipWallet().getParams();
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

  protected WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  @Override
  public BipFormatSupplier getBipFormatSupplier() {
    return bipFormatSupplier;
  }
}
