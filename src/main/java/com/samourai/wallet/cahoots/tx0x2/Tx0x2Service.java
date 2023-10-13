package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.TypeInteraction;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoOutPoint;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.*;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2Service extends AbstractCahootsService<Tx0x2, Tx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2Service.class);
  protected static final TxUtil txUtil = TxUtil.getInstance();
  public static final long CHANGE_SPLIT_THRESHOLD = 100000; // lowest pool denomination (0.001btc)
  private Tx0Service tx0Service;

  public Tx0x2Service(BipFormatSupplier bipFormatSupplier, Tx0Service tx0Service) {
    super(
        CahootsType.TX0X2, bipFormatSupplier, tx0Service.getParams(), TypeInteraction.TX_BROADCAST);
    this.tx0Service = tx0Service;
  }

  //
  // sender: step 0
  //
  @Override
  public Tx0x2 startInitiator(Tx0x2Context cahootsContext) throws Exception {
    Tx0PreviewService tx0PreviewService = tx0Service.getTx0PreviewService();
    Pool pool = cahootsContext.getTx0ConfigInitiator().getPool();
    Tx0Config tx0Config = cahootsContext.getTx0ConfigInitiator();

    // list pool(s) for Tx0x2
    List<Pool> pools = new LinkedList<>();
    pools.add(pool); // initial pool
    if (tx0Config.isCascade()) {
      pools.addAll(tx0PreviewService.findCascadingPools(pool.getPoolId())); // lower pools
    }
    List<String> poolIds = pools.stream().map(p -> p.getPoolId()).collect(Collectors.toList());

    long minSpendValueEach = pool.getTx0PreviewMinSpendValue() / 2;
    long maxSpendValueEach = pool.getTx0PreviewMaxSpendValueCascading() / 2;
    Tx0x2 payload0 = new Tx0x2(params, poolIds, minSpendValueEach, maxSpendValueEach);

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
        // sender
        payload = doStep2(cahoots, cahootsContext);
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
  // counterparty: initialize TX, provide counterparty inputs & changeAdresses
  //
  protected Tx0x2 doStep1(Tx0x2 payload0, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload1 = payload0.copy();

    // find counterparty inputs for maxOutputsEach
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    int account = cahootsContext.getAccount();
    Collection<CahootsUtxo> utxos = cahootsWallet.getUtxosWpkhByAccount(account);
    long spendMin = payload0.getMinSpendValueEach();
    long spendTarget = payload0.getMaxSpendValueEach();
    Collection<CahootsUtxo> counterpartyInputs = selectInputs(utxos, spendMin, spendTarget);

    // register counterparty inputs (to sign it later)
    cahootsContext.addInputs(counterpartyInputs);

    // generate change addresses
    List<BipAddress> counterpartyChangeAddresses = new LinkedList<>();
    Map<String, String> changeAddressByPool = new LinkedHashMap<>();
    for (String poolId : payload1.getPoolIds()) {
      BipAddress changeAddress =
          cahootsWallet.fetchAddressChange(
              SamouraiAccountIndex.DEPOSIT, true, BIP_FORMAT.SEGWIT_NATIVE);
      counterpartyChangeAddresses.add(changeAddress);
      String changeAddressStr = changeAddress.getAddressString();
      changeAddressByPool.put(poolId, changeAddressStr);
      cahootsContext.addOutputAddress(changeAddressStr);
      if (log.isDebugEnabled()) {
        log.debug("+changeAddress[" + poolId + "] = " + changeAddress);
      }

      // register changes by address as we don't know change outpoint yet
      byte[] privKey = changeAddress.getHdAddress().getECKey().getPrivKeyBytes();
      cahootsContext.getKeyBag().add(changeAddressStr, privKey);
    }

    payload1.doStep1(
        changeAddressByPool,
        (Collection<UtxoOutPoint>) (Collection<? extends UtxoOutPoint>) counterpartyInputs);
    return payload1;
  }

  private Collection<CahootsUtxo> selectInputs(
      Collection<CahootsUtxo> utxos, long spendMin, long spendTarget) throws Exception {
    long totalBalance = CahootsUtxo.sumValue(utxos);

    if (totalBalance < spendMin) {
      throw new Exception("Cannot compose #Cahoots: insufficient wallet balance");
    }

    if (totalBalance <= spendTarget) {
      return utxos; // use whole balance
    }

    // select random utxo-set >= spendTarget, prefer only one utxo when possible
    List<CahootsUtxo> selectedUTXOs = new ArrayList<>();
    long sumSelectedUTXOs = 0;

    RandomUtil.getInstance().shuffle(utxos);

    for (CahootsUtxo utxo : utxos) {
      long utxoValue = utxo.getValueLong();
      if (utxoValue >= spendTarget) {
        // select single utxo
        return Arrays.asList(utxo);
      } else if (sumSelectedUTXOs < spendTarget) {
        // add utxos until target reached
        selectedUTXOs.add(utxo);
        sumSelectedUTXOs += utxoValue;
      }
    }
    return selectedUTXOs;
  }

  // build partial TX0 (without counterparty premixs & change)
  protected Tx0x2 doStep2(Tx0x2 payload1, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload2 = payload1.copy();

    // retrieve initiator inputs (with cascading txs if any)
    Collection<BipUtxo> senderSpendFroms =
        cahootsContext.getTx0ConfigInitiator().getOwnSpendFromUtxos();

    // retrieve counterparty info
    Collection<UtxoOutPoint> counterpartyInputs = payload1.getCounterpartyInputs();
    Map<String, String> counterpartyChangeAddressByPoolId =
        payload1.getCounterpartyChangeAddressByPoolId();

    // build full TX0 with initiator+counterparty inputs&outputs (with cascading txs if any)
    Tx0Config tx0ConfigInitiator = cahootsContext.getTx0ConfigInitiator();
    Pool pool = tx0ConfigInitiator.getPool();
    Tx0Config tx0Config = new Tx0Config(tx0ConfigInitiator, senderSpendFroms, pool);
    Tx0x2CahootsConfig tx0x2CahootsConfig =
        new Tx0x2CahootsConfig(counterpartyInputs, counterpartyChangeAddressByPoolId);
    tx0Config.setTx0x2CahootsConfig(tx0x2CahootsConfig);
    Tx0Result tx0Result =
        tx0Service
            .tx0(tx0Config)
            .orElseThrow(
                () -> new NotifiableException("TX0 is not possible for pool " + pool.getPoolId()));

    // register inputs & outputs in CahootsContext (for later checkMaxSpendAmount & sign)
    for (Tx0 tx0 : tx0Result.getList()) {
      // register inputs
      Collection<? extends UtxoOutPoint> ownSpendFroms = tx0.getOwnSpendFroms();
      cahootsContext.addInputs((Collection<UtxoOutPoint>) ownSpendFroms, tx0.getKeyBag());

      // register outputs
      BipUtxo senderChangeUtxo = tx0.getTx0x2CahootsResult().getSenderChangeUtxo();
      if (senderChangeUtxo != null) {
        cahootsContext.addOutputAddress(senderChangeUtxo.getAddress());
      }
      for (TransactionOutput premixOutput : tx0.getOwnPremixOutputs()) {
        cahootsContext.addOutputAddress(getBipFormatSupplier().getToAddress(premixOutput));
      }
      if (tx0.getFeeChange() > 0) {
        cahootsContext.addOutputAddress(
            getBipFormatSupplier().getToAddress(tx0.getSamouraiFeeOutput()));
      }
    }

    // register tx0Result in CahootsContext
    cahootsContext.setTx0ResultInitiator(tx0Result);

    payload2.doStep2(tx0Result);
    return payload2;
  }

  //
  // counterparty: replace premix outputs + sign
  //
  public Tx0x2 doStep3(Tx0x2 payload2, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload3 = payload2.copy();

    Map<String, PSBT> psbtByPoolId = new LinkedHashMap<>();
    for (Map.Entry<String, Tx0x2Item> entry : payload3.getTx0x2ItemByPoolId().entrySet()) {
      String poolId = entry.getKey();
      Tx0x2Item tx0x2Item = entry.getValue();
      Transaction transaction = payload3.getTransaction(poolId);

      // sort inputs
      txUtil.sortBip69Inputs(transaction);

      // update OP_RETURN if needed
      transaction = replaceCounterpartyOpReturn(cahootsContext, tx0x2Item, transaction);

      // append counterparty premixs
      transaction = appendCounterpartyPremixs(tx0x2Item, transaction, cahootsContext);

      // sort outputs
      txUtil.sortBip69Outputs(transaction);

      // verify spendAmount
      checkMaxSpendAmount(payload2, cahootsContext);

      // sign
      signTx(cahootsContext, transaction);

      psbtByPoolId.put(poolId, new PSBT(transaction));
    }
    payload3.doStep3(psbtByPoolId);
    return payload3;
  }

  //
  // sender: sign
  //
  public Tx0x2 doStep4(Tx0x2 payload3, Tx0x2Context cahootsContext) throws Exception {
    Tx0x2 payload4 = payload3.copy();

    Map<String, PSBT> psbtByPoolId = new LinkedHashMap<>();
    for (Map.Entry<String, Tx0x2Item> entry : payload4.getTx0x2ItemByPoolId().entrySet()) {
      String poolId = entry.getKey();
      Transaction transaction = payload3.getTransaction(poolId);

      // verify spendAmount
      checkMaxSpendAmount(payload3, cahootsContext);

      // sign
      signTx(cahootsContext, transaction);

      psbtByPoolId.put(poolId, new PSBT(transaction));
    }
    payload4.doStep4(psbtByPoolId);
    return payload4;
  }

  @Override
  protected int signTx(Tx0x2Context cahootsContext, Transaction transaction) throws Exception {
    int nbSigned = super.signTx(cahootsContext, transaction);
    if (nbSigned <= 0) {
      throw new Exception("Signing problem: nbSigned=" + nbSigned);
    }
    return nbSigned;
  }

  @Override
  protected CahootsResult<Tx0x2Context, Tx0x2> computeCahootsResult(
      Tx0x2Context cahootsContext, Tx0x2 cahoots) {
    // update partial transactions in CahootsContext.Tx0ResultInitiator from final Cahoots
    for (Tx0 tx0 : cahootsContext.getTx0ResultInitiator().getList()) {
      Transaction transaction = cahoots.getTransaction(tx0.getPool().getPoolId());
      tx0._finalizeTx0x2Result(transaction);
    }
    return new Tx0x2Result(cahootsContext, cahoots);
  }

  private void checkMaxSpendAmount(Tx0x2 tx0x2, Tx0x2Context cahootsContext) throws Exception {
    long totalSpendAmount = 0;
    for (Map.Entry<String, Tx0x2Item> e : tx0x2.getTx0x2ItemByPoolId().entrySet()) {
      if (log.isDebugEnabled()) {
        String prefix =
            "[" + cahootsContext.getCahootsType() + "/" + cahootsContext.getTypeUser() + "] ";
        String poolId = e.getKey();
        log.debug(prefix + "checkMaxSpendAmount for " + poolId + "...");
      }
      String poolId = e.getKey();
      totalSpendAmount += computeSpendAmount(tx0x2.getTransaction(poolId), cahootsContext);
    }
    long maxSpendAmount = computeMaxSpendAmount(tx0x2, cahootsContext);
    super.checkMaxSpendAmount(cahootsContext, totalSpendAmount, maxSpendAmount);
  }

  @Override
  protected long computeSpendAmount(Transaction tx, Tx0x2Context cahootsContext) throws Exception {
    long spendAmount = super.computeSpendAmount(tx, cahootsContext);
    if (spendAmount <= 0) {
      throw new Exception("Invalid spendAmount=" + spendAmount); // security check
    }
    return spendAmount;
  }

  protected long computeMaxSpendAmount(Tx0x2 tx0x2, Tx0x2Context cahootsContext) throws Exception {
    long maxSpendAmount;
    String prefix =
        "[" + cahootsContext.getCahootsType() + "/" + cahootsContext.getTypeUser() + "] ";
    switch (cahootsContext.getTypeUser()) {
      case SENDER:
        // spends sharedSamouraiFee + sharedMinerFee
        List<Tx0> tx0s = cahootsContext.getTx0ResultInitiator().getList();
        long totalSamouraiFeeSender =
            tx0s.stream()
                .mapToLong(tx0 -> tx0.getTx0x2CahootsResult().getSamouraiFeeSender())
                .sum();
        long totalMinerFeeSender =
            tx0s.stream()
                .mapToLong(tx0 -> tx0.getTx0x2CahootsResult().getTx0MinerFeeSender())
                .sum();
        Tx0 lastTx0 = tx0s.get(tx0s.size() - 1);
        long splitChangeTolerance =
            lastTx0.getTx0x2Preview().isSplitChange() ? CHANGE_SPLIT_THRESHOLD : 0;
        maxSpendAmount = totalSamouraiFeeSender + totalMinerFeeSender + splitChangeTolerance;
        if (log.isDebugEnabled()) {
          log.debug(
              prefix
                  + "maxSpendAmount = "
                  + maxSpendAmount
                  + ": totalSamouraiFeeSender="
                  + totalSamouraiFeeSender
                  + " + totalMinerFeeSender="
                  + totalMinerFeeSender
                  + " + splitChangeTolerance="
                  + splitChangeTolerance);
        }
        break;
      case COUNTERPARTY:
        // spends sharedSamouraiFee + sharedMinerFee
        long totalSamouraiFeeCounterparty = tx0x2.getTotalSamouraiFeeCounterparty();
        long totalMinerFeeCounterparty = tx0x2.getTotalMinerFeeCounterparty();
        splitChangeTolerance = CHANGE_SPLIT_THRESHOLD;
        maxSpendAmount = totalSamouraiFeeCounterparty + totalMinerFeeCounterparty;
        if (log.isDebugEnabled()) {
          log.debug(
              prefix
                  + "maxSpendAmount = "
                  + maxSpendAmount
                  + ": totalSamouraiFeeCounterparty="
                  + totalSamouraiFeeCounterparty
                  + " + totalMinerFeeCounterparty="
                  + totalMinerFeeCounterparty
                  + "splitChangeTolerance="
                  + splitChangeTolerance);
        }
        break;
      default:
        throw new Exception("Unknown typeUser");
    }
    if (maxSpendAmount <= 0) {
      throw new Exception("Invalid maxSpendAmount=" + maxSpendAmount); // security check
    }
    return maxSpendAmount;
  }

  private Transaction replaceCounterpartyOpReturn(
      CahootsContext cahootsContext, Tx0x2Item tx0x2Item, Transaction transaction)
      throws Exception {
    UtxoOutPoint firstUtxo = cahootsContext.findInput(transaction.getInput(0).getOutpoint());
    if (firstUtxo == null) {
      // first input belongs to sender, no need to update OP_RETURN
      if (log.isDebugEnabled()) {
        log.debug("Using OP_RETURN from SENDER");
      }
      return transaction;
    }

    if (log.isDebugEnabled()) {
      log.debug("Using OP_RETURN from COUNTERPARTY");
    }

    // first input belongs to counterparty => encode it with our own input
    byte[] firstInputKey = cahootsContext.getKeyBag().getPrivKeyBytes(firstUtxo);
    byte[] opReturn =
        tx0Service.computeOpReturn(
            firstUtxo, firstInputKey, tx0x2Item.getFeePaymentCode(), tx0x2Item.getFeePayload());
    TransactionOutput opReturnOutput = tx0Service.computeOpReturnOutput(opReturn);

    // replace OP_RETURN
    List<TransactionOutput> txOuts =
        transaction.getOutputs().stream()
            .filter(txOut -> !txOut.getScriptPubKey().isOpReturn())
            .collect(Collectors.toList());
    txOuts.add(opReturnOutput);
    txUtil.replaceOutputs(transaction, txOuts);
    return transaction;
  }

  private Transaction appendCounterpartyPremixs(
      Tx0x2Item tx0x2Item, Transaction transaction, CahootsContext cahootsContext)
      throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("(CounterParty) Appending " + tx0x2Item.getNbPremixCounterparty() + " premixs...");
    }
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    for (int i = 0; i < tx0x2Item.getNbPremixCounterparty(); i++) {
      BipWallet premixWallet = cahootsWallet.getWalletPremix();
      Pair<TransactionOutput, BipAddress> pair =
          tx0Service.computeOwnPremixOutput(tx0x2Item.getPremixValue(), premixWallet);
      TransactionOutput txOut = pair.getLeft();
      BipAddress premixAddress = pair.getRight();
      transaction.addOutput(txOut);
      cahootsContext.addOutputAddress(premixAddress.getAddressString());
    }
    return transaction;
  }
}
