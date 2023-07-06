package com.samourai.whirlpool.client.tx0;

import com.google.common.collect.Lists;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.KeyBag;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.event.Tx0Event;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.PushTxErrorResponseException;
import com.samourai.whirlpool.client.utils.BIP69InputComparatorUnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.SpendFromsComparator;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.PoolComparatorByDenominationDesc;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImpl;
import com.samourai.whirlpool.protocol.rest.PushTxErrorResponse;
import com.samourai.whirlpool.protocol.rest.PushTxSuccessResponse;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import io.reactivex.Single;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);

  private Tx0PreviewService tx0PreviewService;
  private NetworkParameters params;
  private FeeOpReturnImpl feeOpReturnImpl;
  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  public Tx0Service(
      NetworkParameters params,
      Tx0PreviewService tx0PreviewService,
      FeeOpReturnImpl feeOpReturnImpl) {
    this.params = params;
    this.tx0PreviewService = tx0PreviewService;
    this.feeOpReturnImpl = feeOpReturnImpl;
    if (log.isDebugEnabled()) {
      log.debug(
          "Using feeOpReturnImpl="
              + feeOpReturnImpl.getClass().getName()
              + ", opReturnVersion="
              + feeOpReturnImpl.getOpReturnVersion());
    }
  }

  /** Generate maxOutputs premixes outputs max. */
  public Tx0 tx0(
      Collection<UnspentOutput> spendFroms,
      WalletSupplier walletSupplier,
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

    Tx0 tx0 = tx0(spendFroms, walletSupplier, tx0Config, tx0Preview, utxoKeyProvider);
    log.info(
        " • Tx0 result: txid="
            + tx0.getTx().getHashAsString()
            + ", nbPremixs="
            + tx0.getPremixOutputs().size());
    if (log.isDebugEnabled()) {
      log.debug(tx0.getTx().toString());
    }
    return tx0;
  }

  public Tx0 tx0(
      Collection<UnspentOutput> spendFroms,
      WalletSupplier walletSupplier,
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {

    // save indexes state with Tx0Context
    BipWallet premixWallet = walletSupplier.getWallet(BIP_WALLET.PREMIX_BIP84);
    BipWallet changeWallet =
        walletSupplier.getWallet(tx0Config.getChangeWallet(), BIP_FORMAT.SEGWIT_NATIVE);
    Tx0Context tx0Context = new Tx0Context(premixWallet, changeWallet);

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
      BipWallet depositWallet = walletSupplier.getWallet(BIP_WALLET.DEPOSIT_BIP84);
      feeOrBackAddressBech32 = depositWallet.getNextChangeAddress().getAddressString();
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: back to deposit => " + feeOrBackAddressBech32);
      }
    }

    // sort inputs now, we need to know the first input for OP_RETURN encode
    List<UnspentOutput> sortedSpendFroms = new LinkedList<>();
    sortedSpendFroms.addAll(spendFroms);
    sortedSpendFroms.sort(new BIP69InputComparatorUnspentOutput());

    // op_return
    if (sortedSpendFroms.isEmpty()) {
      throw new IllegalArgumentException("spendFroms should be > 0");
    }
    UnspentOutput firstInput = sortedSpendFroms.get(0);
    BipAddress firstInputAddress = walletSupplier.getAddress(firstInput);
    byte[] firstInputKey = firstInputAddress.getHdAddress().getECKey().getPrivKeyBytes();
    byte[] opReturn = computeOpReturn(firstInput, firstInputKey, tx0Data);

    //
    // tx0
    //

    Tx0 tx0 =
        buildTx0(
            tx0Config,
            sortedSpendFroms,
            walletSupplier,
            premixWallet,
            tx0Preview,
            opReturn,
            feeOrBackAddressBech32,
            changeWallet,
            utxoKeyProvider,
            tx0Context);

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

  protected byte[] computeOpReturn(UnspentOutput firstInput, byte[] firstInputKey, Tx0Data tx0Data)
      throws Exception {

    // use input0 for masking
    TransactionOutPoint maskingOutpoint = firstInput.computeOutpoint(params);
    String feePaymentCode = tx0Data.getFeePaymentCode();
    byte[] feePayload = tx0Data.getFeePayload();
    return feeOpReturnImpl.computeOpReturn(
        feePaymentCode, feePayload, maskingOutpoint, firstInputKey);
  }

  protected Tx0 buildTx0(
      Tx0Config tx0Config,
      Collection<UnspentOutput> sortedSpendFroms,
      WalletSupplier walletSupplier,
      BipWallet premixWallet,
      Tx0Preview tx0Preview,
      byte[] opReturn,
      String feeOrBackAddressBech32,
      BipWallet changeWallet,
      UtxoKeyProvider utxoKeyProvider,
      Tx0Context tx0Context)
      throws Exception {

    long premixValue = tx0Preview.getPremixValue();
    long feeValueOrFeeChange = tx0Preview.getTx0Data().computeFeeValueOrFeeChange();
    int nbPremix = tx0PreviewService.capNbPremix(tx0Preview.getNbPremix(), tx0Preview.getPool());

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
    List<TransactionOutput> outputs = new ArrayList<>();
    Transaction tx = new Transaction(params);

    //
    // premix outputs
    //
    List<TransactionOutput> premixOutputs = new ArrayList<>();
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
    // 1 or 2 change output(s) [Tx0]
    // 2 or 3 change outputs [Decoy Tx0x2]
    //
    List<TransactionOutput> changeOutputs = new LinkedList<>();
    List<BipAddress> changeOutputsAddresses = new LinkedList<>();

    List<Long> changeAmounts = computeChangeAmounts(tx0Config, tx0Preview, sortedSpendFroms);

    if (!changeAmounts.isEmpty()) {
      //  if decoy tx0x2 change outputs total > premix output value, remove last premix output
      if (tx0Config.isDecoyTx0x2() && changeAmounts.size() == 2) {
        if (changeAmounts.get(0) + changeAmounts.get(1) > tx0Preview.getPremixValue()) {
          // should remove at most 1 premix output
          nbPremix--;
          premixOutputs.remove(nbPremix);
          outputs.remove(nbPremix);
        }
      }

      // change outputs
      for (long changeAmount : changeAmounts) {
        BipAddress changeAddress = changeWallet.getNextChangeAddress();
        String changeAddressBech32 = changeAddress.getAddressString();
        TransactionOutput changeOutput =
          bech32Util.getTransactionOutput(changeAddressBech32, changeAmount, params);
        outputs.add(changeOutput);
        changeOutputs.add(changeOutput);
        changeOutputsAddresses.add(changeAddress);
        if (log.isDebugEnabled()) {
          log.debug(
            "Tx0 out (change): address="
                + changeAddressBech32
                + ", path="
                + changeAddress.getPathAddress()
                + " ("
                + changeAmount
                + " sats)");
        }
      }
    }

    // samourai fee (or back deposit)
    TransactionOutput samouraiFeeOutput =
        bech32Util.getTransactionOutput(feeOrBackAddressBech32, feeValueOrFeeChange, params);
    outputs.add(samouraiFeeOutput);
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
        new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturn).build();
    TransactionOutput opReturnOutput =
        new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
    outputs.add(opReturnOutput);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (OP_RETURN): " + opReturn.length + " bytes");
    }

    // all outputs
    outputs.sort(new BIP69OutputComparator());
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

    KeyBag keyBag = new KeyBag();
    keyBag.addAll(sortedSpendFroms, walletSupplier);
    signTx0(tx, keyBag, utxoKeyProvider.getBipFormatSupplier());
    tx.verify();

    // build changeUtxos *after* tx is signed
    List<UnspentOutput> changeUtxos =
        computeChangeUtxos(changeOutputs, changeOutputsAddresses, changeWallet);

    Tx0 tx0 =
        new Tx0(
            tx0Preview,
            sortedSpendFroms,
            tx0Config,
            tx0Context,
            tx,
            premixOutputs,
            changeOutputs,
            changeUtxos,
            opReturnOutput,
            samouraiFeeOutput);
    return tx0;
  }

  private List<Long> computeChangeAmounts(
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      Collection<UnspentOutput> sortedSpendFroms)
      throws Exception {
    long changeValueTotal = tx0Preview.getChangeValue();

    if (changeValueTotal < 0) {
      throw new Exception(
        "Negative change detected, please report this bug. tx0Preview=" + tx0Preview);
    }
    if (changeValueTotal == 0) {
      if (log.isDebugEnabled()) {
        log.debug("Tx0: spending whole utx0, no change");
      }
      return Lists.newLinkedList();
    }

    if (tx0Config.isDecoyTx0x2()) {
      // attempt to decoy Tx0x2: split change between 2 change addresses with STONEWALL
      List<Long> changeAmountsStonewall = computeChangeAmountsStonewall(tx0Config, tx0Preview, sortedSpendFroms);
      if (changeAmountsStonewall != null) {
        if (log.isDebugEnabled()) {
          log.debug("Tx0: decoy Tx0, 2 changes");
        }
        return changeAmountsStonewall;
      }
    }

    // normal Tx0
    if (log.isDebugEnabled()) {
      log.debug("Tx0: normal Tx0, 1 change");
    }
    return Arrays.asList(changeValueTotal);
  }

  List<Long> computeChangeAmountsStonewall(
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      Collection<UnspentOutput> sortedSpendFroms)
      throws Exception {
    Pair<Long,Long> spendFromAmountsStonewall =
      computeSpendFromAmountsStonewall(tx0Config, tx0Preview, sortedSpendFroms);
    if (spendFromAmountsStonewall == null) {
      // stonewall is not possible
      return null;
    }

    // calculate changes
    long changeValueTotalA = spendFromAmountsStonewall.getLeft(); // spend value A
    long changeValueTotalB = spendFromAmountsStonewall.getRight(); // spend value B

    if (tx0Config.getCascadingParent() == null) {
      // initial pool, split fee
      changeValueTotalA -= tx0Preview.getFeeValue() / 2L;
      changeValueTotalB -= tx0Preview.getFeeValue() / 2L;
    } else {
      // lower pools
      changeValueTotalA -= tx0Preview.getFeeChange();
    }

    // split miner fees
    changeValueTotalA -= tx0Preview.getTx0MinerFee() / 2L;
    changeValueTotalB -= tx0Preview.getTx0MinerFee() / 2L;

    int nbPremixA = 0;
    while (changeValueTotalA > tx0Preview.getPremixValue()) {
      changeValueTotalA -= tx0Preview.getPremixValue();
      nbPremixA++;
    }
    int nbPremixB = 0;
    while (changeValueTotalB > tx0Preview.getPremixValue()) {
      changeValueTotalB -= tx0Preview.getPremixValue();
      nbPremixB++;
    }

    long changeValueTotal = changeValueTotalA + changeValueTotalB;

    // if nbPremix < tx0Preview.nbPremix => remove 1 premixOutput. should be at most 1 less.
    int nbPremix = nbPremixA + nbPremixB;
    if (nbPremix < tx0Preview.getNbPremix()) {
      // TODO - Not 100% sure if should recalculate tx0MinerFee.
      //  Recalculating makes it exact as Tx0x2, but usually a small sat difference.

      // recalculate tx0MinerFee
      long tx0MinerFee = ClientUtils.computeTx0MinerFee(
        nbPremix, tx0Preview.getTx0MinerFeePrice(), sortedSpendFroms, params);
      if (tx0MinerFee % 2L != 0) {
        tx0MinerFee++;
      }

      // add tx0MinerFee difference back to change
      long diffTx0MinerFee = tx0Preview.getTx0MinerFee() - tx0MinerFee;
      changeValueTotalA += diffTx0MinerFee/2L;
      changeValueTotalB += diffTx0MinerFee/2L;
      tx0Preview.setTx0MinerFee(tx0MinerFee);

      // recalculate & confirm spendValue. difference should be premixValue + diffTx0MinerFee
      long feeValueOrFeeChange =
        tx0Preview.getFeeValue() != 0 ? tx0Preview.getFeeValue() : tx0Preview.getFeeChange();
      long spendValue = ClientUtils.computeTx0SpendValue(
        tx0Preview.getPremixValue(), nbPremix, feeValueOrFeeChange, tx0MinerFee);
      long diffSpendValue = tx0Preview.getSpendValue() - spendValue;
      if (diffSpendValue == tx0Preview.getPremixValue() + diffTx0MinerFee) {
        // update spend value
        tx0Preview.setSpendValue(spendValue);
      } else {
        String message =
          "Miscalculated Spend Value. diffSpendValue="
          + diffSpendValue
          + "; premixValue="
          + tx0Preview.getPremixValue()
          + "; diffTx0MinerFee="
          + diffTx0MinerFee;
        if(log.isDebugEnabled()){
          log.debug(message);
        }
        throw new Exception(message);
      }

      // confirm recalculated total value
      changeValueTotal = changeValueTotalA + changeValueTotalB;
      if (tx0Preview.getTotalValue() != spendValue + changeValueTotal) {
        String message =
          "Error calculating change. totalValue="
            + tx0Preview.getTotalValue()
            + "; spendValue="
            + spendValue
            + "; changeValue="
            + changeValueTotal
            + "; feeChange="
            + tx0Preview.getFeeChange();
        if(log.isDebugEnabled()) {
          log.debug(message);
        }
        throw new Exception(message);
      }

      // recalculate tx0 size
      int tx0Size = ClientUtils.computeTx0Size(nbPremix, sortedSpendFroms.size(), params);
      tx0Preview.setTx0Size(tx0Size);

      // reduce 1 premix
      tx0Preview.decrementNbPremix();

      // reduce 1 premix miner fee
      tx0Preview.decrementMixMinerFee();
    }

    if (tx0Preview.getPool().getPoolId().equals("0.001btc")) {
      changeValueTotalA = changeValueTotal / 2L;
      changeValueTotalB = changeValueTotal / 2L;
      if (changeValueTotal % 2L == 1) {
        tx0Preview.incrementTx0MinerFee();
      }
    }
    tx0Preview.setChangeValue(changeValueTotalA + changeValueTotalB);

    return Arrays.asList(changeValueTotalA, changeValueTotalB);
  }

  // TODO: Continue to optimize...
  private Pair<Long,Long> computeSpendFromAmountsStonewall(
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      Collection<UnspentOutput> spendFroms) throws NotifiableException {
    long feeParticipation;
    long minSpendFromA;
    long minSpendFromB;
    if (tx0Config.getCascadingParent() == null) {
      // initial pool
      feeParticipation = (tx0Preview.getTx0MinerFee() + tx0Preview.getFeeValue()) / 2L; // split fees
      long minSpendFrom = feeParticipation + tx0Preview.getPremixValue(); // at least 1 must mix each
      // same minSpendFrom for A & B
      minSpendFromA = minSpendFrom;
      minSpendFromB = minSpendFrom;
    } else {
      // lower pools
      feeParticipation = tx0Preview.getTx0MinerFee() / 2L; // split miner fee
      // fee change applied to A
      minSpendFromA = feeParticipation + tx0Preview.getFeeChange() + tx0Preview.getPremixValue();
      minSpendFromB = feeParticipation + tx0Preview.getPremixValue();
    }

    // sort utxos descending to help avoid misses [see WhirlpoolWalletDeocyTx0x2.tx0x2_decoy_3utxos()]
    if (tx0Config.getCascadingParent() == null) {
      List<UnspentOutput> sortedSpendFroms = new LinkedList<>();
      sortedSpendFroms.addAll(spendFroms);
      Collections.sort(sortedSpendFroms, new SpendFromsComparator());
      spendFroms = sortedSpendFroms;
    }

    // check for min spend amount
    long spendFromA = 0L;
    long spendFromB = 0L;
    if (tx0Config.getCascadingParent() == null) {
      // initial pool: check outpoints
      HashMap<String, MyTransactionOutPoint> outPointsSetA = new HashMap<>();
      HashMap<String, MyTransactionOutPoint> outPointsSetB = new HashMap<>();
      for (UnspentOutput spendFrom : spendFroms) {
        MyTransactionOutPoint outPoint = spendFrom.computeOutpoint(params);
        String hash = outPoint.getHash().toString();

        if (spendFromA < minSpendFromA) {
          outPointsSetA.put(hash, outPoint);
          spendFromA += spendFrom.value; // must reach minSpendFrom for A
        } else if (spendFromB < minSpendFromB && !outPointsSetA.containsKey(hash)) {
          outPointsSetB.put(hash, outPoint);
          spendFromB += spendFrom.value; // must reach minSpendFrom for B
        } else {
          // random factor when minSpendFrom is reached
          if (RandomUtil.getInstance().random(0, 1) == 1) {
            if (!outPointsSetB.containsKey(hash)) {
              outPointsSetA.put(hash, outPoint);
              spendFromA += spendFrom.value;
            } else {
              outPointsSetB.put(hash, outPoint);
              spendFromB += spendFrom.value;
            }
          } else {
            if (!outPointsSetA.containsKey(hash)) {
              outPointsSetB.put(hash, outPoint);
              spendFromB += spendFrom.value;
            } else {
              outPointsSetA.put(hash, outPoint);
              spendFromA += spendFrom.value;
            }
          }
        }
      }
    } else {
      // lower pools: does not check outpoints
      for (UnspentOutput spendFrom : spendFroms) {
        if (spendFromA < minSpendFromA) {
          // must reach minSpendFrom for A
          spendFromA += spendFrom.value;
        } else if (spendFromB < minSpendFromB) {
          // must reach minSpendFrom for B
          spendFromB += spendFrom.value;
        } else {
          // random factor when minSpendFrom is reached
          if (RandomUtil.getInstance().random(0, 1) == 1) {
            spendFromA += spendFrom.value;
          } else {
            spendFromB += spendFrom.value;
          }
        }
      }
    }

    if (spendFromA < minSpendFromA || spendFromB < minSpendFromB) {
      if (log.isDebugEnabled()) {
        log.debug(
          "Stonewall is not possible for TX0 decoy change: spendFromA="
          + spendFromA
          + ", spendFromB="
          + spendFromB
          + ", minSpendFromA="
          + minSpendFromA
          + ", minSpendFromB="
          + minSpendFromB);
      }

      // if both inputs (higher pool change outputs) are not large enough for decoy tx02, skip to next lower pool
      if (tx0Config.getCascadingParent() != null) {
        throw new NotifiableException("Decoy Tx0x2 not possible for lower pool: " + tx0Preview.getPool().getPoolId());
      }

      return null;
    }

    return Pair.of(spendFromA, spendFromB);
  }

  private List<UnspentOutput> computeChangeUtxos(
      List<TransactionOutput> changeOutputs,
      List<BipAddress> changeOutputsAddresses,
      BipWallet changeWallet) {
    List<UnspentOutput> changeUtxos = new LinkedList<>();
    for (int i = 0; i < changeOutputs.size(); i++) {
      TransactionOutput changeOutput = changeOutputs.get(i);
      HD_Address changeAddress = changeOutputsAddresses.get(i).getHdAddress();
      String changeAddressBech32 = changeAddress.getAddressString();
      String path = UnspentOutput.computePath(changeAddress);
      UnspentOutput changeUtxo =
          new UnspentOutput(
              new MyTransactionOutPoint(changeOutput, changeAddressBech32, 0),
              null,
              path,
              changeWallet.getPub());
      changeUtxos.add(changeUtxo);
    }
    return changeUtxos;
  }

  public List<Tx0> tx0Cascade(
      Collection<UnspentOutput> spendFroms,
      WalletSupplier walletSupplier,
      Collection<Pool> poolsChoice,
      Tx0Config tx0Config,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    List<Tx0> tx0List = new ArrayList<>();

    // sort pools by denomination
    List<Pool> pools = new LinkedList<>(poolsChoice);
    Collections.sort(pools, new PoolComparatorByDenominationDesc());

    // initial Tx0 on highest pool
    Iterator<Pool> poolsIter = pools.iterator();
    Pool poolInitial = poolsIter.next();
    if (log.isDebugEnabled()) {
      log.debug(" +Tx0 cascading for poolId=" + poolInitial.getPoolId() + "... (1/x)");
    }
    Tx0 tx0 = tx0(spendFroms, walletSupplier, poolInitial, tx0Config, utxoKeyProvider);
    tx0List.add(tx0);
    tx0Config.setCascadingParent(tx0);
    UnspentOutput unspentOutputChange = findTx0Change(tx0);

    // decoy Tx0x2
    UnspentOutput unspentDecoyChange = null;
    Collection<UnspentOutput> decoyTx0x2Changes = new ArrayList<>();
    if (tx0Config.isDecoyTx0x2()) {
      unspentDecoyChange = findTx0Change(tx0, 1);
      decoyTx0x2Changes.add(unspentOutputChange);
      decoyTx0x2Changes.add(unspentDecoyChange);
    }

    // Tx0 cascading for remaining pools
    while (poolsIter.hasNext()) {
      Pool pool = poolsIter.next();
      if (unspentOutputChange == null) {
        break; // stop when no tx0 change
      }

      try {
        if (log.isDebugEnabled()) {
          log.debug(
              " +Tx0 cascading for poolId="
                  + pool.getPoolId()
                  + "... ("
                  + (tx0List.size() + 1)
                  + "/x)");
        }

        if (!tx0Config.isDecoyTx0x2()) {
          // normal Tx0
          tx0 =
              tx0(
                  Collections.singletonList(unspentOutputChange),
                  walletSupplier,
                  pool,
                  tx0Config,
                  utxoKeyProvider);
        } else {
          // decoy Tx0x2
          tx0 =
              tx0(
                  decoyTx0x2Changes,
                  walletSupplier,
                  pool,
                  tx0Config,
                  utxoKeyProvider);
        }
        tx0List.add(tx0);
        tx0Config.setCascadingParent(tx0);
        unspentOutputChange = findTx0Change(tx0);

        if (tx0Config.isDecoyTx0x2()) {
          decoyTx0x2Changes.clear();
          unspentDecoyChange = findTx0Change(tx0, 1);
          decoyTx0x2Changes.add(unspentOutputChange);
          decoyTx0x2Changes.add(unspentDecoyChange);
        }

      } catch (Exception e) {
        // Tx0 is not possible for this pool, ignore it
        if (log.isDebugEnabled()) {
          log.debug(
              "Tx0 cascading skipped for poolId=" + pool.getPoolId() + ": " + e.getMessage(), e);
        }
      }
    }
    return tx0List;
  }

  private UnspentOutput findTx0Change(Tx0 tx0) {
    return findTx0Change(tx0, 0);
  }

  private UnspentOutput findTx0Change(Tx0 tx0, int index) {
    if (tx0.getChangeUtxos().isEmpty()) {
      // no tx0 change
      return null;
    }

    if(index >= tx0.getChangeUtxos().size() || index < 0) {
      // index does not exists
      return null;
    }

    return tx0.getChangeUtxos().get(index);
  }

  protected void signTx0(Transaction tx, KeyBag keyBag, BipFormatSupplier bipFormatSupplier)
      throws Exception {
    SendFactoryGeneric.getInstance().signTransaction(tx, keyBag, bipFormatSupplier);
  }

  public Single<PushTxSuccessResponse> pushTx0(Tx0 tx0, WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    // push to coordinator
    String tx64 = WhirlpoolProtocol.encodeBytes(tx0.getTx().bitcoinSerialize());
    String poolId = tx0.getPool().getPoolId();
    Tx0PushRequest request = new Tx0PushRequest(tx64, poolId);
    ServerApi serverApi = whirlpoolWallet.getConfig().getServerApi();
    return serverApi
        .pushTx0(request)
        .doOnSuccess(
            pushTxSuccessResponse -> {
              // notify
              WhirlpoolEventService.getInstance().post(new Tx0Event(whirlpoolWallet, tx0));
            });
  }

  public PushTxSuccessResponse pushTx0WithRetryOnAddressReuse(
      Tx0 tx0, WhirlpoolWallet whirlpoolWallet) throws Exception {
    int tx0MaxRetry = whirlpoolWallet.getConfig().getTx0MaxRetry();

    // pushTx0 with multiple attempts on address-reuse
    Exception pushTx0Exception = null;
    for (int i = 0; i < tx0MaxRetry; i++) {
      log.info(" • Pushing Tx0: txid=" + tx0.getTx().getHashAsString());
      if (log.isDebugEnabled()) {
        log.debug(tx0.getTx().toString());
      }
      try {
        return AsyncUtil.getInstance().blockingGet(pushTx0(tx0, whirlpoolWallet));
      } catch (PushTxErrorResponseException e) {
        PushTxErrorResponse pushTxErrorResponse = e.getPushTxErrorResponse();
        log.warn(
            "tx0 failed: "
                + e.getMessage()
                + ", attempt="
                + (i + 1)
                + "/"
                + tx0MaxRetry
                + ", pushTxErrorCode="
                + pushTxErrorResponse.pushTxErrorCode);

        if (pushTxErrorResponse.voutsAddressReuse == null
            || pushTxErrorResponse.voutsAddressReuse.isEmpty()) {
          throw e; // not an address-reuse
        }

        // retry on address-reuse
        pushTx0Exception = e;
        tx0 =
            tx0Retry(
                tx0,
                pushTxErrorResponse,
                whirlpoolWallet.getWalletSupplier(),
                whirlpoolWallet.getUtxoSupplier());
      }
    }
    throw pushTx0Exception;
  }

  private Tx0 tx0Retry(
      Tx0 tx0,
      PushTxErrorResponse pushTxErrorResponse,
      WalletSupplier walletSupplier,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    // manage premix address reuses
    Collection<Integer> premixOutputIndexs = ClientUtils.getOutputIndexs(tx0.getPremixOutputs());
    boolean isPremixReuse =
        pushTxErrorResponse.voutsAddressReuse != null
            && !ClientUtils.intersect(pushTxErrorResponse.voutsAddressReuse, premixOutputIndexs)
                .isEmpty();
    if (!isPremixReuse) {
      if (log.isDebugEnabled()) {
        log.debug("isPremixReuse=false => reverting tx0 premix index");
      }
      tx0.getTx0Context().revertIndexPremix();
    }

    // manage change address reuses
    Collection<Integer> changeOutputIndexs = ClientUtils.getOutputIndexs(tx0.getChangeOutputs());
    boolean isChangeReuse =
        pushTxErrorResponse.voutsAddressReuse != null
            && !ClientUtils.intersect(pushTxErrorResponse.voutsAddressReuse, changeOutputIndexs)
                .isEmpty();

    if (!isChangeReuse) {
      if (log.isDebugEnabled()) {
        log.debug("isChangeReuse=false => reverting tx0 change index");
      }
      tx0.getTx0Context().revertIndexChange();
    }

    // rebuild a TX0 with new indexes
    return tx0(tx0.getSpendFroms(), walletSupplier, tx0.getTx0Config(), tx0, utxoKeyProvider);
  }

  public Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
  }

  public static Long getTx0ListAmount(List<Tx0> tx0List) {
    long amount = 0L;
    for (Tx0 tx0 : tx0List) {
      amount += (tx0.getSpendValue() - tx0.getTx0MinerFee());
    }
    return amount;
  }
}
