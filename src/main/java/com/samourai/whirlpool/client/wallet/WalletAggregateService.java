package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.client.BipWallet;
import com.samourai.wallet.client.BipWalletAndAddressType;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java8.util.Lists;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletAggregateService {
  private Logger log = LoggerFactory.getLogger(WalletAggregateService.class);
  private static final int AGGREGATED_UTXOS_PER_TX = 600;
  private static final FormatsUtilGeneric formatUtils = FormatsUtilGeneric.getInstance();

  private NetworkParameters params;
  private Bech32UtilGeneric bech32Util;

  public WalletAggregateService(NetworkParameters params, Bech32UtilGeneric bech32Util) {
    this.params = params;
    this.bech32Util = bech32Util;
  }

  private boolean toWallet(
      BipWalletAndAddressType sourceWallet,
      BipWalletAndAddressType destinationWallet,
      int feeSatPerByte,
      BackendApi backendApi,
      UtxoKeyProvider utxoKeySupplier)
      throws Exception {
    return doAggregate(
        sourceWallet, null, destinationWallet, feeSatPerByte, backendApi, utxoKeySupplier);
  }

  public boolean toAddress(
      BipWalletAndAddressType sourceWallet,
      String destinationAddress,
      WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    int feeSatPerByte = whirlpoolWallet.getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_2);
    BackendApi backendApi = whirlpoolWallet.getConfig().getBackendApi();
    UtxoSupplier utxoSupplier = whirlpoolWallet.getUtxoSupplier();
    return doAggregate(
        sourceWallet, destinationAddress, null, feeSatPerByte, backendApi, utxoSupplier);
  }

  private boolean doAggregate(
      BipWalletAndAddressType sourceWallet,
      String destinationAddress,
      BipWalletAndAddressType destinationWallet,
      int feeSatPerByte,
      BackendApi backendApi,
      UtxoKeyProvider utxoKeySupplier)
      throws Exception {
    if (!formatUtils.isTestNet(params)) {
      throw new NotifiableException(
          "Wallet aggregation is disabled on mainnet for security reasons.");
    }
    List<UnspentOutput> utxos =
        Lists.of(backendApi.fetchWallet(sourceWallet.getPub()).unspent_outputs);
    if (utxos.isEmpty() || (utxos.size() == 1 && sourceWallet == destinationWallet)) {
      // maybe you need to declare zpub as bip84 with /multiaddr?bip84=
      log.info(
          " -> no utxo to aggregate ("
              + sourceWallet.getAccount()
              + ":"
              + sourceWallet.getAddressType()
              + " -> "
              + destinationWallet.getAccount()
              + ":"
              + destinationWallet.getAddressType()
              + ")");
      return false;
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "Found "
              + utxos.size()
              + " utxo to aggregate ("
              + sourceWallet.getAccount()
              + ":"
              + sourceWallet.getAddressType()
              + " -> "
              + destinationWallet.getAccount()
              + ":"
              + destinationWallet.getAddressType()
              + "):");
      ClientUtils.logUtxos(
          utxos,
          sourceWallet.getAddressType().getPurpose(),
          sourceWallet.getAccount().getAccountIndex(),
          params);
    }

    boolean success = false;
    int round = 0;
    int offset = 0;
    while (offset < utxos.size()) {
      List<UnspentOutput> subsetUtxos = new ArrayList<UnspentOutput>();
      offset = AGGREGATED_UTXOS_PER_TX * round;
      for (int i = offset; i < (offset + AGGREGATED_UTXOS_PER_TX) && i < utxos.size(); i++) {
        subsetUtxos.add(utxos.get(i));
      }
      if (!subsetUtxos.isEmpty()) {
        String toAddress = destinationAddress;
        if (toAddress == null) {
          toAddress = bech32Util.toBech32(destinationWallet.getNextAddress(), params);
        }

        log.info(" -> aggregating " + subsetUtxos.size() + " utxos (pass #" + round + ")");
        txAggregate(
            sourceWallet, subsetUtxos, toAddress, feeSatPerByte, backendApi, utxoKeySupplier);
        success = true;
      }
      round++;
    }
    return success;
  }

  private void txAggregate(
      BipWallet sourceWallet,
      List<UnspentOutput> postmixUtxos,
      String toAddress,
      int feeSatPerByte,
      BackendApi backendApi,
      UtxoKeyProvider utxoKeySupplier)
      throws Exception {

    // tx
    Transaction txAggregate = txAggregate(postmixUtxos, toAddress, feeSatPerByte, utxoKeySupplier);

    log.info("txAggregate:");
    log.info(txAggregate.toString());

    // broadcast
    log.info(" • Broadcasting TxAggregate...");
    String txHex = ClientUtils.getTxHex(txAggregate);
    backendApi.pushTx(txHex);
  }

  private Transaction txAggregate(
      List<UnspentOutput> spendFroms,
      String toAddress,
      long feeSatPerByte,
      UtxoKeyProvider utxoKeySupplier)
      throws Exception {

    long inputsValue = UnspentOutput.sumValue(spendFroms);

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
    for (int i = 0; i < spendFroms.size(); i++) {
      UnspentOutput spendFrom = spendFroms.get(i);
      TransactionInput txInput = spendFrom.computeSpendInput(params);
      inputs.add(txInput);
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
    SendFactoryGeneric.getInstance().signTransaction(tx, utxoKeySupplier);

    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    if (log.isDebugEnabled()) {
      log.debug("Tx hash: " + strTxHash);
      log.debug("Tx hex: " + hexTx + "\n");
    }
    return tx;
  }

  public boolean consolidateWallet(WhirlpoolWallet whirlpoolWallet, UtxoKeyProvider utxoKeySupplier)
      throws Exception {
    BipWalletAndAddressType depositWallet = whirlpoolWallet.getWalletDeposit();
    BackendApi backendApi = whirlpoolWallet.getConfig().getBackendApi();

    // consolidate each wallet to deposit
    int feeSatPerByte = whirlpoolWallet.getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_2);
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      if (account != WhirlpoolAccount.DEPOSIT) {
        for (AddressType addressType : account.getAddressTypes()) {
          BipWalletAndAddressType sourceWallet =
              whirlpoolWallet.getWalletSupplier().getWallet(account, addressType);
          log.info(
              " • Consolidating "
                  + account
                  + "/"
                  + addressType
                  + " -> "
                  + depositWallet.getAccount()
                  + "/"
                  + depositWallet.getAddressType()
                  + "...");
          toWallet(sourceWallet, depositWallet, feeSatPerByte, backendApi, utxoKeySupplier);
        }
      }
    }

    if (whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT).size() < 2) {
      log.info(" • Consolidating deposit... nothing to aggregate.");
      return false;
    }

    ClientUtils.sleepUtxosDelay(params);
    log.info(" • Consolidating deposit...");
    boolean success =
        toWallet(
            depositWallet,
            depositWallet,
            feeSatPerByte,
            backendApi,
            whirlpoolWallet.getUtxoSupplier());
    return success;
  }
}
