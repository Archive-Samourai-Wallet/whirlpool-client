package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.utxo.*;
import com.samourai.whirlpool.client.wallet.data.walletState.PersistableWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStatePersister;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataPersister based on filesystem. */
public class FileDataPersister implements DataPersister {
  private static final Logger log = LoggerFactory.getLogger(FileDataPersister.class);

  private AbstractOrchestrator persistOrchestrator;

  private final WhirlpoolWalletConfig config;

  private final PersistableWalletStateSupplier walletStateSupplier;
  private final PersistableUtxoConfigSupplier utxoConfigSupplier;

  public FileDataPersister(WhirlpoolWalletConfig config, HD_Wallet bip44w, String walletIdentifier)
      throws Exception {
    this.config = config;

    this.walletStateSupplier = computeWalletStateSupplier(config, walletIdentifier);
    this.utxoConfigSupplier = computeUtxoConfigSupplier(walletIdentifier);
  }

  protected PersistableWalletStateSupplier computeWalletStateSupplier(
      WhirlpoolWalletConfig config, String walletIdentifier) throws Exception {
    String walletStateFileName = computeFileIndex(walletIdentifier).getAbsolutePath();
    WalletStatePersister persister = new WalletStatePersister(walletStateFileName);
    int externalIndexDefault =
        config.getExternalDestination() != null
            ? config.getExternalDestination().getStartIndex()
            : 0;
    return new PersistableWalletStateSupplier(persister, externalIndexDefault);
  }

  protected UtxoConfigPersister computeUtxoConfigPersister(String walletIdentifier)
      throws Exception {
    String utxoConfigFileName = computeFileUtxos(walletIdentifier).getAbsolutePath();
    return new UtxoConfigPersister(utxoConfigFileName);
  }

  protected PersistableUtxoConfigSupplier computeUtxoConfigSupplier(String walletIdentifier)
      throws Exception {
    UtxoConfigPersister utxoConfigPersister = computeUtxoConfigPersister(walletIdentifier);
    return new PersistableUtxoConfigSupplier(utxoConfigPersister);
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

  protected void load() throws Exception {
    // load persistance once
    utxoConfigSupplier.load();
    walletStateSupplier.load();
  }

  @Override
  public void open() throws Exception {
    // load initial data (or fail)
    load();

    // forced persist initial data (or fail)
    persistData(true);

    // persist orchestrator
    runPersistOrchestrator();
  }

  protected void runPersistOrchestrator() {
    int persistLoopDelay = 10; // persist check each 10s
    persistOrchestrator =
        new AbstractOrchestrator(persistLoopDelay * 1000) {
          @Override
          protected void runOrchestrator() {
            try {
              persistData(false);
            } catch (Exception e) {
              log.error("", e);
            }
          }
        };
    persistOrchestrator.start(true);
  }

  @Override
  public void close() throws Exception {
    // persist before exit
    try {
      persistData(false);
    } catch (Exception e) {
      log.error("", e);
    }

    persistOrchestrator.stop();
  }

  protected void persistData(boolean force) throws Exception {
    utxoConfigSupplier.persist(force);
    walletStateSupplier.persist(force);
  }

  protected WhirlpoolWalletConfig getConfig() {
    return config;
  }

  @Override
  public WalletStateSupplier getWalletStateSupplier() {
    return walletStateSupplier;
  }

  @Override
  public PersistableUtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }
}
