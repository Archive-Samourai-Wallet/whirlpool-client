package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69InputComparatorBipUtxo;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.KeyBag;
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
  public Optional<Tx0> buildTx0(Tx0Config tx0Config) throws Exception {
    // preview
    Pool pool = tx0Config.getPool();
    Tx0Preview tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0Config, pool).orElse(null);
    if (tx0Preview != null) {
      // compute Tx0
      return buildTx0(tx0Config, tx0Preview);
    }
    return Optional.empty();
  }

  protected Optional<Tx0> buildTx0(Tx0Config tx0Config, Tx0Preview tx0Preview) throws Exception {
    if (log.isDebugEnabled()) {
      String poolId = tx0Preview.getPool().getPoolId();
      log.debug(
          " • Tx0["
              + poolId
              + "]: tx0Config={"
              + tx0Config
              + "}\n=> tx0Preview={"
              + tx0Preview
              + "}");
    }

    // save indexes state with Tx0Context
    BipWallet premixWallet = tx0Config.getPremixWallet();
    BipWallet changeWallet = tx0Config.getChangeWallet();
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
      BipWallet feeChangeWallet = tx0Config.getFeeChangeWallet();
      feeOrBackAddressBech32 = feeChangeWallet.getNextAddressChange().getAddressString();
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
    UtxoKeyProvider utxoKeyProvider = tx0Config.getUtxoKeyProvider();
    byte[] firstInputKey = utxoKeyProvider._getPrivKey(firstInput);
    byte[] opReturn = computeOpReturn(firstInput, firstInputKey, tx0Data);

    //
    // tx0
    //

    return buildTx0(
        tx0Config,
        sortedSpendFroms,
        premixWallet,
        tx0Preview,
        opReturn,
        feeOrBackAddressBech32,
        changeWallet,
        tx0Context);
  }

  protected Optional<Tx0> buildTx0(
      Tx0Config tx0Config,
      Collection<BipUtxo> sortedSpendFroms,
      BipWallet premixWallet,
      Tx0Preview tx0Preview,
      byte[] opReturn,
      String feeOrBackAddressBech32,
      BipWallet changeWallet,
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
      return Optional.empty(); // TX0 not possible
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
      TransactionInput input = utxoUtil.computeOutpoint(spendFrom).computeSpendInput();
      tx.addInput(input);
      if (log.isDebugEnabled()) {
        log.debug("Tx0 in: utxo=" + spendFrom);
      }
    }

    UtxoKeyProvider keyProvider = tx0Config.getUtxoKeyProvider();
    KeyBag keyBag = new KeyBag();
    keyBag.addAll(sortedSpendFroms, keyProvider);
    signTx0(tx, keyBag, keyProvider.getBipFormatSupplier());
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
    String poolId = tx0Config.getPool().getPoolId();
    log.info(
        " • Tx0["
            + poolId
            + "]: txid="
            + tx0.getTx().getHashAsString()
            + ", nbPremixs="
            + tx0.getPremixOutputs().size()
            + ", decoyTx0x2="
            + tx0.isDecoyTx0x2());
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
              + TxUtil.getInstance().getTxHex(tx)
              + "\ntx0={"
              + tx0
              + "}");
    }
    return Optional.of(tx0);
  }

  protected byte[] computeOpReturn(BipUtxo firstInput, byte[] firstInputKey, Tx0Data tx0Data)
      throws Exception {

    // use input0 for masking
    TransactionOutPoint maskingOutpoint = utxoUtil.computeOutpoint(firstInput);
    String feePaymentCode = tx0Data.getFeePaymentCode();
    byte[] feePayload = tx0Data.getFeePayload();
    return feeOpReturnImpl.computeOpReturn(
        feePaymentCode, feePayload, maskingOutpoint, firstInputKey);
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
              changeOutput,
              changeAddressBech32,
              null,
              changeWallet.getXPub(),
              false,
              bipAddress.getHdAddress().getChainIndex(),
              bipAddress.getHdAddress().getAddressIndex());
      changeUtxos.add(changeUtxo);
    }
    return changeUtxos;
  }

  public List<Tx0> tx0(Tx0Config tx0Config) throws Exception {
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
    Tx0 tx0 =
        buildTx0(tx0Config)
            .orElseThrow(
                () ->
                    new NotifiableException(
                        "Tx0 is not possible for pool: " + poolInitial.getPoolId()));
    tx0List.add(tx0);
    Collection<? extends BipUtxo> changeUtxos = tx0.getChangeUtxos();

    // Tx0 cascading for remaining pools
    if (tx0Config.isCascade()) {
      // sort pools by denomination
      List<Pool> cascadingPools = tx0PreviewService.findCascadingPools(poolInitial.getPoolId());
      Collections.sort(cascadingPools, new PoolComparatorByDenominationDesc());

      for (Pool pool : cascadingPools) {
        if (changeUtxos.isEmpty()) {
          break; // stop when no tx0 change
        }

        if (log.isDebugEnabled()) {
          log.debug(
              " • Tx0 cascading ("
                  + (tx0List.size() + 1)
                  + "/x): trying poolId="
                  + pool.getPoolId());
        }

        tx0Config = new Tx0Config(tx0Config, changeUtxos, pool);
        tx0Config._setCascading(true);
        tx0Config.setDecoyTx0x2Forced(true); // skip to next lower pool when decoy is not possible
        tx0 = this.buildTx0(tx0Config).orElse(null);
        if (tx0 != null) {
          tx0List.add(tx0);
          changeUtxos = tx0.getChangeUtxos();
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
        tx0 = tx0Retry(tx0, pushTxErrorResponse).get();
      }
    }
    throw pushTx0Exception;
  }

  private Optional<Tx0> tx0Retry(Tx0 tx0, PushTxErrorResponse pushTxErrorResponse)
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
    return buildTx0(tx0.getTx0Config(), tx0);
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
