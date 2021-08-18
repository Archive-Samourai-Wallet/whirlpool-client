package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.ExpirableSupplier;
import com.samourai.whirlpool.client.wallet.data.LoadableSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersister;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WalletDataSupplier extends ExpirableSupplier<WalletResponse>
    implements LoadableSupplier {
  private static final Logger log = LoggerFactory.getLogger(WalletDataSupplier.class);

  protected final WalletSupplier walletSupplier;
  protected final MinerFeeSupplier minerFeeSupplier;
  protected final Tx0ParamService tx0ParamService;
  protected final PoolSupplier poolSupplier;
  protected final WalletStateSupplier walletStateSupplier;

  protected final UtxoSupplier utxoSupplier;
  protected final UtxoConfigSupplier utxoConfigSupplier;

  public WalletDataSupplier(
      int refreshUtxoDelay,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
      WhirlpoolWalletConfig config,
      HD_Wallet bip84w,
      String walletIdentifier)
      throws Exception {
    super(refreshUtxoDelay, null, log);
    this.walletSupplier = computeWalletSupplier(config, bip84w, walletIdentifier);
    this.walletStateSupplier = walletSupplier.getWalletStateSupplier();

    this.minerFeeSupplier = computeMinerFeeSupplier(config);
    this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);
    this.poolSupplier = computePoolSupplier(config, tx0ParamService);

    this.utxoConfigSupplier =
        computeUtxoConfigSupplier(poolSupplier, tx0ParamService, walletIdentifier);
    this.utxoSupplier =
        computeUtxoSupplier(walletSupplier, utxoConfigSupplier, utxoChangesListener);
  }

  protected WalletSupplier computeWalletSupplier(
      WhirlpoolWalletConfig config, HD_Wallet bip84w, String walletIdentifier) throws Exception {
    int externalIndexDefault =
        config.getExternalDestination() != null
            ? config.getExternalDestination().getStartIndex()
            : 0;
    String walletStateFileName = computeFileIndex(walletIdentifier).getAbsolutePath();
    return new WalletSupplier(
        new WalletStatePersister(walletStateFileName),
        config.getBackendApi(),
        bip84w,
        externalIndexDefault);
  }

  protected MinerFeeSupplier computeMinerFeeSupplier(WhirlpoolWalletConfig config) {
    return new MinerFeeSupplier(config.getFeeMin(), config.getFeeMax(), config.getFeeFallback());
  }

  protected PoolSupplier computePoolSupplier(
      WhirlpoolWalletConfig config, Tx0ParamService tx0ParamService) {
    return new PoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
  }

  protected UtxoConfigPersister computeUtxoConfigPersister(String walletIdentifier)
      throws Exception {
    String utxoConfigFileName = computeFileUtxos(walletIdentifier).getAbsolutePath();
    return new UtxoConfigPersister(utxoConfigFileName);
  }

  protected UtxoConfigSupplier computeUtxoConfigSupplier(
      PoolSupplier poolSupplier, Tx0ParamService tx0ParamService, String walletIdentifier)
      throws Exception {
    UtxoConfigPersister utxoConfigPersister = computeUtxoConfigPersister(walletIdentifier);
    return new UtxoConfigSupplier(utxoConfigPersister, poolSupplier, tx0ParamService);
  }

  protected UtxoSupplier computeUtxoSupplier(
      WalletSupplier walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      MessageListener<WhirlpoolUtxoChanges> utxoChangesListener) {
    return new UtxoSupplier(walletSupplier, utxoConfigSupplier, this, utxoChangesListener);
  }

  protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-state-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
    String fileName = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
    return computeFile(fileName);
  }

  protected File computeFile(String fileName) throws NotifiableException {
    return ClientUtils.createFile(fileName); // use current directory
  }

  protected abstract WalletResponse fetchWalletResponse() throws Exception;

  @Override
  protected WalletResponse fetch() throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("fetching...");
    }
    WalletResponse walletResponse = fetchWalletResponse();

    // update minerFeeSupplier
    try {
      minerFeeSupplier._setValue(walletResponse);
    } catch (Exception e) {
      log.error("minerFeeSupplier._setValue failed => using fallback value", e);
    }

    // update utxoSupplier
    utxoSupplier._setValue(walletResponse);

    // update walletStateSupplier
    walletStateSupplier._setValue(walletResponse);

    return walletResponse;
  }

  // accessed by sub-suppliers
  @Override
  public WalletResponse getValue() {
    return super.getValue();
  }

  public WalletSupplier getWalletSupplier() {
    return walletSupplier;
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return minerFeeSupplier;
  }

  public Tx0ParamService getTx0ParamService() {
    return tx0ParamService;
  }

  public PoolSupplier getPoolSupplier() {
    return poolSupplier;
  }

  public UtxoSupplier getUtxoSupplier() {
    return utxoSupplier;
  }

  public UtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }
}
