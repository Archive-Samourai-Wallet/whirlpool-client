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
import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Service extends AbstractCahoots2xService<Tx0x2, Tx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Service.class);

  public Tx0x2Service(BipFormatSupplier bipFormatSupplier, NetworkParameters params) {
    super(CahootsType.TX0X2, bipFormatSupplier, params);
  }

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
        payload = doStep2(cahoots, cahootsContext);
        break;
      case 2:
        payload = doStep3(cahoots, cahootsContext);
        break;
      case 3:
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

    List<CahootsUtxo> selectedUTXO =
        utxos; // TODO TX0X2 select random utxo-set >= spendTarget. Prefer only one UTXO when
               // possible.
    return selectedUTXO;
  }

  private List<TransactionOutput> contributePremixOutputs(
      Tx0x2 payload0, Tx0x2Context cahootsContext, int nbPremixs) throws Exception {
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

    // add spender inputs
    List<CahootsUtxo> cahootsInputs = toCahootsUtxos(tx0Initiator.getSpendFroms(), cahootsContext);
    List<TransactionInput> inputs = cahootsContext.addInputs(cahootsInputs);

    List<TransactionOutput> outputs = new ArrayList<>();

    // TODO TX0X2 add opReturnOutput (from tx0Initiator)

    // TODO TX0X2 add samouraiFeeOutput (from tx0Initiator)

    // TODO add spender premix outputs (from tx0Initiator) (limit to maxOutputsEach)

    // use changeAddress from tx0Initiator to avoid index gap
    String changeAddress =
        getBipFormatSupplier().getToAddress(tx0Initiator.getChangeOutputs().iterator().next());
    // TODO add sender change output (senderInputsSum - senderPremixOutputsSum -
    // samouraiFeeValueEach - minerFeePaid)

    // TODO TX0X2 update counterparty change output to deduce minerFeePaid (see STONEWALLX2Service
    // as example)

    payload2.doStep2(inputs, outputs);
    return payload2;
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

  @Override
  protected long computeMaxSpendAmount(long minerFee, Tx0x2Context cahootsContext)
      throws Exception {
    return 999999999; // TODO TX0X2 implement security checks
  }
}
