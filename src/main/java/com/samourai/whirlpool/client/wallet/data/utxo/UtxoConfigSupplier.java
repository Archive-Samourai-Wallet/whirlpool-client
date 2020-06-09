package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.AbstractPersistableSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtxoConfigSupplier extends AbstractPersistableSupplier<UtxoConfigData> {
  private static final Logger log = LoggerFactory.getLogger(UtxoConfigSupplier.class);

  private final PoolSupplier poolSupplier;
  private final Tx0ParamService tx0ParamService;

  public UtxoConfigSupplier(
      UtxoConfigPersister persister, PoolSupplier poolSupplier, Tx0ParamService tx0ParamService) {
    super(null, null, persister, log);
    this.poolSupplier = poolSupplier;
    this.tx0ParamService = tx0ParamService;
  }

  private String computeAutoAssignPoolId(WhirlpoolUtxo whirlpoolUtxo) {
    Collection<Pool> eligiblePools = null;

    // find eligible pools for tx0
    if (WhirlpoolAccount.DEPOSIT.equals(whirlpoolUtxo.getAccount())) {
      Collection<Pool> pools = poolSupplier.getPools();
      eligiblePools = tx0ParamService.findPools(pools, whirlpoolUtxo.getUtxo().value);
    }

    // find eligible pools for mix
    else if (WhirlpoolAccount.PREMIX.equals(whirlpoolUtxo.getAccount())
        || WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount())) {
      boolean liquidity = WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount());
      eligiblePools = poolSupplier.findPoolsForPremix(whirlpoolUtxo.getUtxo().value, liquidity);
    }

    // auto-assign pool by preference when found
    if (eligiblePools != null && !eligiblePools.isEmpty()) {
      return eligiblePools.iterator().next().getPoolId();
    }
    return null; // no pool found
  }

  private UtxoConfigPersisted newUtxoConfig(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentResponse.UnspentOutput utxo = whirlpoolUtxo.getUtxo();

    // find by tx hash (new PREMIX from TX0)
    String tx0Key = computeUtxoConfigKey(utxo.tx_hash);
    UtxoConfigPersisted utxoConfigByHash = getValue().getUtxoConfig(tx0Key);
    UtxoConfigPersisted utxoConfig =
        utxoConfigByHash != null ? utxoConfigByHash.copy() : new UtxoConfigPersisted();

    // set mixsDone
    if (WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount())) {
      utxoConfig.incrementMixsDone();
    }

    // check pool
    if (utxoConfig.getPoolId() != null) {
      // check pool applicable
      Pool pool = poolSupplier.findPoolById(utxoConfig.getPoolId());
      if (pool == null) {
        log.warn("pool not found for utxoConfig: " + utxoConfig.getPoolId());
      } else if (!tx0ParamService.isPoolApplicable(pool, whirlpoolUtxo)) {
        if (log.isDebugEnabled()) {
          log.debug("pool not applicable for utxo: " + whirlpoolUtxo);
        }
        pool = null;
      }
      if (pool == null) {
        // clear pool configuration
        utxoConfig.setPoolId(null);
      }
    }

    // auto-assign pool when possible
    if (utxoConfig.getPoolId() == null) {
      String poolId = computeAutoAssignPoolId(whirlpoolUtxo);
      if (poolId != null) {
        utxoConfig.setPoolId(poolId);
      }
    }

    // log
    return utxoConfig;
  }

  public UtxoConfigPersisted getUtxoConfigPersisted(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentResponse.UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    String key = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);

    // get existing utxoConfig
    UtxoConfigPersisted utxoConfigPersisted = getValue().getUtxoConfig(key);
    return utxoConfigPersisted;
  }

  public void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String hash, int index) {
    UnspentResponse.UnspentOutput utxo = fromUtxo.getUtxo();
    String fromKey = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
    String toKey = computeUtxoConfigKey(hash, index);
    forwardUtxoConfig(fromKey, toKey);
  }

  public void forwardUtxoConfig(WhirlpoolUtxo fromUtxo, String txid) {
    UnspentResponse.UnspentOutput utxo = fromUtxo.getUtxo();
    String fromKey = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
    String toKey = computeUtxoConfigKey(txid);
    forwardUtxoConfig(fromKey, toKey);
  }

  private void forwardUtxoConfig(String fromKey, String toKey) {
    UtxoConfigData data = getValue();
    UtxoConfigPersisted fromUtxoConfig = data.getUtxoConfig(fromKey);
    if (fromUtxoConfig != null) {
      if (log.isDebugEnabled()) {
        log.debug("forwardUtxoConfig: " + fromKey + " -> " + toKey);
      }
      UtxoConfigPersisted newUtxoConfig = fromUtxoConfig.copy();
      data.add(toKey, newUtxoConfig);
    } else {
      log.warn("forwardUtxoConfig failed: no utxoConfig found for " + fromKey);
    }
  }

  public void setLastChange() {
    getValue().setLastChange();
  }

  public String computeUtxoConfigKey(String hash, int index) {
    return ClientUtils.sha256Hash(ClientUtils.utxoToKey(hash, index));
  }

  private String computeUtxoConfigKey(String utxoHash) {
    return ClientUtils.sha256Hash(utxoHash);
  }

  protected synchronized void onUtxoChanges(UtxoData utxoData) {
    WhirlpoolUtxoChanges utxoChanges = utxoData.getUtxoChanges();

    // create new utxoConfigs
    int nbCreated = createUtxoConfigPersisted(utxoChanges.getUtxosAdded());

    // cleanup utxoConfigs
    int nbCleaned = cleanup(utxoData);

    if (log.isDebugEnabled()) {
      log.debug(
          "utxoConfig: "
              + nbCreated
              + " created, "
              + nbCleaned
              + " cleaned, "
              + getValue().toString()
              + " for "
              + utxoData);
    }
  }

  private int createUtxoConfigPersisted(List<WhirlpoolUtxo> whirlpoolUtxos) {
    int nbCreated = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      UnspentResponse.UnspentOutput utxo = whirlpoolUtxo.getUtxo();
      String key = computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);

      // create new utxoConfig when missing
      UtxoConfigPersisted utxoConfigPersisted = getValue().getUtxoConfig(key);
      if (utxoConfigPersisted == null) {
        utxoConfigPersisted = newUtxoConfig(whirlpoolUtxo);
        getValue().add(key, utxoConfigPersisted);
        nbCreated++;
      }
    }
    return nbCreated;
  }

  private int cleanup(UtxoData utxoData) {
    int nbCleaned = 0;
    if (!utxoData.getUtxos().isEmpty()
        && (utxoData.getUtxoChanges().isFirstFetch()
            || utxoData.getUtxoChanges().getUtxosRemoved().size() > 0)) {
      List<String> validKeys =
          StreamSupport.stream(utxoData.getUtxos().values())
              .map(
                  new Function<WhirlpoolUtxo, String>() {
                    @Override
                    public String apply(WhirlpoolUtxo whirlpoolUtxo) {
                      UnspentResponse.UnspentOutput utxo = whirlpoolUtxo.getUtxo();
                      return computeUtxoConfigKey(utxo.tx_hash, utxo.tx_output_n);
                    }
                  })
              .collect(Collectors.<String>toList());
      nbCleaned = getValue().cleanup(validKeys);
    }
    return nbCleaned;
  }
}