package com.samourai.whirlpool.client.mix;

import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.TxUtil;
import com.samourai.wallet.utxo.UtxoDetail;
import com.samourai.wallet.utxo.UtxoDetailImpl;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.rest.RegisterOutputRequest;
import com.samourai.whirlpool.protocol.websocket.messages.*;
import com.samourai.whirlpool.protocol.websocket.notifications.ConfirmInputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RegisterOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.RevealOutputMixStatusNotification;
import com.samourai.whirlpool.protocol.websocket.notifications.SigningMixStatusNotification;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bitcoinj.core.*;
import org.bouncycastle.crypto.params.RSABlindingParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixProcess {
  private static final Logger log = LoggerFactory.getLogger(MixProcess.class);
  private NetworkParameters params;
  private String poolId;
  private long poolDenomination;
  private IPremixHandler premixHandler;
  private IPostmixHandler postmixHandler;
  private ClientCryptoService clientCryptoService;
  private ChainSupplier chainSupplier;
  private Bech32UtilGeneric bech32Util;

  // hard limit for acceptable fees
  private static final long MAX_ACCEPTABLE_FEES = 100000;

  // mix data
  private byte[] signedBordereau; // will get it on RegisterInputResponse
  private String inputsHash; // will get it on REGISTER_OUTPUT

  // computed values
  private boolean liquidity;
  private byte[] bordereau;
  private RSABlindingParameters blindingParams;
  private MixDestination receiveDestination;
  private UtxoDetail receiveUtxo;

  // security checks
  private boolean registeredInput;
  private boolean confirmedInput;
  private boolean confirmedInputResponse;
  private boolean registeredOutput;
  private boolean revealedOutput;
  private boolean signed;

  public MixProcess(
      NetworkParameters params,
      String poolId,
      long poolDenomination,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ClientCryptoService clientCryptoService,
      ChainSupplier chainSupplier) {
    this.params = params;
    this.poolId = poolId;
    this.poolDenomination = poolDenomination;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
    this.clientCryptoService = clientCryptoService;
    this.chainSupplier = chainSupplier;
    this.bech32Util = Bech32UtilGeneric.getInstance();
  }

  protected RegisterInputRequest registerInput(SubscribePoolResponse subscribePoolResponse)
      throws Exception {

    // we may registerInput several times if disconnected
    if (
    /*registeredInput ||*/ confirmedInput
        || confirmedInputResponse
        || registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    // check denomination
    long actualDenomination = subscribePoolResponse.denomination;
    if (poolDenomination != actualDenomination) {
      log.error(
          "Invalid denomination: expected=" + poolDenomination + ", actual=" + actualDenomination);
      throw new NotifiableException("Unexpected denomination from server");
    }

    // get mix settings
    UtxoDetail utxo = premixHandler.getUtxo();
    String serverNetworkId = subscribePoolResponse.networkId;
    if (!params.getPaymentProtocolId().equals(serverNetworkId)) {
      throw new Exception(
          "Client/server networkId mismatch: server is runinng "
              + serverNetworkId
              + ", client is expecting "
              + params.getPaymentProtocolId());
    }
    this.liquidity = utxo.getValueLong() == poolDenomination;

    if (log.isDebugEnabled()) {
      log.debug("Registering input as " + (this.liquidity ? "LIQUIDITY" : "MUSTMIX"));
    }

    // verify fees acceptable
    checkFees(utxo.getValueLong(), poolDenomination);

    // verify balance
    long mustMixBalanceMin = subscribePoolResponse.mustMixBalanceMin;
    long mustMixBalanceMax = subscribePoolResponse.mustMixBalanceMax;
    checkUtxoBalance(mustMixBalanceMin, mustMixBalanceMax);

    String signature = premixHandler.signMessage(poolId);
    int blockHeight = chainSupplier.getLatestBlock().height;
    RegisterInputRequest registerInputRequest =
        new RegisterInputRequest(
            poolId,
            utxo.getTxHash(),
            utxo.getTxOutputIndex(),
            signature,
            this.liquidity,
            blockHeight);

    registeredInput = true;
    return registerInputRequest;
  }

  protected ConfirmInputRequest confirmInput(
      ConfirmInputMixStatusNotification confirmInputMixStatusNotification) throws Exception {
    // we may confirmInput several times before getting confirmedInputResponse
    if (!registeredInput
        || /*confirmedInput ||*/ confirmedInputResponse
        || registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    // generate a secret bordereau. keep it private and register INPUT with blindedBordereau
    // bordereau will be provided with unblindedSignedBordereau to register POSTMIX with another
    // identity
    this.bordereau = ClientUtils.generateBordereau();
    byte[] publicKey = WhirlpoolProtocol.decodeBytes(confirmInputMixStatusNotification.publicKey64);
    RSAKeyParameters serverPublicKey = ClientUtils.publicKeyUnserialize(publicKey);
    this.blindingParams = clientCryptoService.computeBlindingParams(serverPublicKey);

    String mixId = confirmInputMixStatusNotification.mixId;
    String blindedBordereau64 =
        WhirlpoolProtocol.encodeBytes(clientCryptoService.blind(bordereau, blindingParams));
    String userHash = premixHandler.computeUserHash(mixId);
    ConfirmInputRequest confirmInputRequest =
        new ConfirmInputRequest(mixId, blindedBordereau64, userHash);

    confirmedInput = true;
    return confirmInputRequest;
  }

  protected void onConfirmInputResponse(ConfirmInputResponse confirmInputResponse)
      throws Exception {
    if (!registeredInput
        || !confirmedInput
        || confirmedInputResponse
        || registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    this.signedBordereau = WhirlpoolProtocol.decodeBytes(confirmInputResponse.signedBordereau64);

    confirmedInputResponse = true;
  }

  protected RegisterOutputRequest registerOutput(
      RegisterOutputMixStatusNotification registerOutputMixStatusNotification) throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        // || registeredOutput => allow retrying REGISTER_OUTPUT failures
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    this.inputsHash = registerOutputMixStatusNotification.getInputsHash();
    this.receiveDestination = postmixHandler.computeDestination();

    String unblindedSignedBordereau64 =
        WhirlpoolProtocol.encodeBytes(clientCryptoService.unblind(signedBordereau, blindingParams));
    String bordereau64 = WhirlpoolProtocol.encodeBytes(bordereau);
    RegisterOutputRequest registerOutputRequest =
        new RegisterOutputRequest(
            inputsHash,
            unblindedSignedBordereau64,
            this.receiveDestination.getAddress(),
            bordereau64);

    registeredOutput = true; // mark as registered even if failed

    return registerOutputRequest;
  }

  protected RevealOutputRequest revealOutput(
      RevealOutputMixStatusNotification revealOutputMixStatusNotification) throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        || !registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    RevealOutputRequest revealOutputRequest =
        new RevealOutputRequest(
            revealOutputMixStatusNotification.mixId, this.receiveDestination.getAddress());

    revealedOutput = true;
    return revealOutputRequest;
  }

  protected SigningRequest signing(SigningMixStatusNotification signingMixStatusNotification)
      throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        || !registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    byte[] rawTx = WhirlpoolProtocol.decodeBytes(signingMixStatusNotification.transaction64);
    Transaction tx = new Transaction(params, rawTx);

    // verify tx
    int inputIndex = verifyTx(tx);

    premixHandler.signTransaction(tx, inputIndex, params);

    // verify signature
    tx.verify();

    // transmit
    String[] witnesses64 = ClientUtils.witnessSerialize64(tx.getWitness(inputIndex));
    SigningRequest signingRequest =
        new SigningRequest(signingMixStatusNotification.mixId, witnesses64);

    signed = true;
    return signingRequest;
  }

  //

  private void checkFees(long inputValue, long outputValue) throws NotifiableException {
    long fees = inputValue - outputValue;

    if (liquidity && fees > 0) {
      throw new NotifiableException("Should not pay fees as a liquidity");
    }
    if (fees > MAX_ACCEPTABLE_FEES) {
      log.error(
          "Fees abnormally abnormally: fees="
              + fees
              + ", MAX_ACCEPTABLE_FEES="
              + MAX_ACCEPTABLE_FEES);
      throw new NotifiableException("Fees abnormally high");
    }
  }

  private void checkUtxoBalance(long mustMixBalanceMin, long mustMixBalanceMax)
      throws NotifiableException {
    long premixBalanceMin =
        WhirlpoolProtocol.computePremixBalanceMin(poolDenomination, mustMixBalanceMin, liquidity);
    long premixBalanceMax =
        WhirlpoolProtocol.computePremixBalanceMax(poolDenomination, mustMixBalanceMax, liquidity);

    long utxoBalance = premixHandler.getUtxo().getValueLong();
    if (utxoBalance < premixBalanceMin) {
      throw new NotifiableException(
          "Too low utxo-balance="
              + utxoBalance
              + ". (expected: "
              + premixBalanceMin
              + " <= utxo-balance <= "
              + premixBalanceMax
              + ")");
    }

    if (utxoBalance > premixBalanceMax) {
      throw new NotifiableException(
          "Too high utxo-balance="
              + utxoBalance
              + ". (expected: "
              + premixBalanceMin
              + " <= utxo-balance <= "
              + premixBalanceMax
              + ")");
    }
  }

  private int verifyTx(Transaction tx) throws Exception {
    // verify inputsHash
    String txInputsHash = computeInputsHash(tx.getInputs());
    if (!txInputsHash.equals(inputsHash)) {
      throw new Exception("Inputs hash mismatch. Aborting.");
    }

    // verify my output
    String outputAddress = this.receiveDestination.getAddress();
    Integer outputIndex = ClientUtils.findTxOutputIndex(outputAddress, tx, params);
    if (outputIndex == null) {
      throw new Exception("Output not found in tx");
    }
    long outputValue = tx.getOutput(outputIndex).getValue().getValue();
    receiveUtxo =
        new UtxoDetailImpl(tx.getHashAsString(), outputIndex, outputValue, outputAddress, null);

    // verify my input
    UtxoDetail utxo = premixHandler.getUtxo();
    Integer inputIndex =
        TxUtil.getInstance().findInputIndex(tx, utxo.getTxHash(), utxo.getTxOutputIndex());
    if (outputIndex == null) {
      throw new Exception("Input not found in tx");
    }

    // check fees again
    long inputValue = utxo.getValueLong(); // tx.getInput(inputIndex).getValue().getValue(); is null
    checkFees(inputValue, outputValue);

    // as many inputs as outputs
    if (tx.getInputs().size() != tx.getOutputs().size()) {
      log.error(
          "inputs.size = " + tx.getInputs().size() + ", outputs.size=" + tx.getOutputs().size());
      throw new Exception("Inputs size vs outputs size mismatch");
    }

    // each input should have unique prev-tx
    Set<String> uniquePrevTxs = new HashSet<String>();
    for (TransactionInput input : tx.getInputs()) {
      // check for prev-tx reuse
      String prevTxid = input.getOutpoint().getHash().toString();
      if (uniquePrevTxs.contains(prevTxid)) {
        throw new Exception("Prev-tx reuse detected: " + prevTxid);
      }
      uniquePrevTxs.add(prevTxid);
    }

    Set<String> uniqueAdresses = new HashSet<String>();
    for (TransactionOutput output : tx.getOutputs()) {
      // each output value should be denomination
      if (output.getValue().getValue() != poolDenomination) {
        log.error(
            "outputValue=" + output.getValue().getValue() + ", denomination=" + poolDenomination);
        throw new Exception("Output value mismatch");
      }

      // check output-address reuse
      String outputAddressBech32 = bech32Util.getAddressFromScript(output);
      if (uniqueAdresses.contains(outputAddressBech32)) {
        throw new Exception("Address reuse detected for output: " + outputAddressBech32);
      }
      uniqueAdresses.add(outputAddressBech32);
    }
    return inputIndex;
  }

  private String computeInputsHash(List<TransactionInput> inputs) {
    List<Utxo> utxos = new ArrayList<>();
    for (TransactionInput input : inputs) {
      Utxo utxo =
          new Utxo(input.getOutpoint().getHash().toString(), input.getOutpoint().getIndex());
      utxos.add(utxo);
    }
    return WhirlpoolProtocol.computeInputsHash(utxos);
  }

  public UtxoDetail getReceiveUtxo() {
    return receiveUtxo;
  }

  private boolean throwProtocolException() throws Exception {
    String message =
        "Protocol exception: "
            + " registeredInput="
            + registeredInput
            + " confirmedInput="
            + confirmedInput
            + ", confirmedInputResponse="
            + confirmedInputResponse
            + ", registeredOutput="
            + registeredOutput
            + ", revealedOutput="
            + revealedOutput
            + ", signed="
            + signed;
    throw new ProtocolException(message);
  }
}
