package com.samourai.whirlpool.client.tx0x2;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Service extends AbstractCahoots2xService<Tx0x2, Tx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Service.class);

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
    Tx0x2 tx0x21 = doStep1(tx0x20, cahootsContext);
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
        payload = doStep2(cahoots, cahootsContext); // sender
        break;
      case 2:
        payload = doStep3(cahoots, cahootsContext); // counterparty
        break;
      case 3:
        payload = doStep4(cahoots, cahootsContext); // sender
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
  // counterparty
  //
  private Tx0x2 doStep1(Tx0x2 payload0, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload1 = payload0.copy();
    payload1.setCounterpartyAccount(cahootsContext.getAccount());
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    byte[] fingerprint = cahootsWallet.getFingerprint();
    payload1.setFingerprintCollab(fingerprint);

    // counterparty inputs
    int account = cahootsContext.getAccount();
    List<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(account);
    long spendMin = payload0.getPremixValue() + payload0.getSamouraiFeeValueEach();
    long spendTarget =
        payload0.getPremixValue() * payload0.getMaxOutputsEach()
            + payload0.getSamouraiFeeValueEach();
    List<CahootsUtxo> selectedUTXO = contributeInputs(utxos, spendMin, spendTarget);
    List<TransactionInput> inputs = cahootsContext.addInputs(selectedUTXO);

    // counterparty premix outputs
    long myInputsSum = CahootsUtxo.sumValue(selectedUTXO).longValue();
    int nbPremixs =
        (int)
            Math.min(
                (myInputsSum - payload0.getSamouraiFeeValueEach()) / payload0.getPremixValue(),
                payload0.getMaxOutputsEach());
    List<TransactionOutput> outputs = contributePremixOutputs(payload0, cahootsContext, nbPremixs);
    long myPremixOutputsSum = outputs.size() * payload0.getPremixValue();

    // counterparty change output
    long changeAmount =
        myInputsSum
            - myPremixOutputsSum
            - payload0.getSamouraiFeeValueEach(); // not including minerFee yet
    if (changeAmount > 0) {
      String changeAddress =
          cahootsWallet.fetchAddressChange(
              payload0.getCounterpartyAccount(), true, BIP_FORMAT.SEGWIT_NATIVE);
      if (log.isDebugEnabled()) {
        log.debug("+output (CounterParty change) = " + changeAddress + ", value=" + changeAmount);
      }
      TransactionOutput changeOutput = computeTxOutput(changeAddress, changeAmount, cahootsContext);
      payload1.setCollabChange(changeAddress);
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
      Tx0x2 payload0, Tx0x2Context cahootsContext, int nbPremixs) throws Exception {
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();

    List<TransactionOutput> outputs = new ArrayList<>();
    for (int i = 0; i < nbPremixs; i++) {
      String changeAddress =
          cahootsWallet.fetchAddressReceive(
              SamouraiAccountIndex.PREMIX, true, BIP_FORMAT.SEGWIT_NATIVE);
      if (log.isDebugEnabled()) {
        log.debug("+output (CounterParty premix) = " + changeAddress);
      }
      TransactionOutput changeOutput =
          computeTxOutput(changeAddress, payload0.getPremixValue(), cahootsContext);
      outputs.add(changeOutput);
    }
    return outputs;
  }

  //
  // sender
  //
  private Tx0x2 doStep2(Tx0x2 payload1, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload2 = payload1.copy();
    Tx0 tx0Initiator = cahootsContext.getTx0Initiator();

    // compute minerFee
    int nbPremixCounterparty = payload1.getTransaction().getOutputs().size() - 1;
    int nbPremixSender = Math.min(tx0Initiator.getNbPremix(), payload1.getMaxOutputsEach());
    int nbPremix = nbPremixCounterparty + nbPremixSender;
    int nbInputsCounterparty = payload1.getTransaction().getInputs().size();
    int nbInputsSender = tx0Initiator.getSpendFroms().size();
    int nbInputs = nbInputsCounterparty + nbInputsSender;
    int tx0Size = ClientUtils.computeTx0Size(nbPremix, nbInputs, params);
    long fee = FeeUtil.getInstance().calculateFee(tx0Size, tx0Initiator.getTx0MinerFeePrice());
    if (fee % 2L != 0) {
      fee++;
    }
    payload2.setFeeAmount(fee);
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

    // keep track of minerFeePaid
    long minerFeePaid = fee / 2L;
    cahootsContext.setMinerFeePaid(minerFeePaid); // sender & counterparty pay half minerFee

    // add sender inputs
    List<CahootsUtxo> cahootsInputs = toCahootsUtxos(tx0Initiator.getSpendFroms(), cahootsContext);
    List<TransactionInput> inputs = cahootsContext.addInputs(cahootsInputs);

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

    // add sender change output (senderInputsSum - senderPremixOutputsSum - samouraiFeeValueEach -
    // minerFeePaid)
    long senderInputsSum = CahootsUtxo.sumValue(cahootsInputs).longValue();

    long senderPremixOutputsSum = 0;
    senderPremixOutputsSum = payload2.getPremixValue() * nbPremixSender;

    long senderChangeAmount =
        senderInputsSum
            - senderPremixOutputsSum
            - payload2.getSamouraiFeeValueEach()
            - minerFeePaid;

    // use changeAddress from tx0Initiator to avoid index gap
    String changeAddress =
        getBipFormatSupplier().getToAddress(tx0Initiator.getChangeOutputs().iterator().next());

    if (log.isDebugEnabled()) {
      log.debug("+output (Sender change) = " + changeAddress + ", value=" + senderChangeAmount);
    }

    TransactionOutput senderChangeOutput =
        computeTxOutput(changeAddress, senderChangeAmount, cahootsContext);
    payload2.setCollabChange(changeAddress);
    outputs.add(senderChangeOutput);

    // update counterparty change output to deduce minerFeePaid
    Transaction tx = payload1.getTransaction();
    String collabChangeAddress = payload1.getCollabChange();
    TransactionOutput counterpartyChangeOutput =
        TxUtil.getInstance().findOutputByAddress(tx, collabChangeAddress, getBipFormatSupplier());
    if (counterpartyChangeOutput == null) {
      throw new Exception("Cannot compose #Cahoots: counterpartyChangeOutput not found");
    }

    // counterparty pays half of fees
    Coin counterpartyChangeValue =
        Coin.valueOf(counterpartyChangeOutput.getValue().longValue() - minerFeePaid);

    if (log.isDebugEnabled()) {
      log.debug("counterparty change output value post fee:" + counterpartyChangeValue);
    }

    counterpartyChangeOutput.setValue(counterpartyChangeValue);
    payload2.getPSBT().setTransaction(tx);

    payload2.doStep2(inputs, outputs);
    return payload2;
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
        log.debug("+output (Sender change) = " + changeAddress + ", value=" + premixOutputValue);
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
        inputs.stream().map(utxo -> utxo.getUtxoName()).collect(Collectors.toSet());
    List<CahootsUtxo> cahootsUtxos =
        allCahootsUtxos.stream()
            .filter(
                cahootsUtxo -> {
                  String key = cahootsUtxo.getOutpoint().getUtxoName();
                  return inputIds.contains(key);
                })
            .collect(Collectors.toList());
    if (cahootsUtxos.size() != inputs.size()) {
      throw new Exception("CahootsUtxo not found for TX0 input");
    }
    return cahootsUtxos;
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
  public Tx0x2 doStep4(Tx0x2 payload2, Tx0x2Context cahootsContext) throws Exception {
    // set samourai fee for max spend check
    long samouraiFeeValueEach = payload2.getSamouraiFeeValueEach();
    cahootsContext.setSamouraiFee(samouraiFeeValueEach * 2);

    Tx0x2 payload3 = super.doStep4(payload2, cahootsContext);

    return payload3;
  }

  //
  // used in steps 3 & 4 to verify
  //
  @Override
  protected long computeMaxSpendAmount(long minerFee, Tx0x2Context cahootsContext) {
    long sharedMinerFee = minerFee / 2; // splits miner fee
    long samouraiFeeValueEach = cahootsContext.getSamouraiFee() / 2; // splits samourai fee
    long maxSpendAmount = samouraiFeeValueEach + sharedMinerFee;

    if (log.isDebugEnabled()) {
      String prefix =
          "[" + cahootsContext.getCahootsType() + "/" + cahootsContext.getTypeUser() + "] ";
      log.debug(
          prefix
              + "maxSpendAmount = "
              + maxSpendAmount
              + ": samouraiFeeValueEach="
              + samouraiFeeValueEach
              + " + sharedMinerFee="
              + sharedMinerFee);
    }

    return maxSpendAmount;
  }
}
