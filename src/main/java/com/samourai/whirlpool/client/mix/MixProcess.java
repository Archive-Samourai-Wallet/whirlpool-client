package com.samourai.whirlpool.client.mix;

import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.exception.ProtocolException;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.utils.ClientCryptoService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.soroban.payload.mix.*;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputRequest;
import com.samourai.whirlpool.protocol.soroban.payload.registerInput.RegisterInputResponse;
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
  private long mustMixBalanceMin;
  private long mustMixBalanceMax;
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
  private Utxo receiveUtxo;

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
      long mustMixBalanceMin,
      long mustMixBalanceMax,
      IPremixHandler premixHandler,
      IPostmixHandler postmixHandler,
      ClientCryptoService clientCryptoService,
      ChainSupplier chainSupplier) {
    this.params = params;
    this.poolId = poolId;
    this.poolDenomination = poolDenomination;
    this.mustMixBalanceMin = mustMixBalanceMin;
    this.mustMixBalanceMax = mustMixBalanceMax;
    this.premixHandler = premixHandler;
    this.postmixHandler = postmixHandler;
    this.clientCryptoService = clientCryptoService;
    this.chainSupplier = chainSupplier;
    this.bech32Util = Bech32UtilGeneric.getInstance();
  }

  protected RegisterInputRequest registerInput() throws Exception {

    // we may registerInput several times if disconnected
    if (
    /*registeredInput ||*/ confirmedInput
        || confirmedInputResponse
        || registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    // get mix settings
    UtxoWithBalance utxo = premixHandler.getUtxo();
    this.liquidity = utxo.getBalance() == poolDenomination;

    if (log.isDebugEnabled()) {
      log.debug(
          "Registering input as "
              + (this.liquidity ? "LIQUIDITY" : "MUSTMIX")
              + ":"
              + utxo.toString());
    }

    // verify fees acceptable
    checkFees(utxo.getBalance(), poolDenomination);

    // verify balance
    checkUtxoBalance(mustMixBalanceMin, mustMixBalanceMax);

    String signature = premixHandler.signMessage(poolId);
    int blockHeight = chainSupplier.getLatestBlock().height;
    RegisterInputRequest registerInputRequest =
        new RegisterInputRequest(utxo.getHash(), utxo.getIndex(), signature, blockHeight);

    registeredInput = true;
    return registerInputRequest;
  }

  protected ConfirmInputRequest confirmInput(RegisterInputResponse registerInputResponse)
      throws Exception {
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
    RSAKeyParameters serverPublicKey =
        ClientUtils.publicKeyUnserialize(registerInputResponse.mixPublicKey);
    this.blindingParams = clientCryptoService.computeBlindingParams(serverPublicKey);

    String mixId = registerInputResponse.mixId;
    String blindedBordereau64 =
        WhirlpoolProtocol.encodeBytes(clientCryptoService.blind(bordereau, blindingParams));
    String userHash = premixHandler.computeUserHash(mixId);
    ConfirmInputRequest confirmInputRequest = new ConfirmInputRequest(blindedBordereau64, userHash);

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
      MixStatusResponseRegisterOutput mixNotificationRegisterOutput) throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        // || registeredOutput => allow retrying REGISTER_OUTPUT failures
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    this.inputsHash = mixNotificationRegisterOutput.inputsHash;
    this.receiveDestination = postmixHandler.computeDestinationNext();

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

  protected RevealOutputRequest revealOutput() throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        || !registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    RevealOutputRequest revealOutputRequest =
        new RevealOutputRequest(this.receiveDestination.getAddress());

    revealedOutput = true;
    return revealOutputRequest;
  }

  protected SigningRequest signing(MixStatusResponseSigning mixNotificationSigning)
      throws Exception {
    if (!registeredInput
        || !confirmedInput
        || !confirmedInputResponse
        || !registeredOutput
        || revealedOutput
        || signed) {
      throwProtocolException();
    }

    byte[] rawTx = WhirlpoolProtocol.decodeBytes(mixNotificationSigning.transaction64);
    Transaction tx = new Transaction(params, rawTx);

    // verify tx
    int inputIndex = verifyTx(tx);

    premixHandler.signTransaction(tx, inputIndex, params);

    // verify signature
    tx.verify();

    // transmit
    String[] witnesses64 = ClientUtils.witnessSerialize64(tx.getWitness(inputIndex));
    SigningRequest signingRequest = new SigningRequest(witnesses64);

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

    long utxoBalance = premixHandler.getUtxo().getBalance();
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
    Integer outputIndex =
        ClientUtils.findTxOutputIndex(this.receiveDestination.getAddress(), tx, params);
    if (outputIndex == null) {
      throw new Exception("Output not found in tx");
    }
    receiveUtxo = new Utxo(tx.getHashAsString(), outputIndex);

    // verify my input
    UtxoWithBalance utxo = premixHandler.getUtxo();
    Integer inputIndex = TxUtil.getInstance().findInputIndex(tx, utxo.getHash(), utxo.getIndex());
    if (outputIndex == null) {
      throw new Exception("Input not found in tx");
    }

    // check fees again
    long inputValue = utxo.getBalance(); // tx.getInput(inputIndex).getValue().getValue(); is null
    long outputValue = tx.getOutput(outputIndex).getValue().getValue();
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
    List<Utxo> utxos = new ArrayList<Utxo>();
    for (TransactionInput input : inputs) {
      Utxo utxo =
          new Utxo(input.getOutpoint().getHash().toString(), input.getOutpoint().getIndex());
      utxos.add(utxo);
    }
    return WhirlpoolProtocol.computeInputsHash(utxos);
  }

  public Utxo getReceiveUtxo() {
    return receiveUtxo;
  }

  public MixDestination getReceiveDestination() {
    return receiveDestination;
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

  // only available after .registerInput()
  public boolean isLiquidity() {
    return liquidity;
  }
}
