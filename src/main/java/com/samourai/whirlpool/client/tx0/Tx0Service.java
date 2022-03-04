package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.BIP69InputComparatorUnspentOutput;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.util.XorMask;
import java.util.*;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);

  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private final XorMask xorMask;

  private WhirlpoolWalletConfig config;
  private Tx0PreviewService tx0PreviewService;

  public Tx0Service(WhirlpoolWalletConfig config, Tx0PreviewService tx0PreviewService) {
    this.config = config;
    this.tx0PreviewService = tx0PreviewService;
    xorMask = XorMask.getInstance(config.getSecretPointFactory());
  }

  /** Generate maxOutputs premixes outputs max. */
  public Tx0 tx0(
      Collection<UnspentOutput> spendFroms,
      BipWallet depositWallet,
      BipWallet premixWallet,
      BipWallet postmixWallet,
      BipWallet badbankWallet,
      Pool pool,
      Tx0Config tx0Config,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {

    // compute & preview
    Tx0Previews tx0Previews = tx0PreviewService.tx0Previews(tx0Config, spendFroms);
    Tx0Preview tx0Preview = tx0Previews.getTx0Preview(pool.getPoolId());
    if (tx0Preview == null) {
      throw new NotifiableException("Tx0 not possible for pool: " + pool.getPoolId());
    }

    log.info(
        " • Tx0: spendFrom="
            + spendFroms
            + ", changeWallet="
            + tx0Config.getChangeWallet().name()
            + ", tx0Preview={"
            + tx0Preview
            + "}");

    return tx0(
        spendFroms,
        depositWallet,
        premixWallet,
        postmixWallet,
        badbankWallet,
        tx0Config,
        tx0Preview,
        utxoKeyProvider);
  }

  public Tx0 tx0(
      Collection<UnspentOutput> spendFroms,
      BipWallet depositWallet,
      BipWallet premixWallet,
      BipWallet postmixWallet,
      BipWallet badbankWallet,
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    NetworkParameters params = config.getNetworkParameters();

    Tx0Data tx0Data = tx0Preview.getTx0Data();

    // compute opReturnValue for feePaymentCode and feePayload
    String feeOrBackAddressBech32;
    if (tx0Data.getFeeValue() > 0) {
      // pay to fee
      feeOrBackAddressBech32 = tx0Data.getFeeAddress();
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: samourai => " + feeOrBackAddressBech32);
      }
    } else {
      // pay to deposit
      feeOrBackAddressBech32 = depositWallet.getNextChangeAddress().getAddressString();
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: back to deposit => " + feeOrBackAddressBech32);
      }
    }

    // sort inputs now, we need to know the first input for OP_RETURN encode
    List<UnspentOutput> sortedSpendFroms = new LinkedList<UnspentOutput>();
    sortedSpendFroms.addAll(spendFroms);
    Collections.sort(sortedSpendFroms, new BIP69InputComparatorUnspentOutput());

    // compute feePayloadMasked
    UnspentOutput firstInput = sortedSpendFroms.get(0);
    byte[] firstInputKey = utxoKeyProvider._getPrivKey(firstInput.tx_hash, firstInput.tx_output_n);
    String feePaymentCode = tx0Data.getFeePaymentCode();
    byte[] feePayload = tx0Data.getFeePayload();
    byte[] feePayloadMasked =
        xorMask.mask(
            feePayload, feePaymentCode, params, firstInputKey, firstInput.computeOutpoint(params));
    if (log.isDebugEnabled()) {
      log.debug("feePayloadHex=" + Hex.toHexString(feePayload));
    }
    return tx0(
        sortedSpendFroms,
        depositWallet,
        premixWallet,
        postmixWallet,
        badbankWallet,
        tx0Config,
        tx0Preview,
        feePayloadMasked,
        feeOrBackAddressBech32,
        utxoKeyProvider);
  }

  protected Tx0 tx0(
      List<UnspentOutput> sortedSpendFroms,
      BipWallet depositWallet,
      BipWallet premixWallet,
      BipWallet postmixWallet,
      BipWallet badbankWallet,
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      byte[] feePayloadMasked,
      String feeOrBackAddressBech32,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {

    // find change wallet
    BipWallet changeWallet;
    switch (tx0Config.getChangeWallet()) {
      case PREMIX:
        changeWallet = premixWallet;
        break;
      case POSTMIX:
        changeWallet = postmixWallet;
        break;
      case BADBANK:
        changeWallet = badbankWallet;
        break;
      default:
        changeWallet = depositWallet;
        break;
    }

    //
    // tx0
    //

    Tx0 tx0 =
        buildTx0(
            sortedSpendFroms,
            premixWallet,
            tx0Preview,
            feePayloadMasked,
            feeOrBackAddressBech32,
            changeWallet,
            config.getNetworkParameters(),
            utxoKeyProvider);

    Transaction tx = tx0.getTx();
    final String hexTx = TxUtil.getInstance().getTxHex(tx);
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    // System.out.println(tx);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 hash: " + strTxHash);
      log.debug("Tx0 hex: " + hexTx);
      long feePrice = tx0Preview.getTx0MinerFee() / tx.getVirtualTransactionSize();
      log.debug("Tx0 size: " + tx.getVirtualTransactionSize() + "b, feePrice=" + feePrice + "s/b");
    }
    return tx0;
  }

  protected Tx0 buildTx0(
      Collection<UnspentOutput> sortedSpendFroms,
      BipWallet premixWallet,
      Tx0Preview tx0Preview,
      byte[] feePayloadMasked,
      String feeOrBackAddressBech32,
      BipWallet changeWallet,
      NetworkParameters params,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {

    long premixValue = tx0Preview.getPremixValue();
    long feeValueOrFeeChange = tx0Preview.getTx0Data().computeFeeValueOrFeeChange();
    int nbPremix = tx0PreviewService.capNbPremix(tx0Preview.getNbPremix(), tx0Preview.getPool());
    long changeValueTotal = tx0Preview.getChangeValue();

    // verify

    if (sortedSpendFroms.size() <= 0) {
      throw new IllegalArgumentException("spendFroms should be > 0");
    }

    if (feeValueOrFeeChange <= 0) {
      throw new IllegalArgumentException("feeValueOrFeeChange should be > 0");
    }

    // at least 1 premix
    if (nbPremix < 1) {
      throw new Exception("Invalid nbPremix=" + nbPremix);
    }

    // verify outputsSum
    long totalValue = tx0Preview.getTotalValue();
    long spendFromBalance = UnspentOutput.sumValue(sortedSpendFroms);
    if (totalValue != spendFromBalance) {
      throw new Exception("Invalid outputsSum for tx0: " + totalValue + " vs " + spendFromBalance);
    }

    //
    // tx0
    //
    //
    // make tx:
    // 5 spendTo outputs
    // SW fee
    // change
    // OP_RETURN
    //
    List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
    Transaction tx = new Transaction(params);

    //
    // premix outputs
    //
    List<TransactionOutput> premixOutputs = new ArrayList<TransactionOutput>();
    for (int j = 0; j < nbPremix; j++) {
      // send to PREMIX
      BipAddress toAddress = premixWallet.getNextAddress();
      String toAddressBech32 = toAddress.getAddressString();
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx0 out (premix): address="
                + toAddressBech32
                + ", path="
                + toAddress.getPathAddress()
                + " ("
                + premixValue
                + " sats)");
      }

      TransactionOutput txOutSpend =
          bech32Util.getTransactionOutput(toAddressBech32, premixValue, params);
      outputs.add(txOutSpend);
      premixOutputs.add(txOutSpend);
    }

    //
    // 1 or 2 change output(s)
    //
    List<TransactionOutput> changeOutputs = new LinkedList<TransactionOutput>();
    if (changeValueTotal > 0) {
      BipAddress changeAddress = changeWallet.getNextChangeAddress();
      String changeAddressBech32 = changeAddress.getAddressString();
      TransactionOutput changeOutput =
          bech32Util.getTransactionOutput(changeAddressBech32, changeValueTotal, params);
      outputs.add(changeOutput);
      changeOutputs.add(changeOutput);
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx0 out (change): address="
                + changeAddressBech32
                + ", path="
                + changeAddress.getPathAddress()
                + " ("
                + changeValueTotal
                + " sats)");
      }
    } else {
      if (log.isDebugEnabled()) {
        log.debug("Tx0: spending whole utx0, no change");
      }
      if (changeValueTotal < 0) {
        throw new Exception(
            "Negative change detected, please report this bug. tx0Preview=" + tx0Preview);
      }
    }

    // samourai fee (or back deposit)
    TransactionOutput txSWFee =
        bech32Util.getTransactionOutput(feeOrBackAddressBech32, feeValueOrFeeChange, params);
    outputs.add(txSWFee);
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0 out (fee): feeAddress="
              + feeOrBackAddressBech32
              + " ("
              + feeValueOrFeeChange
              + " sats)");
    }

    // add OP_RETURN output
    Script op_returnOutputScript =
        new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(feePayloadMasked).build();
    TransactionOutput txFeeOutput =
        new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
    outputs.add(txFeeOutput);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (OP_RETURN): " + feePayloadMasked.length + " bytes");
    }
    if (feePayloadMasked.length != WhirlpoolProtocol.FEE_PAYLOAD_LENGTH) {
      throw new Exception(
          "Invalid opReturnValue length detected, please report this bug. opReturnValue="
              + feePayloadMasked
              + " vs "
              + WhirlpoolProtocol.FEE_PAYLOAD_LENGTH);
    }

    // all outputs
    Collections.sort(outputs, new BIP69OutputComparator());
    for (TransactionOutput to : outputs) {
      tx.addOutput(to);
    }

    // all inputs
    for (UnspentOutput spendFrom : sortedSpendFroms) {
      TransactionInput input = spendFrom.computeSpendInput(params);
      tx.addInput(input);
      if (log.isDebugEnabled()) {
        log.debug("Tx0 in: utxo=" + spendFrom);
      }
    }

    signTx0(tx, utxoKeyProvider);
    tx.verify();

    Tx0 tx0 = new Tx0(tx0Preview, tx, premixOutputs, changeOutputs);
    return tx0;
  }

  protected void signTx0(Transaction tx, UtxoKeyProvider utxoKeyProvider) throws Exception {
    SendFactoryGeneric.getInstance().signTransaction(tx, utxoKeyProvider);
  }
}
