package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.KeyBag;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.DebugUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletAggregateService {
  private Logger log = LoggerFactory.getLogger(WalletAggregateService.class);
  private static final int AGGREGATED_UTXOS_PER_TX = 200;
  private static final FormatsUtilGeneric formatUtils = FormatsUtilGeneric.getInstance();
  private final UtxoUtil utxoUtil = UtxoUtil.getInstance();
  private Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  private NetworkParameters params;
  private WhirlpoolWallet whirlpoolWallet;

  public WalletAggregateService(NetworkParameters params, WhirlpoolWallet whirlpoolWallet) {
    this.params = params;
    this.whirlpoolWallet = whirlpoolWallet;
  }

  private boolean toWallet(
      WhirlpoolAccount sourceAccount, BipWallet destinationWallet, int feeSatPerByte)
      throws Exception {
    return doAggregate(sourceAccount, null, destinationWallet, feeSatPerByte);
  }

  public boolean toAddress(WhirlpoolAccount sourceAccount, String destinationAddress)
      throws Exception {
    int feeSatPerByte = whirlpoolWallet.getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_2);
    return doAggregate(sourceAccount, destinationAddress, null, feeSatPerByte);
  }

  private boolean doAggregate(
      WhirlpoolAccount sourceAccount,
      String destinationAddress,
      BipWallet destinationWallet,
      int feeSatPerByte)
      throws Exception {
    if (!formatUtils.isTestNet(params)) {
      throw new NotifiableException(
          "Wallet aggregation is disabled on mainnet for security reasons.");
    }
    whirlpoolWallet.getUtxoSupplier().refresh();
    Collection<WhirlpoolUtxo> utxos = whirlpoolWallet.getUtxoSupplier().findUtxos(sourceAccount);
    if (utxos.isEmpty() || (utxos.size() == 1 && sourceAccount == destinationWallet.getAccount())) {
      // maybe you need to declare zpub as bip84 with /multiaddr?bip84=
      log.info(
          " -> no utxo to aggregate (" + sourceAccount + " -> " + destinationWallet.getId() + ")");
      return false;
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "Found "
              + utxos.size()
              + " utxo to aggregate ("
              + sourceAccount
              + " -> "
              + destinationWallet.getId()
              + "):");
      log.info(
          DebugUtils.getDebugUtxos(
              utxos, whirlpoolWallet.getChainSupplier().getLatestBlock().height));
    }

    boolean success = false;
    int round = 0;
    int offset = 0;
    WhirlpoolUtxo[] utxosArray = utxos.toArray(new WhirlpoolUtxo[] {});
    while (offset < utxos.size()) {
      List<BipUtxo> subsetUtxos = new ArrayList<>();
      offset = AGGREGATED_UTXOS_PER_TX * round;
      for (int i = offset; i < (offset + AGGREGATED_UTXOS_PER_TX) && i < utxos.size(); i++) {
        subsetUtxos.add(utxosArray[i]);
      }
      if (!subsetUtxos.isEmpty()) {
        String toAddress = destinationAddress;
        if (toAddress == null) {
          toAddress = destinationWallet.getNextAddressReceive().getAddressString();
        }

        log.info(" -> aggregating " + subsetUtxos.size() + " utxos (pass #" + round + ")");
        txAggregate(subsetUtxos, toAddress, feeSatPerByte);
        success = true;
      }
      round++;
    }
    return success;
  }

  private void txAggregate(List<BipUtxo> postmixUtxos, String toAddress, int feeSatPerByte)
      throws Exception {

    // tx
    Transaction txAggregate = computeTxAggregate(postmixUtxos, toAddress, feeSatPerByte);

    log.info("txAggregate:");
    log.info(txAggregate.toString());

    // broadcast
    log.info(" • Broadcasting TxAggregate...");
    String txHex = TxUtil.getInstance().getTxHex(txAggregate);
    whirlpoolWallet.getPushTx().pushTx(txHex);
  }

  private Transaction computeTxAggregate(
      List<BipUtxo> spendFroms, String toAddress, long feeSatPerByte) throws Exception {

    long inputsValue = BipUtxo.sumValue(spendFroms);

    Transaction tx = new Transaction(params);
    long minerFee =
        FeeUtil.getInstance().estimatedFeeSegwit(spendFroms.size(), 0, 0, 1, 0, feeSatPerByte);
    long destinationValue = inputsValue - minerFee;

    // 1 output
    if (log.isDebugEnabled()) {
      log.debug("Tx out: address=" + toAddress + " (" + destinationValue + " sats)");
    }

    TransactionOutput output = bech32Util.getTransactionOutput(toAddress, destinationValue, params);
    tx.addOutput(output);

    // prepare N inputs
    List<TransactionInput> inputs = new ArrayList<TransactionInput>();
    KeyBag keyBag = new KeyBag();
    for (BipUtxo spendFrom : spendFroms) {
      TransactionInput txInput = utxoUtil.computeSpendInput(spendFrom, params);
      inputs.add(txInput);
      keyBag.add(spendFrom, whirlpoolWallet.getUtxoSupplier());
      if (log.isDebugEnabled()) {
        log.debug("Tx in: " + spendFrom);
      }
    }

    // sort inputs & add
    Collections.sort(inputs, new BIP69InputComparator());
    for (TransactionInput ti : inputs) {
      tx.addInput(ti);
    }

    // sign inputs
    SendFactoryGeneric.getInstance()
        .signTransaction(tx, keyBag, whirlpoolWallet.getUtxoSupplier().getBipFormatSupplier());

    final String hexTx = TxUtil.getInstance().getTxHex(tx);
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    if (log.isDebugEnabled()) {
      log.debug("Tx hash: " + strTxHash);
      log.debug("Tx hex: " + hexTx + "\n");
    }
    return tx;
  }

  public boolean consolidateWallet() throws Exception {
    BipWallet destinationWallet = whirlpoolWallet.getWalletDeposit();
    boolean success = false;

    // consolidate each account to deposit
    int feeSatPerByte = whirlpoolWallet.getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_2);
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      log.info(" • Consolidating " + account + " -> " + destinationWallet.getId() + "...");
      if (toWallet(account, destinationWallet, feeSatPerByte)) {
        success = true;
      }
    }

    if (whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT).size() < 2) {
      log.info(" • Consolidating deposit... nothing to aggregate.");
      return false;
    }

    ClientUtils.sleepUtxosDelayAsync(params).blockingAwait();
    return success;
  }
}
