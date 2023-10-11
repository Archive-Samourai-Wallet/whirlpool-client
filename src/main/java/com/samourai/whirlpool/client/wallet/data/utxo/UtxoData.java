package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoConfirmInfo;
import com.samourai.wallet.utxo.UtxoConfirmInfoImpl;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoData {
  private static final Logger log = LoggerFactory.getLogger(UtxoData.class);
  private static final UtxoUtil utxoUtil = UtxoUtil.getInstance();

  private final BipUtxo[] unspentOutputs;
  private final WalletResponse.Tx[] txs;

  // computed by init()
  private Map<String, WhirlpoolUtxo> utxos;
  private Map<String, List<WhirlpoolUtxo>> utxosByAddress;
  private Map<WhirlpoolAccount, List<WalletResponse.Tx>> txsByAccount;
  private WhirlpoolUtxoChanges utxoChanges;
  private Map<WhirlpoolAccount, Long> balanceByAccount;
  private long balanceTotal;

  public UtxoData(BipUtxo[] unspentOutputs, WalletResponse.Tx[] txs) {
    this.unspentOutputs = unspentOutputs;
    this.txs = txs;
  }

  protected void init(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      UtxoSupplier utxoSupplier,
      PoolSupplier poolSupplier,
      Map<String, WhirlpoolUtxo> previousUtxos,
      int latestBlockHeight) {
    // txs
    final Map<WhirlpoolAccount, List<WalletResponse.Tx>> freshTxs =
        new LinkedHashMap<WhirlpoolAccount, List<WalletResponse.Tx>>();
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      freshTxs.put(account, new LinkedList<WalletResponse.Tx>());
    }
    for (WalletResponse.Tx tx : txs) {
      Collection<WhirlpoolAccount> txAccounts = findTxAccounts(tx, walletSupplier);
      for (WhirlpoolAccount txAccount : txAccounts) {
        freshTxs.get(txAccount).add(tx);
      }
    }
    this.txsByAccount = freshTxs;

    // fresh utxos
    final Map<String, BipUtxo> freshUtxos = new LinkedHashMap<>();
    for (BipUtxo utxo : unspentOutputs) {
      String utxoKey = utxoUtil.utxoToKey(utxo);
      freshUtxos.put(utxoKey, utxo);

      // init 'confirmedBlockHeight' (from 'confirmations') when missing (required for
      // UnspentOutput)
      UtxoConfirmInfo confirmInfo = utxo.getConfirmInfo();
      int confirmations = confirmInfo.getConfirmations(latestBlockHeight);
      if (confirmInfo.getConfirmedBlockHeight() == null && confirmations > 0) {
        Integer confirmBlockHeight =
            utxoUtil.computeConfirmedBlockHeight(confirmations, latestBlockHeight);
        utxo.setConfirmInfo(new UtxoConfirmInfoImpl(confirmBlockHeight));
      }
    }

    // replace utxos
    boolean isFirstFetch = false;
    if (previousUtxos == null) {
      previousUtxos = new LinkedHashMap<>();
      isFirstFetch = true;
    }

    this.utxos = new LinkedHashMap<>();
    this.utxosByAddress = new LinkedHashMap<>();
    this.utxoChanges = new WhirlpoolUtxoChanges(isFirstFetch);

    // add existing utxos
    for (WhirlpoolUtxo whirlpoolUtxo : previousUtxos.values()) {
      String key = utxoUtil.utxoToKey(whirlpoolUtxo);

      BipUtxo freshUtxo = freshUtxos.get(key);
      if (freshUtxo != null) {
        // set blockHeight when confirmed
        Integer freshConfirmedBlockHeight = freshUtxo.getConfirmInfo().getConfirmedBlockHeight();
        if (whirlpoolUtxo.getConfirmInfo().getConfirmedBlockHeight() == null
            && freshConfirmedBlockHeight != null) {
          whirlpoolUtxo.setConfirmInfo(new UtxoConfirmInfoImpl(freshConfirmedBlockHeight));
          utxoChanges.getUtxosConfirmed().add(whirlpoolUtxo);
        }
        // add
        addUtxo(whirlpoolUtxo);
      } else {
        // obsolete
        utxoChanges.getUtxosRemoved().add(whirlpoolUtxo);
      }
    }

    // add missing utxos
    for (Map.Entry<String, BipUtxo> e : freshUtxos.entrySet()) {
      String key = e.getKey();
      if (!previousUtxos.containsKey(key)) {
        BipUtxo utxo = e.getValue();
        try {
          // find account
          BipWallet bipWallet = utxo.getBipWallet(walletSupplier);
          if (bipWallet == null) {
            throw new Exception("Unknown wallet for utxo: " + utxoUtil.utxoToKey(utxo));
          }
          WhirlpoolAccount whirlpoolAccount = bipWallet.getAccount();

          // auto-assign pool for mixable utxos
          String poolId = null;
          if (utxoSupplier.isMixableUtxo(utxo, bipWallet)) { //  exclude premix/postmix change
            poolId = computeAutoAssignPoolId(whirlpoolAccount, utxo.getValueLong(), poolSupplier);
          }

          // add missing
          NetworkParameters params = bipWallet.getParams();
          BipFormat bipFormat =
              utxoSupplier.getBipFormatSupplier().findByAddress(utxo.getAddress(), params);
          WhirlpoolUtxo whirlpoolUtxo =
              new WhirlpoolUtxo(utxo, bipWallet, bipFormat, poolId, utxoConfigSupplier);
          if (!isFirstFetch) {
            // set lastActivity when utxo is detected but ignore on first fetch
            whirlpoolUtxo.getUtxoState().setLastActivity();
            if (log.isDebugEnabled()) {
              log.debug("+utxo: " + whirlpoolUtxo);
            }
          }
          utxoChanges.getUtxosAdded().add(whirlpoolUtxo);
          addUtxo(whirlpoolUtxo);
        } catch (Exception ee) {
          log.error("error loading new utxo: " + utxo, ee);
        }
      }
    }

    // compute balances
    this.balanceByAccount = new LinkedHashMap<WhirlpoolAccount, Long>();
    long total = 0;
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      Collection<WhirlpoolUtxo> utxosForAccount = findUtxos(account);
      long balance = BipUtxo.sumValue(utxosForAccount);
      balanceByAccount.put(account, balance);
      total += balance;
    }
    this.balanceTotal = total;

    if (log.isDebugEnabled()) {
      log.debug("utxos: " + previousUtxos.size() + " => " + utxos.size() + ", " + utxoChanges);
    }

    // cleanup utxoConfigs
    if (!utxoChanges.isEmpty()) {
      if (!utxos.isEmpty() && utxoChanges.getUtxosRemoved().size() > 0) {
        utxoConfigSupplier.clean(utxos.values());
      }
    }
  }

  private String computeAutoAssignPoolId(
      WhirlpoolAccount account, long value, PoolSupplier poolSupplier) {
    Collection<Pool> eligiblePools = new LinkedList<Pool>();

    // find eligible pools for tx0/premix/postmix
    switch (account) {
      case DEPOSIT:
        eligiblePools = poolSupplier.findPoolsForTx0(value);
        break;

      case PREMIX:
        eligiblePools = poolSupplier.findPoolsForPremix(value, false);
        break;

      case POSTMIX:
        eligiblePools = poolSupplier.findPoolsForPremix(value, true);
        break;
    }

    // auto-assign pool by preference when found
    if (!eligiblePools.isEmpty()) {
      return eligiblePools.iterator().next().getPoolId();
    }
    return null; // no pool found
  }

  private void addUtxo(WhirlpoolUtxo whirlpoolUtxo) {
    String key = utxoUtil.utxoToKey(whirlpoolUtxo);
    utxos.put(key, whirlpoolUtxo);

    String addr = whirlpoolUtxo.getAddress();
    if (utxosByAddress.get(addr) == null) {
      utxosByAddress.put(addr, new LinkedList<>());
    }
    utxosByAddress.get(addr).add(whirlpoolUtxo);
  }

  private Collection<WhirlpoolAccount> findTxAccounts(
      WalletResponse.Tx tx, WalletSupplier walletSupplier) {
    Set<WhirlpoolAccount> accounts = new LinkedHashSet<WhirlpoolAccount>();
    // verify inputs
    for (WalletResponse.TxInput input : tx.inputs) {
      if (input.prev_out != null) {
        BipWallet bipWallet = walletSupplier.getWalletByXPub(input.prev_out.xpub.m);
        if (bipWallet != null) {
          accounts.add(bipWallet.getAccount());
        }
      }
    }
    // verify outputs
    for (WalletResponse.TxOutput output : tx.out) {
      BipWallet bipWallet = walletSupplier.getWalletByXPub(output.xpub.m);
      if (bipWallet != null) {
        accounts.add(bipWallet.getAccount());
      }
    }
    return accounts;
  }

  // utxos

  public Map<String, WhirlpoolUtxo> getUtxos() {
    return utxos;
  }

  public Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount) {
    return txsByAccount.get(whirlpoolAccount);
  }

  public WhirlpoolUtxoChanges getUtxoChanges() {
    return utxoChanges;
  }

  public WhirlpoolUtxo findByUtxoKey(String utxoHash, int utxoIndex) {
    String utxoKey = utxoUtil.utxoToKey(utxoHash, utxoIndex);
    return utxos.get(utxoKey);
  }

  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return utxos.values().stream()
        .filter(
            whirlpoolUtxo -> {
              if (!ArrayUtils.contains(whirlpoolAccounts, whirlpoolUtxo.getAccount())) {
                return false;
              }
              return true;
            })
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  public Collection<WhirlpoolUtxo> findUtxosByAddress(String address) {
    Collection<WhirlpoolUtxo> result = utxosByAddress.get(address);
    if (result == null) {
      result = new LinkedList<>();
    }
    return result;
  }

  // balances

  public long getBalance(WhirlpoolAccount account) {
    return balanceByAccount.get(account);
  }

  public long getBalanceTotal() {
    return balanceTotal;
  }

  @Override
  public String toString() {
    return utxos.size() + " utxos";
  }
}
