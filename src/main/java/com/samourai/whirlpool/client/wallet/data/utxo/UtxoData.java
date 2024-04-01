package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoData {
  private static final Logger log = LoggerFactory.getLogger(UtxoData.class);

  private final UnspentOutput[] unspentOutputs;
  private final WalletResponse.Tx[] txs;

  // computed by init()
  private Map<String, WhirlpoolUtxo> utxos;
  private Map<String, List<WhirlpoolUtxo>> utxosByAddress;
  private Map<SamouraiAccount, List<WalletResponse.Tx>> txsByAccount;
  private WhirlpoolUtxoChanges utxoChanges;
  private Map<SamouraiAccount, Long> balanceByAccount;
  private long balanceTotal;
  private int latestBlockHeight;

  public UtxoData(UnspentOutput[] unspentOutputs, WalletResponse.Tx[] txs, int latestBlockHeight) {
    this.unspentOutputs = unspentOutputs;
    this.txs = txs;
    this.latestBlockHeight = latestBlockHeight;
  }

  protected void init(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      UtxoSupplier utxoSupplier,
      PoolSupplier poolSupplier,
      Map<String, WhirlpoolUtxo> previousUtxos) {
    // txs
    final Map<SamouraiAccount, List<WalletResponse.Tx>> freshTxs = new LinkedHashMap<>();
    for (SamouraiAccount account : SamouraiAccount.values()) {
      freshTxs.put(account, new LinkedList<>());
    }
    for (WalletResponse.Tx tx : txs) {
      Collection<SamouraiAccount> txAccounts = findTxAccounts(tx, walletSupplier);
      for (SamouraiAccount txAccount : txAccounts) {
        freshTxs.get(txAccount).add(tx);
      }
    }
    this.txsByAccount = freshTxs;

    // fresh utxos
    final Map<String, UnspentOutput> freshUtxos = new LinkedHashMap<>();
    for (UnspentOutput utxo : unspentOutputs) {
      String utxoKey = ClientUtils.utxoToKey(utxo);
      freshUtxos.put(utxoKey, utxo);
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
      String key = ClientUtils.utxoToKey(whirlpoolUtxo.getUtxo());

      UnspentOutput freshUtxo = freshUtxos.get(key);
      if (freshUtxo != null) {
        // set blockHeight when confirmed
        if (whirlpoolUtxo.getBlockHeight() == null && freshUtxo.confirmations > 0) {
          int blockHeight =
              ClientUtils.computeBlockHeight(freshUtxo.confirmations, latestBlockHeight);
          whirlpoolUtxo.setUtxoConfirmed(freshUtxo, blockHeight, latestBlockHeight);
          utxoChanges.getUtxosConfirmed().add(whirlpoolUtxo);
        }
        // update utxo.confirmations (not really needed)
        whirlpoolUtxo.getUtxo().confirmations = freshUtxo.confirmations;
        // add
        addUtxo(whirlpoolUtxo);
      } else {
        // obsolete
        utxoChanges.getUtxosRemoved().add(whirlpoolUtxo);
      }
    }

    // add missing utxos
    for (Map.Entry<String, UnspentOutput> e : freshUtxos.entrySet()) {
      String key = e.getKey();
      if (!previousUtxos.containsKey(key)) {
        UnspentOutput utxo = e.getValue();
        try {
          WhirlpoolUtxo whirlpoolUtxo = utxoSupplier.computeWhirlpoolUtxo(utxo, latestBlockHeight);

          // auto-assign pool for mixable utxos & mixableStatus
          String poolId = null;
          if (utxoSupplier.isMixableUtxo(
              utxo, whirlpoolUtxo.getAccount())) { //  exclude premix/postmix change
            poolId = computeAutoAssignPoolId(whirlpoolUtxo.getAccount(), utxo.value, poolSupplier);
          }

          whirlpoolUtxo.setPoolIdAndMixableStatus(poolId, latestBlockHeight);

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
    this.balanceByAccount = new LinkedHashMap<>();
    long total = 0;
    for (SamouraiAccount account : SamouraiAccount.values()) {
      Collection<WhirlpoolUtxo> utxosForAccount = findUtxos(account);
      long balance = WhirlpoolUtxo.sumValue(utxosForAccount);
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
      SamouraiAccount account, long value, PoolSupplier poolSupplier) {
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
    String key = ClientUtils.utxoToKey(whirlpoolUtxo.getUtxo());
    utxos.put(key, whirlpoolUtxo);

    String addr = whirlpoolUtxo.getUtxo().addr;
    if (utxosByAddress.get(addr) == null) {
      utxosByAddress.put(addr, new LinkedList<WhirlpoolUtxo>());
    }
    utxosByAddress.get(addr).add(whirlpoolUtxo);
  }

  private Collection<SamouraiAccount> findTxAccounts(
      WalletResponse.Tx tx, WalletSupplier walletSupplier) {
    Set<SamouraiAccount> accounts = new LinkedHashSet<SamouraiAccount>();
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

  public Collection<WalletResponse.Tx> findTxs(SamouraiAccount samouraiAccount) {
    return txsByAccount.get(samouraiAccount);
  }

  public WhirlpoolUtxoChanges getUtxoChanges() {
    return utxoChanges;
  }

  public WhirlpoolUtxo findByUtxoKey(String utxoHash, int utxoIndex) {
    String utxoKey = ClientUtils.utxoToKey(utxoHash, utxoIndex);
    return utxos.get(utxoKey);
  }

  public Collection<WhirlpoolUtxo> findUtxos(final SamouraiAccount... samouraiAccounts) {
    return utxos.values().stream()
        .filter(
            whirlpoolUtxo -> {
              if (!ArrayUtils.contains(samouraiAccounts, whirlpoolUtxo.getAccount())) {
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

  public long getBalance(SamouraiAccount account) {
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
