package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.BIP69InputComparatorUnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponse;
import java.math.BigInteger;
import java.util.*;
import java8.util.function.ToLongFunction;
import java8.util.stream.StreamSupport;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);

  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private final FormatsUtilGeneric formatsUtilGeneric = FormatsUtilGeneric.getInstance();
  private final WhirlpoolFee whirlpoolFee;

  private WhirlpoolWalletConfig config;

  public Tx0Service(WhirlpoolWalletConfig config) {
    this.config = config;
    whirlpoolFee = WhirlpoolFee.getInstance(config.getSecretPointFactory());
  }

  private int computeNbPremixMax(
      long premixValue,
      Collection<? extends UnspentOutput> spendFrom,
      long feeValueOrFeeChange,
      int feeTx0,
      Pool pool) {
    NetworkParameters params = config.getNetworkParameters();
    long spendFromBalance = computeSpendFromBalance(spendFrom);

    // compute nbPremix ignoring TX0 fee
    int nbPremixInitial = (int) Math.ceil(spendFromBalance / premixValue);

    // compute nbPremix with TX0 fee
    int nbPremix = nbPremixInitial;
    while (true) {
      // estimate TX0 fee for nbPremix
      long tx0MinerFee = ClientUtils.computeTx0MinerFee(nbPremix, feeTx0, spendFrom, params);
      long spendValue =
          ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
      if (log.isDebugEnabled()) {
        log.debug(
            "computeNbPremixMax: nbPremix="
                + nbPremix
                + " => spendValue="
                + spendValue
                + ", tx0MinerFee="
                + tx0MinerFee
                + ", spendFromBalance="
                + spendFromBalance
                + ", nbPremixInitial="
                + nbPremixInitial);
      }
      if (spendFromBalance < spendValue) {
        // if UTXO balance is insufficient, try with less nbPremix
        nbPremix--;
      } else {
        // nbPremix found
        break;
      }
    }
    // no negative value
    if (nbPremix < 0) {
      nbPremix = 0;
    }
    nbPremix = capNbPremix(nbPremix, pool);
    return nbPremix;
  }

  private long computeOutputsSum(Tx0Preview tx0Preview) {
    long tx0SpendValue =
        ClientUtils.computeTx0SpendValue(
            tx0Preview.getPremixValue(),
            tx0Preview.getNbPremix(),
            tx0Preview.computeFeeValueOrFeeChange(),
            tx0Preview.getTx0MinerFee());
    return tx0SpendValue + tx0Preview.getChangeValue();
  }

  public Tx0Preview tx0Preview(
      Collection<UnspentOutputWithKey> spendFroms, Tx0Config tx0Config, Tx0Param tx0Param)
      throws Exception {
    // fetch fresh Tx0Data
    Tx0Data tx0Data = fetchTx0Data(tx0Param.getPool().getPoolId());
    return tx0Preview(spendFroms, tx0Config, tx0Param, tx0Data);
  }

  protected Tx0Preview tx0Preview(
      Collection<UnspentOutputWithKey> spendFroms,
      Tx0Config tx0Config,
      Tx0Param tx0Param,
      Tx0Data tx0Data)
      throws Exception {

    // check balance min
    final long spendFromBalanceMin = tx0Param.getSpendFromBalanceMin();
    long spendFromBalance = computeSpendFromBalance(spendFroms);
    if (spendFromBalance < spendFromBalanceMin) {
      throw new NotifiableException(
          "Insufficient utxo value for Tx0: " + spendFromBalance + " < " + spendFromBalanceMin);
    }

    // check fee (duplicate safety check)
    int feeTx0 = tx0Param.getFeeTx0();
    if (feeTx0 < config.getFeeMin()) {
      throw new NotifiableException("Invalid fee for Tx0: " + feeTx0 + " < " + config.getFeeMin());
    }
    if (feeTx0 > config.getFeeMax()) {
      throw new NotifiableException("Invalid fee for Tx0: " + feeTx0 + " > " + config.getFeeMax());
    }

    // check premixValue (duplicate safety check)
    long premixValue = tx0Param.getPremixValue();
    if (!tx0Param.getPool().checkInputBalance(premixValue, false)) {
      throw new NotifiableException("Invalid premixValue for Tx0: " + premixValue);
    }

    NetworkParameters params = config.getNetworkParameters();
    long feeValueOrFeeChange = tx0Data.computeFeeValueOrFeeChange();
    Pool pool = tx0Param.getPool();
    int nbPremix = computeNbPremixMax(premixValue, spendFroms, feeValueOrFeeChange, feeTx0, pool);
    long tx0MinerFee = ClientUtils.computeTx0MinerFee(nbPremix, feeTx0, spendFroms, params);
    long premixMinerFee = tx0Param.getPremixValue() - tx0Param.getPool().getDenomination();
    long mixMinerFee = nbPremix * premixMinerFee;
    long spendValue =
        ClientUtils.computeTx0SpendValue(premixValue, nbPremix, feeValueOrFeeChange, tx0MinerFee);
    long changeValue = spendFromBalance - spendValue;

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool,
            tx0Data,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            tx0Param.getFeeTx0(),
            tx0Param.getFeePremix(),
            premixValue,
            changeValue,
            nbPremix);

    // verify outputsSum
    long outputsSum = computeOutputsSum(tx0Preview);
    if (outputsSum != spendFromBalance) {
      throw new Exception(
          "Invalid outputsSum for tx0: "
              + outputsSum
              + " vs "
              + spendFromBalance
              + " for tx0Preview=["
              + tx0Preview
              + "]");
    }
    return tx0Preview;
  }

  /** Generate maxOutputs premixes outputs max. */
  public Tx0 tx0(
      Collection<UnspentOutputWithKey> spendFroms,
      Bip84Wallet depositWallet,
      Bip84Wallet premixWallet,
      Bip84Wallet postmixWallet,
      Bip84Wallet badbankWallet,
      Tx0Config tx0Config,
      Tx0Param tx0Param)
      throws Exception {

    // compute & preview
    Tx0Preview tx0Preview = tx0Preview(spendFroms, tx0Config, tx0Param);

    log.info(
        " • Tx0: spendFrom="
            + spendFroms
            + ", tx0Param=["
            + tx0Param
            + "], changeWallet="
            + tx0Config.getChangeWallet().name()
            + ", tx0Preview=["
            + tx0Preview
            + "]");

    return tx0(
        spendFroms,
        depositWallet,
        premixWallet,
        postmixWallet,
        badbankWallet,
        tx0Config,
        tx0Preview);
  }

  public Tx0 tx0(
      Collection<UnspentOutputWithKey> spendFroms,
      Bip84Wallet depositWallet,
      Bip84Wallet premixWallet,
      Bip84Wallet postmixWallet,
      Bip84Wallet badbankWallet,
      Tx0Config tx0Config,
      Tx0Preview tx0Preview)
      throws Exception {
    NetworkParameters params = config.getNetworkParameters();

    Tx0Data tx0Data = tx0Preview.getTx0Data();

    // compute opReturnValue for feePaymentCode and feePayload
    byte[] feePayload = tx0Data.getFeePayload();
    int feeIndice;
    String feeOrBackAddressBech32;
    if (tx0Data.getFeeValue() > 0) {
      // pay to fee
      feeIndice = tx0Data.getFeeIndice();
      feeOrBackAddressBech32 = tx0Data.getFeeAddress();
      if (log.isDebugEnabled()) {
        log.debug(
            "feeAddressDestination: samourai => "
                + feeOrBackAddressBech32
                + ", feeIndice="
                + feeIndice);
      }
    } else {
      // pay to deposit
      feeIndice = 0;
      feeOrBackAddressBech32 = bech32Util.toBech32(depositWallet.getNextChangeAddress(), params);
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: back to deposit => " + feeOrBackAddressBech32);
      }
    }

    // sort inputs now, we need to know the first input for OP_RETURN encode
    List<UnspentOutputWithKey> sortedSpendFroms = new LinkedList<UnspentOutputWithKey>();
    sortedSpendFroms.addAll(spendFroms);
    Collections.sort(sortedSpendFroms, new BIP69InputComparatorUnspentOutput());

    UnspentOutputWithKey firstInput = sortedSpendFroms.get(0);
    String feePaymentCode = tx0Data.getFeePaymentCode();
    byte[] opReturnValue =
        whirlpoolFee.encode(
            feeIndice,
            feePayload,
            feePaymentCode,
            params,
            firstInput.getKey(),
            firstInput.computeOutpoint(params));
    if (log.isDebugEnabled()) {
      log.debug(
          "computing opReturnValue for feeIndice="
              + feeIndice
              + ", feePayloadHex="
              + (feePayload != null ? Hex.toHexString(feePayload) : "null"));
    }
    return tx0(
        sortedSpendFroms,
        depositWallet,
        premixWallet,
        postmixWallet,
        badbankWallet,
        tx0Config,
        tx0Preview,
        opReturnValue,
        feeOrBackAddressBech32);
  }

  protected Tx0 tx0(
      List<UnspentOutputWithKey> sortedSpendFroms,
      Bip84Wallet depositWallet,
      Bip84Wallet premixWallet,
      Bip84Wallet postmixWallet,
      Bip84Wallet badbankWallet,
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      byte[] opReturnValue,
      String feeOrBackAddressBech32)
      throws Exception {

    // find change wallet
    Bip84Wallet changeWallet;
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
            opReturnValue,
            feeOrBackAddressBech32,
            changeWallet,
            config.getNetworkParameters());

    Transaction tx = tx0.getTx();
    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
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

  private int capNbPremix(int nbPremix, Pool pool) {
    int maxOutputs = config.getTx0MaxOutputs();
    if (maxOutputs > 0) {
      nbPremix = Math.min(maxOutputs, nbPremix); // cap with maxOutputs
    }
    nbPremix = Math.min(pool.getTx0MaxOutputs(), nbPremix); // cap with pool.tx0MaxOutputs
    return nbPremix;
  }

  protected long computeSpendFromBalance(Collection<? extends UnspentOutput> spendFroms) {
    long balance =
        StreamSupport.stream(spendFroms)
            .mapToLong(
                new ToLongFunction<UnspentOutput>() {
                  @Override
                  public long applyAsLong(UnspentOutput unspentOutput) {
                    return unspentOutput.value;
                  }
                })
            .sum();
    return balance;
  }

  protected Tx0 buildTx0(
      Collection<UnspentOutputWithKey> sortedSpendFroms,
      Bip84Wallet premixWallet,
      Tx0Preview tx0Preview,
      byte[] opReturnValue,
      String feeOrBackAddressBech32,
      Bip84Wallet changeWallet,
      NetworkParameters params)
      throws Exception {

    long premixValue = tx0Preview.getPremixValue();
    long feeValueOrFeeChange = tx0Preview.computeFeeValueOrFeeChange();
    int nbPremix = capNbPremix(tx0Preview.getNbPremix(), tx0Preview.getPool());
    long changeValueTotal = tx0Preview.getChangeValue();

    // verify

    if (sortedSpendFroms.size() <= 0) {
      throw new IllegalArgumentException("spendFroms should be > 0");
    }

    if (feeValueOrFeeChange <= 0) {
      throw new IllegalArgumentException("samouraiFeeOrBack should be > 0");
    }

    // at least 1 premix
    if (nbPremix < 1) {
      throw new Exception("Invalid nbPremix=" + nbPremix);
    }

    // verify outputsSum
    long outputsSum = computeOutputsSum(tx0Preview);
    long spendFromBalance = computeSpendFromBalance(sortedSpendFroms);
    if (outputsSum != spendFromBalance) {
      throw new Exception("Invalid outputsSum for tx0: " + outputsSum + " vs " + spendFromBalance);
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
      HD_Address toAddress = premixWallet.getNextAddress();
      String toAddressBech32 = bech32Util.toBech32(toAddress, params);
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx0 out (premix): address="
                + toAddressBech32
                + ", path="
                + toAddress.toJSON().get("path")
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
      boolean useFakeOutputs = useFakeOutput(sortedSpendFroms);
      long[] changeValues = computeChangeValues(changeValueTotal, useFakeOutputs);
      for (long changeValue : changeValues) {
        HD_Address changeAddress = changeWallet.getNextChangeAddress();
        String changeAddressBech32 = bech32Util.toBech32(changeAddress, params);
        TransactionOutput changeOutput =
            bech32Util.getTransactionOutput(changeAddressBech32, changeValue, params);
        outputs.add(changeOutput);
        changeOutputs.add(changeOutput);
        if (log.isDebugEnabled()) {
          log.debug(
              "Tx0 out (change): address="
                  + changeAddressBech32
                  + ", path="
                  + changeAddress.toJSON().get("path")
                  + " ("
                  + changeValue
                  + " sats)");
        }
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
        new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturnValue).build();
    TransactionOutput txFeeOutput =
        new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
    outputs.add(txFeeOutput);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (OP_RETURN): " + opReturnValue.length + " bytes");
    }
    if (opReturnValue.length != WhirlpoolFee.FEE_LENGTH) {
      throw new Exception(
          "Invalid opReturnValue length detected, please report this bug. opReturnValue="
              + opReturnValue
              + " vs "
              + WhirlpoolFee.FEE_LENGTH);
    }

    // all outputs
    Collections.sort(outputs, new BIP69OutputComparator());
    for (TransactionOutput to : outputs) {
      tx.addOutput(to);
    }

    // all inputs
    for (UnspentOutputWithKey spendFrom : sortedSpendFroms) {
      buildTx0Input(tx, spendFrom, params);
      if (log.isDebugEnabled()) {
        log.debug("Tx0 in: utxo=" + spendFrom);
      }
    }

    signTx0(tx, sortedSpendFroms, params);
    tx.verify();

    Tx0 tx0 = new Tx0(tx0Preview, tx, premixOutputs, changeOutputs);
    return tx0;
  }

  private boolean useFakeOutput(Collection<UnspentOutputWithKey> spendFroms) {
    // experimental feature reserved for testnet
    if (!FormatsUtilGeneric.getInstance().isTestNet(config.getNetworkParameters())) {
      return false;
    }

    // single prev-tx => never
    int nbPrevTxs = ClientUtils.countPrevTxs(spendFroms);
    if (nbPrevTxs == 1) {
      if (log.isDebugEnabled()) {
        log.debug("useFakeOutput => false (nbPrevTxs=" + nbPrevTxs + ")");
      }
      return false;
    }

    int randomFactor = config.getTx0FakeOutputRandomFactor();
    // 0 => never
    if (randomFactor == 0) {
      if (log.isDebugEnabled()) {
        log.debug("useFakeOutput => false (randomFactor=" + randomFactor + ")");
      }
      return false;
    }
    // 1 => always
    if (randomFactor == 1) {
      if (log.isDebugEnabled()) {
        log.debug("useFakeOutput => true (randomFactor=" + randomFactor + ")");
      }
      return true;
    }
    // random
    boolean result = ClientUtils.random(1, randomFactor) == 1;
    if (log.isDebugEnabled()) {
      log.debug("useFakeOutput => " + result + " (randomFactor=" + randomFactor + ")");
    }
    return result;
  }

  protected long[] computeChangeValues(long changeValueTotal, boolean useFakeOutput) {
    final int VALUE_MIN = config.getTx0FakeOutputMinValue();
    if (changeValueTotal < (2 * VALUE_MIN + 1)) {
      // changeValueTotal too low for fake output
      useFakeOutput = false;
    }
    if (useFakeOutput) {
      // 2 change outputs
      long changeValue1 = ClientUtils.random(VALUE_MIN, changeValueTotal - VALUE_MIN);
      long changeValue2 = changeValueTotal - changeValue1;
      return new long[] {changeValue1, changeValue2};
    } else {
      // 1 change output
      return new long[] {changeValueTotal};
    }
  }

  protected void buildTx0Input(
      Transaction tx, UnspentOutputWithKey input, NetworkParameters params) {
    ECKey spendFromKey = ECKey.fromPrivate(input.getKey());
    TransactionOutPoint depositSpendFrom = input.computeOutpoint(params);

    SegwitAddress segwitAddress = new SegwitAddress(spendFromKey.getPubKey(), params);
    WpTransactionOutPoint outpoint =
        new WpTransactionOutPoint(
            params,
            depositSpendFrom.getHash(),
            (int) depositSpendFrom.getIndex(),
            BigInteger.valueOf(depositSpendFrom.getValue().longValue()),
            segwitAddress.segWitRedeemScript().getProgram());

    TransactionInput _input = new TransactionInput(params, null, new byte[0], outpoint);
    tx.addInput(_input);
  }

  protected void signTx0(
      Transaction tx, Collection<UnspentOutputWithKey> inputs, NetworkParameters params) {
    int idx = 0;
    for (UnspentOutputWithKey input : inputs) {

      String address = input.addr;
      ECKey spendFromKey = ECKey.fromPrivate(input.getKey());

      // sign input
      boolean isBech32 = formatsUtilGeneric.isValidBech32(address);
      if (isBech32 || Address.fromBase58(params, address).isP2SHAddress()) {

        SegwitAddress segwitAddress = new SegwitAddress(spendFromKey.getPubKey(), params);
        final Script redeemScript = segwitAddress.segWitRedeemScript();
        final Script scriptCode = redeemScript.scriptCode();

        TransactionSignature sig =
            tx.calculateWitnessSignature(
                idx,
                spendFromKey,
                scriptCode,
                Coin.valueOf(input.value),
                Transaction.SigHash.ALL,
                false);
        final TransactionWitness witness = new TransactionWitness(2);
        witness.setPush(0, sig.encodeToBitcoin());
        witness.setPush(1, spendFromKey.getPubKey());
        tx.setWitness(idx, witness);

        if (!isBech32) {
          // P2SH
          final ScriptBuilder sigScript = new ScriptBuilder();
          sigScript.data(redeemScript.getProgram());
          tx.getInput(idx).setScriptSig(sigScript.build());
          //                    tx.getInput(idx).getScriptSig().correctlySpends(tx, idx, new
          // Script(Hex.decode(input.script)), Coin.valueOf(input.value), Script.ALL_VERIFY_FLAGS);
        }

      } else {
        TransactionSignature sig =
            tx.calculateSignature(
                idx,
                spendFromKey,
                new Script(Hex.decode(input.script)),
                Transaction.SigHash.ALL,
                false);
        tx.getInput(idx).setScriptSig(ScriptBuilder.createInputScript(sig, spendFromKey));
      }

      idx++;
    }
  }

  protected Tx0Data fetchTx0Data(String poolId) throws Exception {
    try {
      Tx0DataResponse tx0Response = config.getServerApi().fetchTx0Data(poolId, config.getScode());
      byte[] feePayload = WhirlpoolProtocol.decodeBytes(tx0Response.feePayload64);
      Tx0Data tx0Data =
          new Tx0Data(
              tx0Response.feePaymentCode,
              tx0Response.feeValue,
              tx0Response.feeChange,
              tx0Response.feeDiscountPercent,
              // tx0Response.message,
              feePayload,
              tx0Response.feeAddress,
              tx0Response.feeIndice);
      return tx0Data;
    } catch (HttpException e) {
      throw ClientUtils.wrapRestError(e);
    }
  }
}
