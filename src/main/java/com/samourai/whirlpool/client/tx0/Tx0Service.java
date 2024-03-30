package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.constants.BIP_WALLET;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.event.Tx0Event;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.BIP69InputComparatorUnspentOutput;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.WhirlpoolInfo;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.PoolComparatorByDenominationDesc;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImpl;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import com.samourai.whirlpool.protocol.soroban.exception.PushTxErrorException;
import com.samourai.whirlpool.protocol.soroban.payload.beans.PushTxError;
import com.samourai.whirlpool.protocol.soroban.payload.tx0.Tx0PushRequest;
import com.samourai.whirlpool.protocol.soroban.payload.tx0.Tx0PushResponseSuccess;
import java.util.*;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(Tx0Service.class);
  private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private Tx0PreviewService tx0PreviewService;
  private NetworkParameters params;
  private FeeOpReturnImpl feeOpReturnImpl;

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
      UtxoKeyProvider utxoKeyProvider,
      Tx0Previews tx0Previews)
      throws Exception {

    // compute & preview
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
      feeOrBackAddressBech32 = depositWallet.getNextAddressChange().getAddressString();
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
    byte[] firstInputKey = utxoKeyProvider._getPrivKey(firstInput.tx_hash, firstInput.tx_output_n);
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
    List<TransactionOutput> outputs = new ArrayList<>();
    Transaction tx = new Transaction(params);

    //
    // premix outputs
    //
    List<TransactionOutput> premixOutputs = new ArrayList<>();
    for (int j = 0; j < nbPremix; j++) {
      // send to PREMIX
      BipAddress toAddress = premixWallet.getNextAddressReceive();
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
    List<TransactionOutput> changeOutputs = new LinkedList<>();
    List<BipAddress> changeOutputsAddresses = new LinkedList<>();
    if (changeValueTotal > 0) {
      BipAddress changeAddress = changeWallet.getNextAddressChange();
      String changeAddressBech32 = changeAddress.getAddressString();
      TransactionOutput changeOutput =
          bech32Util.getTransactionOutput(changeAddressBech32, changeValueTotal, params);
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

    signTx0(tx, utxoKeyProvider);
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
              path,
              changeWallet.getXPub());
      changeUtxos.add(changeUtxo);
    }
    return changeUtxos;
  }

  public List<Tx0> tx0Cascade(
      Collection<UnspentOutput> spendFroms,
      WalletSupplier walletSupplier,
      Collection<Pool> poolsChoice,
      Tx0Config tx0Config,
      UtxoKeyProvider utxoKeyProvider,
      Tx0Previews tx0Previews)
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
    Tx0 tx0 = tx0(spendFroms, walletSupplier, poolInitial, tx0Config, utxoKeyProvider, tx0Previews);
    tx0List.add(tx0);
    tx0Config.setCascadingParent(tx0);
    UnspentOutput unspentOutputChange = findTx0Change(tx0);

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
        tx0 =
            tx0(
                Collections.singletonList(unspentOutputChange),
                walletSupplier,
                pool,
                tx0Config,
                utxoKeyProvider,
                tx0Previews);
        tx0List.add(tx0);
        tx0Config.setCascadingParent(tx0);
        unspentOutputChange = findTx0Change(tx0);
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
    if (tx0.getChangeUtxos().isEmpty()) {
      // no tx0 change
      return null;
    }
    return tx0.getChangeUtxos().get(0);
  }

  protected void signTx0(Transaction tx, UtxoKeyProvider utxoKeyProvider) throws Exception {
    SendFactoryGeneric.getInstance().signTransaction(tx, utxoKeyProvider);
  }

  public Tx0PushResponseSuccess pushTx0(
      Tx0 tx0, CoordinatorSupplier coordinatorSupplier, WhirlpoolApiClient whirlpoolApiClient)
      throws Exception {
    String tx64 = WhirlpoolProtocol.encodeBytes(tx0.getTx().bitcoinSerialize());
    String poolId = tx0.getPool().getPoolId();
    Tx0PushRequest request = new Tx0PushRequest(tx64, poolId);
    Coordinator coordinator = coordinatorSupplier.findCoordinatorByPoolIdOrThrow(poolId);
    Tx0PushResponseSuccess response =
        ClientUtils.loopHttpAttempts(
            tx0.getTx0Config().getTx0AttemptsSoroban(),
            () ->
                whirlpoolApiClient.tx0Push(
                    request, coordinator.getSender())); // throws PushTxErrorException
    // notify
    WhirlpoolEventService.getInstance().post(new Tx0Event(tx0));
    return response;
  }

  public Tx0PushResponseSuccess pushTx0WithRetryOnAddressReuse(
      Tx0 tx0, WalletSupplier walletSupplier, UtxoSupplier utxoSupplier, Tx0Info tx0Info)
      throws Exception {
    // pushTx0 with multiple attempts on address-reuse
    Exception pushTx0Exception = null;
    int tx0AttemptsAddressReuse = tx0Info.getTx0DataConfig().getTx0AttemptsAddressReuse();
    for (int i = 0; i < tx0AttemptsAddressReuse; i++) {
      log.info(" • Pushing Tx0: txid=" + tx0.getTx().getHashAsString());
      if (log.isDebugEnabled()) {
        log.debug(tx0.getTx().toString());
      }
      try {
        WhirlpoolInfo whirlpoolInfo = tx0Info.getWhirlpoolInfo();
        return pushTx0(
            tx0, whirlpoolInfo.getCoordinatorSupplier(), whirlpoolInfo.createWhirlpoolApiClient());
      } catch (PushTxErrorException e) {
        PushTxError pushTxError = e.getPushTxError();
        log.warn(
            "tx0 failed: "
                + e.getMessage()
                + ", attempt="
                + (i + 1)
                + "/"
                + tx0AttemptsAddressReuse
                + ", error="
                + pushTxError.error);

        if (pushTxError.voutsAddressReuse == null || pushTxError.voutsAddressReuse.isEmpty()) {
          throw e; // not an address-reuse
        }

        // retry on address-reuse
        pushTx0Exception = e;
        tx0 = tx0Retry(tx0, pushTxError, walletSupplier, utxoSupplier);
      }
    }
    throw pushTx0Exception;
  }

  private Tx0 tx0Retry(
      Tx0 tx0,
      PushTxError pushTxError,
      WalletSupplier walletSupplier,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    // manage premix address reuses
    Collection<Integer> premixOutputIndexs = ClientUtils.getOutputIndexs(tx0.getPremixOutputs());
    boolean isPremixReuse =
        pushTxError.voutsAddressReuse != null
            && !ClientUtils.intersect(pushTxError.voutsAddressReuse, premixOutputIndexs).isEmpty();
    if (!isPremixReuse) {
      if (log.isDebugEnabled()) {
        log.debug("isPremixReuse=false => reverting tx0 premix index");
      }
      tx0.getTx0Context().revertIndexPremix();
    }

    // manage change address reuses
    Collection<Integer> changeOutputIndexs = ClientUtils.getOutputIndexs(tx0.getChangeOutputs());
    boolean isChangeReuse =
        pushTxError.voutsAddressReuse != null
            && !ClientUtils.intersect(pushTxError.voutsAddressReuse, changeOutputIndexs).isEmpty();

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

  public NetworkParameters getParams() {
    return params;
  }
}
