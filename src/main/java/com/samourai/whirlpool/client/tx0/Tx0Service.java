package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69InputComparatorBipUtxo;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.KeyBag;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.BipUtxoImpl;
import com.samourai.whirlpool.client.event.Tx0Event;
import com.samourai.whirlpool.client.exception.NotifiableException;
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

  /**
   * Generate maxOutputs premixes outputs max. Returns NULL when TX0 is not possible for this pool.
   */
  public Tx0 tx0(
      WalletSupplier walletSupplier,
      Pool pool,
      Tx0Config tx0Config,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    return tx0Opt(walletSupplier, pool, tx0Config, utxoKeyProvider)
        .orElseThrow(
            () -> new NotifiableException("Tx0 not possible for pool: " + pool.getPoolId()));
  }

  /**
   * Generate maxOutputs premixes outputs max. Returns NULL when TX0 is not possible for this pool.
   */
  public Optional<Tx0> tx0Opt(
      WalletSupplier walletSupplier,
      Pool pool,
      Tx0Config tx0Config,
      UtxoKeyProvider utxoKeyProvider)
      throws Exception {
    // compute & preview
    Optional<Tx0Preview> tx0PreviewOpt =
        tx0PreviewService.tx0PreviewOpt(tx0Config, pool.getPoolId());
    if (!tx0PreviewOpt.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("Tx0 not possible for pool: " + pool.getPoolId());
      }
      return Optional.empty();
    }
    Tx0Preview tx0Preview = tx0PreviewOpt.get();
    log.info(" • Tx0: tx0Config={" + tx0Config + "} => tx0Preview={" + tx0Preview + "}");

    Tx0 tx0 = tx0(walletSupplier, tx0Config, tx0Preview, utxoKeyProvider);
    if (tx0 == null) {
      return Optional.empty();
    }
    log.info(
        " • Tx0 result: txid="
            + tx0.getTx().getHashAsString()
            + ", nbPremixs="
            + tx0.getPremixOutputs().size());
    if (log.isDebugEnabled()) {
      log.debug("Tx0: " + tx0.toString());
    }
    return Optional.of(tx0);
  }

  public Tx0 tx0(
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
    List<BipUtxo> sortedSpendFroms = new LinkedList<>();
    sortedSpendFroms.addAll(tx0Config.getSpendFromUtxos());
    sortedSpendFroms.sort(new BIP69InputComparatorBipUtxo());

    // op_return
    if (sortedSpendFroms.isEmpty()) {
      throw new IllegalArgumentException("spendFroms should be > 0");
    }
    BipUtxo firstInput = sortedSpendFroms.get(0);
    BipAddress firstInputAddress = firstInput.getBipAddress(walletSupplier);
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

  protected byte[] computeOpReturn(BipUtxo firstInput, byte[] firstInputKey, Tx0Data tx0Data)
      throws Exception {

    // use input0 for masking
    TransactionOutPoint maskingOutpoint = utxoUtil.computeOutpoint(firstInput, params);
    String feePaymentCode = tx0Data.getFeePaymentCode();
    byte[] feePayload = tx0Data.getFeePayload();
    return feeOpReturnImpl.computeOpReturn(
        feePaymentCode, feePayload, maskingOutpoint, firstInputKey);
  }

  protected Tx0 buildTx0(
      Tx0Config tx0Config,
      Collection<BipUtxo> sortedSpendFroms,
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
    int nbPremix =
        tx0PreviewService.capNbPremix(tx0Preview.getNbPremix(), tx0Preview.getPool(), false);

    // verify

    if (sortedSpendFroms.size() <= 0) {
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
      return null; // TX0 not possible
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
    // 1 or 2 change output(s) [Tx0]
    // 2 or 3 change outputs [Decoy Tx0x2]
    //
    List<TransactionOutput> changeOutputs = new LinkedList<>();
    List<BipAddress> changeOutputsAddresses = new LinkedList<>();

    Collection<Long> changeAmounts = tx0Preview.getChangeAmounts();
    if (!changeAmounts.isEmpty()) {
      // change outputs
      for (long changeAmount : changeAmounts) {
        BipAddress changeAddress = changeWallet.getNextAddressChange();
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
    for (BipUtxo spendFrom : sortedSpendFroms) {
      TransactionInput input = utxoUtil.computeOutpoint(spendFrom, params).computeSpendInput();
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
    List<? extends BipUtxo> changeUtxos =
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

  private List<? extends BipUtxo> computeChangeUtxos(
      List<TransactionOutput> changeOutputs,
      List<BipAddress> changeOutputsAddresses,
      BipWallet changeWallet) {
    List<BipUtxo> changeUtxos = new LinkedList<>();
    for (int i = 0; i < changeOutputs.size(); i++) {
      TransactionOutput changeOutput = changeOutputs.get(i);
      BipAddress bipAddress = changeOutputsAddresses.get(i);
      String changeAddressBech32 = bipAddress.getAddressString();
      BipUtxo changeUtxo =
          new BipUtxoImpl(
              changeOutput.getParentTransactionHash().toString(),
              changeOutput.getIndex(),
              changeOutput.getValue().getValue(),
              changeAddressBech32,
              null,
              changeWallet.getXPub(),
              false,
              bipAddress.getHdAddress().getChainIndex(),
              bipAddress.getHdAddress().getAddressIndex(),
              changeOutput.getScriptBytes());
      changeUtxos.add(changeUtxo);
    }
    return changeUtxos;
  }

  public List<Tx0> tx0Cascade(
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
      log.debug(" > Tx0 cascading for poolId=" + poolInitial.getPoolId() + "... (1/x)");
    }
    Tx0 tx0 = tx0(walletSupplier, poolInitial, tx0Config, utxoKeyProvider);
    tx0List.add(tx0);
    Collection<? extends BipUtxo> changeUtxos = tx0.getChangeUtxos();

    // Tx0 cascading for remaining pools
    while (poolsIter.hasNext()) {
      Pool pool = poolsIter.next();
      if (changeUtxos.isEmpty()) {
        break; // stop when no tx0 change
      }

      if (log.isDebugEnabled()) {
        log.debug(
            " > Tx0 cascading for poolId="
                + pool.getPoolId()
                + "... ("
                + (tx0List.size() + 1)
                + "/x)");
      }

      tx0Config = new Tx0Config(tx0Config, changeUtxos);
      tx0Config._setCascading(true);
      tx0 = tx0Opt(walletSupplier, pool, tx0Config, utxoKeyProvider).orElse(null);
      if (tx0 != null) {
        tx0List.add(tx0);
        changeUtxos = tx0.getChangeUtxos();
      } else {
        // Tx0 is not possible for this pool, skip to next lower pool
      }
    }
    List<String> poolIds =
        tx0List.stream()
            .map(t -> t.getPool().getPoolId() + ":" + (t.isDecoyTx0x2() ? "decoy" : "noDecoy"))
            .collect(Collectors.toList());
    log.info("Tx0 cascading success on " + tx0List.size() + " pools (" + poolIds + ")");
    return tx0List;
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
    return tx0(walletSupplier, tx0.getTx0Config(), tx0, utxoKeyProvider);
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
