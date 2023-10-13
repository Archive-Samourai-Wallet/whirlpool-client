package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69InputComparatorUtxo;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.KeyBag;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.BipUtxoImpl;
import com.samourai.wallet.utxo.UtxoOutPoint;
import com.samourai.wallet.utxo.UtxoOutPointImpl;
import com.samourai.whirlpool.client.event.Tx0Event;
import com.samourai.whirlpool.client.exception.PushTxErrorResponseException;
import com.samourai.whirlpool.client.utils.ClientUtils;
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
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);

  private Tx0PreviewService tx0PreviewService;
  private NetworkParameters params;
  private FeeOpReturnImpl feeOpReturnImpl;
  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private final UtxoUtil utxoUtil = UtxoUtil.getInstance();
  private final TxUtil txUtil = TxUtil.getInstance();

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
  protected Optional<Tx0> buildTx0(Tx0Config tx0Config) throws Exception {
    // preview
    Tx0Preview tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0Config).orElse(null);
    if (tx0Preview != null) {
      // compute Tx0
      return buildTx0(tx0Config, tx0Preview);
    }
    return Optional.empty();
  }

  protected Optional<Tx0> buildTx0(Tx0Config tx0Config, Tx0Preview tx0Preview) throws Exception {
    if (tx0Preview.isTx0x2Cahoots()) { // tx0x2 cahoots (2-party)
      return buildTx0_tx0x2Cahoots(tx0Config, tx0Preview);
    } else if (tx0Preview.isTx0x2Decoy()) { // tx0x2 decoy
      return buildTx0_tx0x2Decoy(tx0Config, tx0Preview);
    }
    return buildTx0_regular(tx0Config, tx0Preview);
  }

  // regular tx0
  protected Optional<Tx0> buildTx0_regular(Tx0Config tx0Config, Tx0Preview tx0Preview)
      throws Exception {
    if (log.isDebugEnabled()) {
      String poolId = tx0Config.getPool().getPoolId();
      log.debug(
          " • Tx0["
              + poolId
              + "][noDecoy]: tx0Config={"
              + tx0Config
              + "}\n=> tx0Preview={"
              + tx0Preview);
    }

    int nbPremixs = tx0Preview.getNbPremix();

    // inputs
    Collection<UtxoOutPoint> inputs =
        (Collection<UtxoOutPoint>)
            (Collection<? extends UtxoOutPoint>) tx0Config.getOwnSpendFromUtxos();

    // change outputs
    BipWallet changeWallet = tx0Config.getChangeWallet();
    Pair<TransactionOutput, BipAddress> ownChange = null;
    List<TransactionOutput> changeOutputs = new LinkedList<>();
    if (tx0Preview.getChangeValue() > 0) {
      ownChange = computeOwnChangeOutput(tx0Preview.getChangeValue(), changeWallet, "");
      changeOutputs.add(ownChange.getLeft());
    }

    Optional<Tx0> tx0Opt = buildTx0(tx0Config, tx0Preview, nbPremixs, inputs, changeOutputs, true);
    if (tx0Opt.isPresent()) {
      Tx0 tx0 = tx0Opt.get();

      if (tx0Config.isCascade()) {
        // set cascading utxos
        List<BipUtxo> cascadingChangeUtxos = new LinkedList<>();
        if (ownChange != null) {
          cascadingChangeUtxos.add(computeOwnChangeUtxo(ownChange, changeWallet));
        }
        tx0._setCascadingChangeUtxos(cascadingChangeUtxos);
      }
    }
    return tx0Opt;
  }

  // tx0x2 decoy
  protected Optional<Tx0> buildTx0_tx0x2Decoy(Tx0Config tx0Config, Tx0Preview tx0Preview)
      throws Exception {
    if (log.isDebugEnabled()) {
      String poolId = tx0Config.getPool().getPoolId();
      log.debug(
          " • Tx0["
              + poolId
              + "][Tx0x2Decoy]: tx0Config={"
              + tx0Config
              + "}\n=> tx0Preview={"
              + tx0Preview);
    }
    Tx0x2Preview tx0x2Preview = tx0Preview.getTx0x2Preview();

    // INPUTS: sender inputs
    Collection<UtxoOutPoint> inputs = new LinkedList<>(tx0Config.getOwnSpendFromUtxos());

    // OUTPUTS

    Collection<TransactionOutput> changeOutputs = new LinkedList<>();
    BipWallet changeWallet = tx0Config.getChangeWallet();
    // sender changes
    Pair<TransactionOutput, BipAddress> ownChangeSender = null;
    if (tx0x2Preview.getChangeAmountSender() > 0) {
      ownChangeSender =
          computeOwnChangeOutput(tx0x2Preview.getChangeAmountSender(), changeWallet, "(sender)");
      changeOutputs.add(ownChangeSender.getLeft());
    }
    // counterparty changes
    Pair<TransactionOutput, BipAddress> ownChangeCounterparty = null;
    if (tx0x2Preview.isTx0x2Decoy()) {
      if (tx0x2Preview.getChangeAmountCounterparty() > 0) {
        ownChangeCounterparty =
            computeOwnChangeOutput(
                tx0x2Preview.getChangeAmountCounterparty(), changeWallet, "(counterparty)");
        changeOutputs.add(ownChangeCounterparty.getLeft());
      }
    }

    // premixs: sender + counterparty
    int nbPremixs = tx0x2Preview.getNbPremixSender() + tx0x2Preview.getNbPremixCounterparty();

    // build TX0 & sign
    Optional<Tx0> tx0Opt = buildTx0(tx0Config, tx0Preview, nbPremixs, inputs, changeOutputs, true);

    // build change utxos after tx is built
    if (tx0Opt.isPresent()) {
      Tx0 tx0 = tx0Opt.get();
      List<BipUtxo> cascadingChangeUtxos = new LinkedList<>();

      if (tx0.isTx0x2Decoy()) {
        // sender change
        BipUtxo senderChangeUtxo = null;
        if (ownChangeSender != null) {
          senderChangeUtxo = computeOwnChangeUtxo(ownChangeSender, changeWallet);
          cascadingChangeUtxos.add(senderChangeUtxo);
        }

        // counterparty change
        BipUtxo counterpartyChangeUtxo = null;
        if (ownChangeCounterparty != null) {
          counterpartyChangeUtxo = computeOwnChangeUtxo(ownChangeCounterparty, changeWallet);
          cascadingChangeUtxos.add(counterpartyChangeUtxo);
        }

        // set Tx0x2DecoyResult
        Tx0x2DecoyResult tx0x2DecoyResult =
            new Tx0x2DecoyResult(tx0x2Preview, senderChangeUtxo, counterpartyChangeUtxo);
        tx0._setTx0x2DecoyResult(tx0x2DecoyResult);
      }
      if (tx0Config.isCascade()) {
        tx0._setCascadingChangeUtxos(cascadingChangeUtxos);
      }
    }
    return tx0Opt;
  }

  // tx0x2 Cahoots (2-party)
  protected Optional<Tx0> buildTx0_tx0x2Cahoots(Tx0Config tx0Config, Tx0Preview tx0Preview)
      throws Exception {
    if (log.isDebugEnabled()) {
      String poolId = tx0Config.getPool().getPoolId();
      log.debug(
          " • Tx0["
              + poolId
              + "][Tx0x2Cahoots]: tx0Config={"
              + tx0Config
              + "}\n=> tx0Preview={"
              + tx0Preview);
    }
    Tx0x2Preview tx0x2Preview = tx0Preview.getTx0x2Preview();
    Tx0x2CahootsConfig tx0x2CahootsConfig = tx0Config.getTx0x2CahootsConfig();

    // INPUTS: sender + counterparty
    Collection<UtxoOutPoint> inputs = new LinkedList<>();
    inputs.addAll(tx0Config.getOwnSpendFromUtxos());
    inputs.addAll(tx0x2CahootsConfig.getCounterpartyInputs());

    // OUTPUTS
    Collection<TransactionOutput> changeOutputs = new LinkedList<>();
    BipWallet changeWallet = tx0Config.getChangeWallet();
    // sender changes
    Pair<TransactionOutput, BipAddress> ownChangeSender = null;
    if (tx0x2Preview.getChangeAmountSender() > 0) {
      ownChangeSender =
          computeOwnChangeOutput(tx0x2Preview.getChangeAmountSender(), changeWallet, "(sender)");
      changeOutputs.add(ownChangeSender.getLeft());
    }

    // counterparty change
    Pair<TransactionOutput, String> tx0x2CounterpartyChange = null;
    long changeAmount = tx0x2Preview.getChangeAmountCounterparty();
    if (changeAmount > 0) {
      // add counterarty change output
      String poolId = tx0Config.getPool().getPoolId();
      String changeAddressBech32 =
          tx0x2CahootsConfig.getCounterpartyChangeAddressPerPoolId().get(poolId);
      TransactionOutput changeOutput =
          bech32Util.getTransactionOutput(changeAddressBech32, changeAmount, params);
      changeOutputs.add(changeOutput);
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx0 out (change)(counterparty): address="
                + changeAddressBech32
                + ", ("
                + changeAmount
                + " sats)");
      }
      tx0x2CounterpartyChange = Pair.of(changeOutput, changeAddressBech32);
    }

    // premixs: only sender (counterparty premixs will be added later on step3)
    int nbPremixs = tx0x2Preview.getNbPremixSender();

    // build TX0 without signing yet (will sign later once tx updated by counterparty)
    Optional<Tx0> tx0Opt = buildTx0(tx0Config, tx0Preview, nbPremixs, inputs, changeOutputs, false);

    // build change utxos after tx is built
    if (tx0Opt.isPresent()) {
      Tx0 tx0 = tx0Opt.get();
      List<BipUtxo> cascadingChangeUtxos = new LinkedList<>();

      // sender change
      BipUtxo senderChangeUtxo = null;
      if (ownChangeSender != null) {
        senderChangeUtxo = computeOwnChangeUtxo(ownChangeSender, changeWallet);
        cascadingChangeUtxos.add(senderChangeUtxo);
      }

      // counterparty change
      UtxoOutPoint counterpartyChangeOutPoint = null;
      if (tx0x2CounterpartyChange != null) {
        counterpartyChangeOutPoint =
            new UtxoOutPointImpl(
                tx0x2CounterpartyChange.getLeft(), tx0x2CounterpartyChange.getRight());
      }

      // set Tx0x2CahootsResult
      Tx0x2CahootsResult tx0x2CahootsResult =
          new Tx0x2CahootsResult(tx0x2Preview, senderChangeUtxo, counterpartyChangeOutPoint);
      tx0._setTx0x2CahootsResult(tx0x2CahootsResult);

      if (tx0Config.isCascade()) {
        tx0._setCascadingChangeUtxos(cascadingChangeUtxos);
      }
    }
    return tx0Opt;
  }

  protected Optional<Tx0> buildTx0(
      Tx0Config tx0Config,
      Tx0Preview tx0Preview,
      int nbPremixs,
      Collection<UtxoOutPoint> inputs,
      Collection<TransactionOutput> changeOutputs,
      boolean sign)
      throws Exception {
    String poolId = tx0Preview.getPool().getPoolId();
    long premixValue = tx0Preview.getPremixValue();
    long feeValueOrFeeChange = tx0Preview.getTx0Data().computeFeeValueOrFeeChange();
    int nbPremix =
        tx0PreviewService.capNbPremix(tx0Preview.getNbPremix(), tx0Preview.getPool(), false);

    // verify

    if (tx0Config.getOwnSpendFromUtxos().size() <= 0) {
      throw new IllegalArgumentException("spendFroms should be > 0");
    }

    if (feeValueOrFeeChange <= 0) {
      throw new IllegalArgumentException("feeValueOrFeeChange should be > 0");
    }

    // at least 1 premix
    if (nbPremix < 1) {
      if (log.isDebugEnabled()) {
        log.debug("Invalid nbPremix=" + nbPremix);
      }
      return Optional.empty(); // TX0 not possible
    }

    // save indexes state with Tx0Context
    BipWallet premixWallet = tx0Config.getPremixWallet();
    BipWallet changeWallet = tx0Config.getChangeWallet();
    Tx0Context tx0Context = new Tx0Context(premixWallet, changeWallet); // save index state

    Tx0Data tx0Data = tx0Preview.getTx0Data();

    // compute fee destination
    String feeOrBackAddressBech32;
    if (tx0Data.getFeeValue() > 0) {
      // pay to fee
      feeOrBackAddressBech32 = tx0Data.getFeeAddress();
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: samourai => " + feeOrBackAddressBech32);
      }
    } else {
      // pay to deposit
      BipWallet feeChangeWallet = tx0Config.getFeeChangeWallet();
      BipAddress bipAddress = feeChangeWallet.getNextAddressChange();
      feeOrBackAddressBech32 = bipAddress.getAddressString();
      if (log.isDebugEnabled()) {
        log.debug("feeAddressDestination: back as change => " + bipAddress);
      }
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
    for (int i = 0; i < nbPremixs; i++) {
      TransactionOutput premixOutput = computeOwnPremixOutput(premixValue, premixWallet).getLeft();
      premixOutputs.add(premixOutput);
      outputs.add(premixOutput);
    }

    //
    // Own changes outputs (back to own change wallet)
    // 1 change output(s) [Tx0 or Cahoots Tx0x2]
    // 2 change outputs [decoy Tx0x2]
    //
    List<TransactionOutput> allChangeOutputs = new LinkedList<>();
    for (TransactionOutput changeOutput : changeOutputs) {
      outputs.add(changeOutput);
      allChangeOutputs.add(changeOutput);
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

    // add inputs
    for (UtxoOutPoint spendFrom : inputs) {
      TransactionInput input = utxoUtil.computeSpendInput(spendFrom, params);
      tx.addInput(input);
      if (log.isDebugEnabled()) {
        log.debug("Tx0 in: utxo=" + spendFrom);
      }
    }

    // sort inputs to find the first input
    List<UtxoOutPoint> sortedInputs = new LinkedList<>(inputs);
    Collections.sort(sortedInputs, new BIP69InputComparatorUtxo());
    UtxoOutPoint firstInput = sortedInputs.get(0);

    // add OP_RETURN output
    TransactionOutput opReturnOutput = computeOpReturnOutput(firstInput, tx0Config, tx0Preview);
    outputs.add(opReturnOutput);

    // all outputs
    for (TransactionOutput to : outputs) {
      tx.addOutput(to);
    }

    // normalize tx
    txUtil.sortBip69InputsAndOutputs(tx);

    // prepare keybag to sign
    UtxoKeyProvider keyProvider = tx0Config.getUtxoKeyProvider();
    KeyBag keyBag = new KeyBag();
    keyBag.addAll((Collection<BipUtxo>) tx0Config.getOwnSpendFromUtxos(), keyProvider);

    // sign
    if (sign) {
      signTx0(tx, keyBag, keyProvider.getBipFormatSupplier());
    }
    tx.verify();

    Tx0 tx0 =
        new Tx0(
            tx0Preview,
            inputs,
            tx0Config.getOwnSpendFromUtxos(),
            tx0Config,
            tx0Context,
            tx,
            premixOutputs,
            allChangeOutputs,
            opReturnOutput,
            samouraiFeeOutput,
            keyBag,
            sign);
    log.info(
        " • Tx0["
            + poolId
            + "]: txid="
            + tx0.getTx().getHashAsString()
            + ", nbOwnPremixs="
            + tx0.getOwnPremixOutputs().size()
            + ", tx0x2="
            + tx0Preview.isTx0x2Any());
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0["
              + poolId
              + "]: size="
              + tx.getVirtualTransactionSize()
              + "b, feePrice="
              + tx0.getTx0MinerFeePrice()
              + "s/b"
              + "\nhex="
              + txUtil.getTxHex(tx)
              + "\ntx0={"
              + tx0
              + "}");
    }
    return Optional.of(tx0);
  }

  protected TransactionOutput computeOpReturnOutput(
      UtxoOutPoint firstInput, Tx0Config tx0Config, Tx0Preview tx0Preview) throws Exception {
    BipUtxo ownFirstInput =
        tx0Config.getOwnSpendFromUtxos().stream()
            .filter(u -> utxoUtil.utxoToKey(u).equals(utxoUtil.utxoToKey(firstInput)))
            .findFirst()
            .orElse(null);
    byte[] opReturn;
    if (ownFirstInput != null) {
      // encode opReturn with own first input
      byte[] firstInputKey = tx0Config.getUtxoKeyProvider()._getPrivKey(ownFirstInput);
      Tx0Data tx0Data = tx0Preview.getTx0Data();
      opReturn =
          computeOpReturn(
              ownFirstInput, firstInputKey, tx0Data.getFeePaymentCode(), tx0Data.getFeePayload());
      if (log.isDebugEnabled()) {
        log.debug("Using own OP_RETURN");
      }
    } else {
      if (!tx0Preview.isTx0x2Cahoots()) {
        throw new Exception("ownFirstInput not found: " + firstInput);
      }
      if (log.isDebugEnabled()) {
        log.debug("Using OP_RETURN from COUNTERPARTY");
      }
      // first input is counterparty's one
      // temporarily use a blank opReturn of same size (to preserve tx0 minerFees)
      // which will be replaced by counterparty later
      opReturn = new byte[feeOpReturnImpl.getOpReturnLength()];
    }

    // build OP_RETURN output
    return computeOpReturnOutput(opReturn);
  }

  public TransactionOutput computeOpReturnOutput(byte[] opReturn) {
    // add OP_RETURN output
    Script op_returnOutputScript =
        new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturn).build();
    TransactionOutput opReturnOutput =
        new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (OP_RETURN): " + opReturn.length + " bytes");
    }
    return opReturnOutput;
  }

  public Pair<TransactionOutput, BipAddress> computeOwnPremixOutput(
      long amount, BipWallet premixWallet) throws Exception {
    BipAddress toAddress = premixWallet.getNextAddressReceive();
    String toAddressBech32 = toAddress.getAddressString();
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0 out (own premix): address="
              + toAddressBech32
              + ", path="
              + toAddress.getPathAddress()
              + " ("
              + amount
              + " sats)");
    }
    TransactionOutput txOut = bech32Util.getTransactionOutput(toAddressBech32, amount, params);
    return Pair.of(txOut, toAddress);
  }

  public Pair<TransactionOutput, BipAddress> computeOwnChangeOutput(
      long changeAmount, BipWallet changeWallet, String debugInfo) throws Exception {
    BipAddress changeAddress = changeWallet.getNextAddressChange();
    String changeAddressBech32 = changeAddress.getAddressString();
    TransactionOutput changeOutput =
        bech32Util.getTransactionOutput(changeAddressBech32, changeAmount, params);
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0 out (change)"
              + debugInfo
              + ": address="
              + changeAddressBech32
              + ", path="
              + changeAddress.getPathAddress()
              + " ("
              + changeAmount
              + " sats)");
    }
    return Pair.of(changeOutput, changeAddress);
  }

  public byte[] computeOpReturn(
      UtxoOutPoint firstInputOutPoint,
      byte[] firstInputKey,
      String feePaymentCode,
      byte[] feePayload)
      throws Exception {
    // use input0 for masking
    TransactionOutPoint maskingOutpoint = new MyTransactionOutPoint(firstInputOutPoint, params, 0);
    return feeOpReturnImpl.computeOpReturn(
        feePaymentCode, feePayload, maskingOutpoint, firstInputKey);
  }

  private BipUtxo computeOwnChangeUtxo(
      Pair<TransactionOutput, BipAddress> ownChange, BipWallet changeWallet) {
    TransactionOutput changeOutput = ownChange.getLeft();
    BipAddress bipAddress = ownChange.getRight();
    return new BipUtxoImpl(
        changeOutput,
        bipAddress.getAddressString(),
        null,
        changeWallet.getXPub(),
        false,
        bipAddress.getHdAddress().getChainIndex(),
        bipAddress.getHdAddress().getAddressIndex());
  }

  public Optional<Tx0Result> tx0(Tx0Config tx0Config) throws Exception {
    List<Tx0> tx0List = new ArrayList<>();

    // initial Tx0 on highest pool
    Pool poolInitial = tx0Config.getPool();
    if (log.isDebugEnabled()) {
      if (tx0Config.isCascade()) {
        log.debug(" • Tx0 cascading (1/x): trying poolId=" + poolInitial.getPoolId());
      } else {
        log.debug(" • Tx0: poolId=" + poolInitial.getPoolId());
      }
    }
    Tx0 tx0 = buildTx0(tx0Config).orElse(null);
    if (tx0 == null) {
      return Optional.empty();
    }
    tx0List.add(tx0);
    Tx0 higherPoolTx0 = tx0;

    // Tx0 cascading for remaining pools
    if (tx0Config.isCascade()) {
      // sort pools by denomination
      List<Pool> cascadingPools = tx0PreviewService.findCascadingPools(poolInitial.getPoolId());
      Collections.sort(cascadingPools, new PoolComparatorByDenominationDesc());

      for (Pool pool : cascadingPools) {
        Collection<BipUtxo> higherTx0CascadingChangeUtxos = higherPoolTx0.getCascadingChangeUtxos();
        if (higherTx0CascadingChangeUtxos.isEmpty()) {
          break; // stop when no tx0 change
        }

        if (log.isDebugEnabled()) {
          log.debug(
              " • Tx0 cascading ("
                  + (tx0List.size() + 1)
                  + "/x): trying poolId="
                  + pool.getPoolId());
        }

        tx0Config = new Tx0Config(tx0Config, higherTx0CascadingChangeUtxos, pool);
        tx0Config._setCascading(true);
        tx0Config.setTx0x2DecoyForced(true); // skip to next lower pool when decoy is not possible
        Tx0x2CahootsConfig tx0x2CahootsConfig = tx0Config.getTx0x2CahootsConfig();
        if (tx0x2CahootsConfig != null) {
          List<UtxoOutPoint> counterpartyInputsLower = new LinkedList<>();
          UtxoOutPoint counterpartyChangeOutPoint =
              higherPoolTx0.getTx0x2CahootsResult().getCounterpartyChangeOutPoint();
          if (counterpartyChangeOutPoint != null) {
            counterpartyInputsLower.add(counterpartyChangeOutPoint);
          }
          Tx0x2CahootsConfig tx0x2CahootsConfigLower =
              new Tx0x2CahootsConfig(
                  counterpartyInputsLower,
                  tx0x2CahootsConfig.getCounterpartyChangeAddressPerPoolId());
          tx0Config.setTx0x2CahootsConfig(tx0x2CahootsConfigLower);
        }
        tx0 = this.buildTx0(tx0Config).orElse(null);
        if (tx0 != null) {
          tx0List.add(tx0);
          higherPoolTx0 = tx0;
        } else {
          // Tx0 is not possible for this pool, skip to next lower pool
        }
      }
    }
    List<String> poolIds =
        tx0List.stream()
            .map(t -> t.getPool().getPoolId() + "(" + t.getNbPremix() + ")")
            .collect(Collectors.toList());
    log.info(
        " • Tx0 success on "
            + tx0List.size()
            + " pool"
            + (tx0List.size() > 1 ? "s" : "")
            + ": "
            + StringUtils.join(poolIds, "->"));
    return Optional.of(new Tx0Result(tx0List));
  }

  protected void signTx0(Transaction tx, KeyBag keyBag, BipFormatSupplier bipFormatSupplier)
      throws Exception {
    SendFactoryGeneric.getInstance().signTransaction(tx, keyBag, bipFormatSupplier);
  }

  public Single<PushTxSuccessResponse> pushTx0(Tx0 tx0, WhirlpoolWallet whirlpoolWallet)
      throws Exception {
    // push to coordinator
    Transaction tx = tx0.getTx();
    String poolId = tx0.getPool().getPoolId();
    ServerApi serverApi = whirlpoolWallet.getConfig().getServerApi();
    return pushTx0(tx, poolId, serverApi)
        .doOnSuccess(
            pushTxSuccessResponse -> {
              // notify
              WhirlpoolEventService.getInstance().post(new Tx0Event(whirlpoolWallet, tx0));
            });
  }

  public Single<PushTxSuccessResponse> pushTx0(Transaction tx, String poolId, ServerApi serverApi)
      throws Exception {
    // push to coordinator
    String tx64 = WhirlpoolProtocol.encodeBytes(tx.bitcoinSerialize());
    Tx0PushRequest request = new Tx0PushRequest(tx64, poolId);
    return serverApi.pushTx0(request);
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
        tx0 = tx0Retry(tx0, pushTxErrorResponse).get();
      }
    }
    throw pushTx0Exception;
  }

  private Optional<Tx0> tx0Retry(Tx0 tx0, PushTxErrorResponse pushTxErrorResponse)
      throws Exception {
    // manage premix address reuses
    Collection<Integer> premixOutputIndexs = ClientUtils.getOutputIndexs(tx0.getOwnPremixOutputs());
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
    return buildTx0(tx0.getTx0Config(), tx0);
  }

  public Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
  }

  public NetworkParameters getParams() {
    return params;
  }
}
