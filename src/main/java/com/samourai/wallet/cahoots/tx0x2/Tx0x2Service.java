package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.AbstractCahoots2xService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsUtxo;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;
import java.util.*;
import java.util.stream.Collectors;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Service extends AbstractCahoots2xService<Tx0x2, Tx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Service.class);
  public static final long CHANGE_SPLIT_THRESHOLD = 100000; // lowest pool denomination (0.001btc)

  public Tx0x2Service(BipFormatSupplier bipFormatSupplier, NetworkParameters params) {
    super(CahootsType.TX0X2, bipFormatSupplier, params);
  }

  //
  // sender: step 0
  //
  @Override
  public Tx0x2 startInitiator(Tx0x2Context cahootsContext) throws Exception {
    Tx0 tx0Initiator = cahootsContext.getTx0Initiator();
    String poolId = tx0Initiator.getPool().getPoolId();
    long premixValue = tx0Initiator.getPremixValue();
    int maxOutputsEach = (int) Math.floor(tx0Initiator.getPool().getTx0MaxOutputs() / 2);
    long samouraiFeeValueEach = (int) Math.floor(tx0Initiator.getFeeValue() / 2);
    byte[] fingerprint = cahootsContext.getCahootsWallet().getFingerprint();
    Tx0x2 payload0 =
        new Tx0x2(params, fingerprint, poolId, premixValue, maxOutputsEach, samouraiFeeValueEach);

    if (log.isDebugEnabled()) {
      log.debug("# Tx0x2 INITIATOR => step=" + payload0.getStep());
    }
    return payload0;
  }

  //
  // counterparty: step 1
  //
  @Override
  public Tx0x2 startCollaborator(Tx0x2Context cahootsContext, Tx0x2 tx0x20) throws Exception {
    Tx0x2 tx0x21 = doStep1Initial(tx0x20, cahootsContext);
    if (log.isDebugEnabled()) {
      log.debug("# Tx0x2 COUNTERPARTY => step=" + tx0x21.getStep());
    }
    return tx0x21;
  }

  @Override
  public Tx0x2 reply(Tx0x2Context cahootsContext, Tx0x2 cahoots) throws Exception {
    int step = cahoots.getStep();
    if (log.isDebugEnabled()) {
      log.debug("# Tx0x2 " + cahootsContext.getTypeUser() + " <= step=" + step);
    }
    Tx0x2 payload;
    switch (step) {
      case 1:
        // sender
        payload = doStep2Initial(cahoots, cahootsContext);
        break;
      case 2:
        // counterparty
        payload = doStep3(cahoots, cahootsContext);
        break;
      case 3:
        // sender
        payload = doStep4(cahoots, cahootsContext);
        break;
      default:
        throw new Exception("Unrecognized #Cahoots step");
    }
    if (payload == null) {
      throw new Exception("Cannot compose #Cahoots");
    }
    if (log.isDebugEnabled()) {
      log.debug("# Tx0x2 " + cahootsContext.getTypeUser() + " => step=" + payload.getStep());
    }
    return payload;
  }

  //
  // counterparty: handles initial pool
  //
  protected Tx0x2 doStep1Initial(Tx0x2 payload0, Tx0x2Context cahootsContext) throws Exception {
    // counterparty inputs
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    int account = cahootsContext.getAccount();
    List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(account);
    long spendMin = payload0.getPremixValue() + payload0.getSamouraiFeeValueEach();
    long spendTarget =
        payload0.getPremixValue() * payload0.getMaxOutputsEach()
            + payload0.getSamouraiFeeValueEach();
    List<CahootsUtxo> counterpartyInputs = contributeInputs(utxos, spendMin, spendTarget);
    long samouraiFeeValueEach =
        payload0.getSamouraiFeeValueEach(); // pay samourai fee for initial pool

    return doStep1(payload0, cahootsContext, counterpartyInputs, samouraiFeeValueEach);
  }

  //
  // counterparty: handles lower pools
  //
  protected Tx0x2 doStep1Cascading(
      Tx0x2 payload0, Tx0x2Context cahootsContext, TransactionOutput higherPoolChange)
      throws Exception {
    // counterparty inputs (higher pool change output)
    List<CahootsUtxo> counterpartyInputs =
        Arrays.asList(toCahootsUtxo(cahootsContext, higherPoolChange));
    long samouraiFeeValueEach = 0; // no samourai fee for lower pools

    return doStep1(payload0, cahootsContext, counterpartyInputs, samouraiFeeValueEach);
  }

  protected Tx0x2 doStep1(
      Tx0x2 payload0,
      Tx0x2Context cahootsContext,
      List<CahootsUtxo> counterpartyInputs,
      long samouraiFeeValueEach)
      throws Exception {
    Tx0x2 payload1 = payload0.copy();
    payload1.setCounterpartyAccount(cahootsContext.getAccount());
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    byte[] fingerprint = cahootsWallet.getFingerprint();
    payload1.setFingerprintCollab(fingerprint);

    // add counterparty inputs
    List<TransactionInput> inputs = cahootsContext.addInputs(counterpartyInputs);

    // add counterparty premix outputs
    long myInputsSum = CahootsUtxo.sumValue(counterpartyInputs).longValue();
    int nbPremixs =
        (int)
            Math.min(
                (myInputsSum - samouraiFeeValueEach) / payload0.getPremixValue(),
                payload0.getMaxOutputsEach());
    List<TransactionOutput> outputs = contributePremixOutputs(payload0, cahootsContext, nbPremixs);
    long myPremixOutputsSum = outputs.size() * payload0.getPremixValue();

    // add counterparty change output
    long changeAmount =
        myInputsSum - myPremixOutputsSum - samouraiFeeValueEach; // not including minerFee yet
    if (changeAmount > 0) {
      BipAddress changeAddress =
          cahootsWallet.fetchAddressChange(
              payload0.getCounterpartyAccount(), true, BIP_FORMAT.SEGWIT_NATIVE);
      if (log.isDebugEnabled()) {
        log.debug("+output (CounterParty change) = " + changeAddress + ", value=" + changeAmount);
      }
      TransactionOutput changeOutput = computeTxOutput(changeAddress, changeAmount, cahootsContext);
      payload1.setCollabChange(changeAddress.getAddressString());
      outputs.add(changeOutput);
    }

    payload1.doStep1(inputs, outputs, cahootsWallet.getChainSupplier());
    return payload1;
  }

  private List<CahootsUtxo> contributeInputs(
      List<CahootsUtxo> utxos, long spendMin, long spendTarget) throws Exception {
    long totalBalance = CahootsUtxo.sumValue(utxos).longValue();

    if (totalBalance < spendMin) {
      throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
    }

    if (totalBalance <= spendTarget) {
      return utxos; // use whole balance
    }

    // select random utxo-set >= spendTarget, prefer only one utxo when possible
    List<CahootsUtxo> selectedUTXOs = new ArrayList<CahootsUtxo>();
    long sumSelectedUTXOs = 0;

    RandomUtil.getInstance().shuffle(utxos);

    for (CahootsUtxo utxo : utxos) {
      long utxoValue = utxo.getOutpoint().getValue().longValue();
      if (utxoValue >= spendTarget) {
        // select single utxo
        List<CahootsUtxo> singleSelectedUTXO = new ArrayList<CahootsUtxo>();
        singleSelectedUTXO.add(utxo);
        return singleSelectedUTXO;
      } else if (sumSelectedUTXOs < spendTarget) {
        // add utxos until target reached
        selectedUTXOs.add(utxo);
        sumSelectedUTXOs += utxoValue;
      }
    }
    return selectedUTXOs;
  }

  private List<TransactionOutput> contributePremixOutputs(
      Tx0x2 payload, Tx0x2Context cahootsContext, int nbPremixs) throws Exception {
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();

    List<TransactionOutput> outputs = new ArrayList<>();
    for (int i = 0; i < nbPremixs; i++) {
      BipAddress changeAddress =
          cahootsWallet.fetchAddressReceive(
              SamouraiAccountIndex.PREMIX, true, BIP_FORMAT.SEGWIT_NATIVE);
      if (log.isDebugEnabled()) {
        log.debug("+output (CounterParty premix) = " + changeAddress);
      }
      TransactionOutput changeOutput =
          computeTxOutput(changeAddress, payload.getPremixValue(), cahootsContext);
      outputs.add(changeOutput);
    }
    return outputs;
  }

  //
  // sender: handles initial pool
  //
  protected Tx0x2 doStep2Initial(Tx0x2 payload1, Tx0x2Context cahootsContext) throws Exception {
    Tx0 tx0Initiator = cahootsContext.getTx0Initiator();
    List<CahootsUtxo> counterpartyInputs =
        toCahootsUtxos(tx0Initiator.getSpendFroms(), cahootsContext);
    long samouraiFeeValueEach = payload1.getSamouraiFeeValueEach();

    return doStep2(payload1, cahootsContext, counterpartyInputs, samouraiFeeValueEach, null, 0);
  }

  //
  // sender: handles lower pools
  //
  protected Tx0x2 doStep2Cascading(
      Tx0x2 payload1,
      Tx0x2Context cahootsContext,
      TransactionOutput higherPoolSenderChange,
      TransactionOutput higherPoolCounterpartyChange,
      long higherPoolMinerFee)
      throws Exception {

    Tx0 tx0Initiator = cahootsContext.getTx0Initiator();
    List<CahootsUtxo> counterpartyInputs =
        Arrays.asList(toCahootsUtxo(cahootsContext, higherPoolSenderChange));
    long samouraiFeeValueEach = tx0Initiator.getSamouraiFeeOutput().getValue().getValue();

    return doStep2(
        payload1,
        cahootsContext,
        counterpartyInputs,
        samouraiFeeValueEach,
        higherPoolCounterpartyChange,
        higherPoolMinerFee);
  }

  protected Tx0x2 doStep2(
      Tx0x2 payload1,
      Tx0x2Context cahootsContext,
      List<CahootsUtxo> counterpartyInputs,
      long samouraiFeeValueEach,
      TransactionOutput higherPoolCounterpartyChangeOrNull,
      long higherPoolMinerFee)
      throws Exception {
    Tx0x2 payload2 = payload1.copy();
    Tx0 tx0Initiator = cahootsContext.getTx0Initiator();
    int nbPremixSender = Math.min(tx0Initiator.getNbPremix(), payload2.getMaxOutputsEach());

    // compute minerFee
    long fee = computeMinerFee(payload2, tx0Initiator);
    payload2.setFeeAmount(fee);

    // keep track of minerFeePaid
    long minerFeePaid = fee / 2L;
    cahootsContext.setMinerFeePaid(minerFeePaid); // sender & counterparty pay half minerFee

    if (higherPoolCounterpartyChangeOrNull != null) {
      // update counterparty input with deduced split miner fee paid (higher pool counterparty
      // change
      // output)
      Transaction tx = payload1.getTransaction();
      TransactionInput counterpartyChangeInput = tx.getInput(0);
      counterpartyChangeInput.setValue(higherPoolCounterpartyChangeOrNull.getValue());
      TransactionOutPoint outpoint = counterpartyChangeInput.getOutpoint();
      outpoint.setValue(higherPoolCounterpartyChangeOrNull.getValue());
      payload2
          .getOutpoints()
          .put(
              outpoint.getHash().toString() + "-" + outpoint.getIndex(),
              higherPoolCounterpartyChangeOrNull.getValue().value);
    }

    // add sender inputs
    List<TransactionInput> inputs = cahootsContext.addInputs(counterpartyInputs);

    // add sender outputs
    List<TransactionOutput> outputs = new ArrayList<>();
    int maxOutputsEach = payload2.getMaxOutputsEach();

    // add OP_RETURN output
    TransactionOutput opReturnOutput = tx0Initiator.getOpReturnOutput();
    outputs.add(opReturnOutput);

    // add samourai fee output
    TransactionOutput samouraiFeeOutput = tx0Initiator.getSamouraiFeeOutput();
    outputs.add(samouraiFeeOutput);

    // add sender premix outputs (limit to maxOutputsEach)
    contributeSenderPremixOutputs(cahootsContext, tx0Initiator, outputs, maxOutputsEach);

    // add sender change output
    // (senderInputsSum - senderPremixOutputsSum - samouraiFeeValueEach - minerFeePaid)
    long senderInputsSum = CahootsUtxo.sumValue(counterpartyInputs).longValue();
    long senderPremixOutputsSum = payload2.getPremixValue() * nbPremixSender;
    long senderChangeValue =
        senderInputsSum - senderPremixOutputsSum - samouraiFeeValueEach - minerFeePaid;

    // if sender change large enough, add another premix output (for cascading tx0)
    if (higherPoolCounterpartyChangeOrNull != null
        && senderChangeValue > payload2.getPremixValue()
        && nbPremixSender <= maxOutputsEach) {
      BipAddress changeAddress =
          cahootsContext
              .getCahootsWallet()
              .fetchAddressReceive(SamouraiAccountIndex.PREMIX, true, BIP_FORMAT.SEGWIT_NATIVE);
      if (log.isDebugEnabled()) {
        log.debug("+output (Sender premix) = " + changeAddress);
      }
      TransactionOutput changeOutput =
          computeTxOutput(changeAddress, payload2.getPremixValue(), cahootsContext);
      outputs.add(changeOutput);

      senderChangeValue -= payload2.getPremixValue();
      nbPremixSender++;
    }
    if (senderChangeValue < 0) {
      return null; // negative change detected
    }

    // use changeAddress from tx0Initiator to avoid index gap
    String senderChangeAddress =
        getBipFormatSupplier().getToAddress(tx0Initiator.getChangeOutputs().iterator().next());

    // update counterparty change output to deduce minerFeePaid
    Transaction tx = payload1.getTransaction();
    String collabChangeAddress = payload1.getCollabChange();
    TransactionOutput counterpartyChangeOutput =
        TxUtil.getInstance().findOutputByAddress(tx, collabChangeAddress, getBipFormatSupplier());
    if (counterpartyChangeOutput == null) {
      throw new Exception("Cannot compose #Cahoots: counterpartyChangeOutput not found");
    }

    // counterparty pays half of fees
    long counterpartyChange = counterpartyChangeOutput.getValue().longValue();
    Coin counterpartyChangeValue =
        Coin.valueOf(
            counterpartyChange
                - minerFeePaid
                - higherPoolMinerFee // running total of higher pool fees that needs to be included
            );
    if (log.isDebugEnabled()) {
      log.debug(
          "counterparty change output value:"
              + counterpartyChange
              + "; minerFeePaid: "
              + minerFeePaid
              + "; change value post fee:"
              + counterpartyChangeValue);
    }

    // split change evenly when both changes < threshold
    if (senderChangeValue < CHANGE_SPLIT_THRESHOLD
        && counterpartyChangeValue.getValue() < CHANGE_SPLIT_THRESHOLD) {
      long combinedChangeValue = senderChangeValue + counterpartyChangeValue.getValue();
      long splitChangeValue = combinedChangeValue / 2L;
      if (combinedChangeValue % 2L != 0) {
        fee++;
        payload2.setFeeAmount(fee);
      }

      if (log.isDebugEnabled()) {
        log.debug(
            "+output (Sender change) = " + senderChangeAddress + ", value=" + splitChangeValue);
        log.debug(
            "+output (Counterparty change) = "
                + collabChangeAddress
                + ", value="
                + splitChangeValue);
      }

      senderChangeValue = splitChangeValue;
      counterpartyChangeValue = Coin.valueOf(splitChangeValue);
    }

    // set counterparty change output
    counterpartyChangeOutput.setValue(counterpartyChangeValue);

    // set sender change output
    TransactionOutput senderChangeOutput =
        computeTxOutput(senderChangeAddress, senderChangeValue, cahootsContext);
    payload2.setCollabChange(senderChangeAddress);
    outputs.add(senderChangeOutput);

    payload2.getPSBT().setTransaction(tx);
    payload2.doStep2(inputs, outputs);
    return payload2;
  }

  private long computeMinerFee(Tx0x2 payload, Tx0 tx0Initiator) {
    int nbPremixCounterparty = payload.getTransaction().getOutputs().size() - 1;
    int nbPremixSender = Math.min(tx0Initiator.getNbPremix(), payload.getMaxOutputsEach());
    int nbPremix = nbPremixCounterparty + nbPremixSender;
    int nbInputsCounterparty = payload.getTransaction().getInputs().size();
    int nbInputsSender = tx0Initiator.getSpendFroms().size();
    int nbInputs = nbInputsCounterparty + nbInputsSender;
    int tx0Size = ClientUtils.computeTx0Size(nbPremix, nbInputs, params);
    long fee = ClientUtils.computeTx0MinerFee(tx0Size, tx0Initiator.getTx0MinerFeePrice(), true);
    if (log.isDebugEnabled()) {
      log.debug(
          "nbPremixCounterparty="
              + nbPremixCounterparty
              + ", nbPremixSender="
              + nbPremixSender
              + ", nbInputsCounterparty="
              + nbInputsCounterparty
              + ", nbInputsSender="
              + nbInputsSender
              + ", fee="
              + fee);
    }
    return fee;
  }

  private void contributeSenderPremixOutputs(
      Tx0x2Context cahootsContext,
      Tx0 tx0Initiator,
      List<TransactionOutput> outputs,
      int maxOutputsEach)
      throws Exception {
    List<TransactionOutput> premixOutputs = tx0Initiator.getPremixOutputs();
    int position = 0;
    for (TransactionOutput premixOutput : premixOutputs) {
      if (position >= maxOutputsEach) {
        break;
      }

      long premixOutputValue = premixOutput.getValue().longValue();
      String changeAddress = getBipFormatSupplier().getToAddress(premixOutput);

      if (log.isDebugEnabled()) {
        log.debug("+output (Sender premix) = " + changeAddress + ", value=" + premixOutputValue);
      }

      // add correct addresses to cahootsContext
      TransactionOutput senderChangeOutput =
          computeTxOutput(changeAddress, premixOutputValue, cahootsContext);
      outputs.add(premixOutput);
      position++;
    }
  }

  private List<CahootsUtxo> toCahootsUtxos(
      Collection<UnspentOutput> inputs, CahootsContext cahootsContext) throws Exception {
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    List<CahootsUtxo> allCahootsUtxos =
        cahootsWallet.getUtxosWpkhByAccount(cahootsContext.getAccount());

    Set<String> inputIds =
        inputs.stream()
            .map(utxo -> utxo.tx_hash + ":" + utxo.tx_output_n)
            .collect(Collectors.toSet());
    List<CahootsUtxo> cahootsUtxos =
        allCahootsUtxos.stream()
            .filter(
                cahootsUtxo -> {
                  String key =
                      cahootsUtxo.getOutpoint().getTxHash().toString()
                          + ":"
                          + cahootsUtxo.getOutpoint().getTxOutputN();
                  return inputIds.contains(key);
                })
            .collect(Collectors.toList());
    if (cahootsUtxos.size() != inputs.size()) {
      throw new Exception("CahootsUtxo not found for TX0 input");
    }
    return cahootsUtxos;
  }

  // used to create lower pool input from higher pool change output
  private CahootsUtxo toCahootsUtxo(Tx0x2Context cahootsContext, TransactionOutput higherPoolChange)
      throws Exception {
    HD_Address address =
        cahootsContext
            .getCahootsWallet()
            .getReceiveWallet(cahootsContext.getAccount(), BIP_FORMAT.SEGWIT_NATIVE)
            .getAddressAt(Chain.CHANGE.getIndex(), 0)
            .getHdAddress();
    byte[] key =
        cahootsContext
            .getCahootsWallet()
            .getUtxosWpkhByAccount(cahootsContext.getAccount())
            .get(0)
            .getKey();
    return new CahootsUtxo(
        new MyTransactionOutPoint(higherPoolChange, address.getAddressString(), 0),
        UnspentOutput.computePath(address),
        cahootsContext
            .getCahootsWallet()
            .getReceiveWallet(cahootsContext.getAccount(), BIP_FORMAT.SEGWIT_NATIVE)
            .getXPub(),
        key);
  }

  //
  // counterparty
  //
  @Override
  public Tx0x2 doStep3(Tx0x2 payload2, Tx0x2Context cahootsContext) throws Exception {
    // set samourai fee for max spend check
    long samouraiFeeValueEach = payload2.getSamouraiFeeValueEach();
    cahootsContext.setSamouraiFee(samouraiFeeValueEach * 2);

    Tx0x2 payload3 = super.doStep3(payload2, cahootsContext);
    return payload3;
  }

  //
  // sender
  //
  @Override
  public Tx0x2 doStep4(Tx0x2 payload3, Tx0x2Context cahootsContext) throws Exception {
    // set samourai fee for max spend check
    long samouraiFeeValueEach = payload3.getSamouraiFeeValueEach();
    cahootsContext.setSamouraiFee(samouraiFeeValueEach * 2);

    Tx0x2 payload4 = super.doStep4(payload3, cahootsContext);
    return payload4;
  }

  //
  // used in steps 3 & 4 to verify
  //
  @Override
  protected long computeMaxSpendAmount(long minerFee, Tx0x2Context cahootsContext)
      throws Exception {
    long sharedMinerFee = minerFee / 2L; // splits miner fee
    long samouraiFeeValue = cahootsContext.getSamouraiFee();
    long samouraiFeeValueEach = samouraiFeeValue / 2L; // splits samourai fee
    long maxSpendAmount = 0L;

    String prefix =
        "[" + cahootsContext.getCahootsType() + "/" + cahootsContext.getTypeUser() + "] ";
    switch (cahootsContext.getTypeUser()) {
      case SENDER:
        if (!cahootsContext.isLowerPool() && !cahootsContext.isBottomPool()) {
          // initial pool
          maxSpendAmount = samouraiFeeValueEach + sharedMinerFee;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + ": samouraiFeeEach="
                    + samouraiFeeValueEach
                    + " + sharedMinerFee="
                    + sharedMinerFee);
          }
        } else if (!cahootsContext.isLowerPool() && cahootsContext.isBottomPool()) {
          // 0.001btc pool only
          long inputValue = cahootsContext.getInputs().get(0).getValue();
          long outputValue =
              cahootsContext.getTx0Initiator().getPremixValue()
                  * (cahootsContext.getOutputAddresses().size() - 1);
          long changeValue = inputValue - outputValue - samouraiFeeValueEach - sharedMinerFee;
          long maxBottomPoolSplit = changeValue / 2L; // max split output
          maxSpendAmount = samouraiFeeValueEach + sharedMinerFee + maxBottomPoolSplit;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + " + samouraiFeeEach="
                    + samouraiFeeValueEach
                    + " + sharedMinerFee="
                    + sharedMinerFee
                    + " + maxBottomPoolSplit="
                    + maxBottomPoolSplit);
          }
        } else if (cahootsContext.isLowerPool() && !cahootsContext.isBottomPool()) {
          // middle pools
          maxSpendAmount = samouraiFeeValue + sharedMinerFee;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + ": samouraiFee="
                    + samouraiFeeValue
                    + " + sharedMinerFee="
                    + sharedMinerFee);
          }
        } else if (cahootsContext.isLowerPool() && cahootsContext.isBottomPool()) {
          // bottom pool (0.001btc)
          long inputValue = cahootsContext.getInputs().get(0).getValue();
          long outputValue =
              cahootsContext.getTx0Initiator().getPremixValue()
                  * (cahootsContext.getOutputAddresses().size() - 1);
          long changeValue = inputValue - outputValue - samouraiFeeValue - sharedMinerFee;
          long maxBottomPoolSplit = changeValue / 2L; // max split output
          maxSpendAmount = samouraiFeeValue + sharedMinerFee + maxBottomPoolSplit;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + ": samouraiFee="
                    + samouraiFeeValue
                    + " + sharedMinerFee="
                    + sharedMinerFee
                    + " + maxBottomPoolSplit="
                    + maxBottomPoolSplit);
          }
        }
        break;
      case COUNTERPARTY:
        if (!cahootsContext.isLowerPool() && !cahootsContext.isBottomPool()) {
          // initial pool
          maxSpendAmount = samouraiFeeValueEach + sharedMinerFee;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + ": samouraiFeeEach="
                    + samouraiFeeValueEach
                    + " + sharedMinerFee="
                    + sharedMinerFee);
          }
        } else if (!cahootsContext.isLowerPool() && cahootsContext.isBottomPool()) {
          // 0.001btc pool only
          long maxBottomPoolSplit =
              cahootsContext.getInputs().get(0).getValue(); // TODO more accurate check
          maxSpendAmount = samouraiFeeValueEach + sharedMinerFee + maxBottomPoolSplit;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + " + samouraiFeeEach="
                    + samouraiFeeValueEach
                    + " + sharedMinerFee="
                    + sharedMinerFee
                    + " + maxBottomPoolSplit="
                    + maxBottomPoolSplit);
          }
        } else if (cahootsContext.isLowerPool() && !cahootsContext.isBottomPool()) {
          // middle pools
          maxSpendAmount = sharedMinerFee;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + " + sharedMinerFee="
                    + sharedMinerFee);
          }
        } else if (cahootsContext.isLowerPool() && cahootsContext.isBottomPool()) {
          // bottom pool (0.001btc)
          long maxBottomPoolSplit =
              cahootsContext.getInputs().get(0).getValue(); // TODO more accurate check
          maxSpendAmount = sharedMinerFee + maxBottomPoolSplit;
          if (log.isDebugEnabled()) {
            log.debug(
                prefix
                    + "maxSpendAmount = "
                    + maxSpendAmount
                    + " + sharedMinerFee="
                    + sharedMinerFee
                    + " + maxBottomPoolSplit="
                    + maxBottomPoolSplit);
          }
        }
        break;
      default:
        throw new Exception("Unknown typeUser");
    }

    return maxSpendAmount;
  }
}
