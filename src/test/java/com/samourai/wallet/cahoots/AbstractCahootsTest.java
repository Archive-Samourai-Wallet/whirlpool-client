package com.samourai.wallet.cahoots;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Result;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Service;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.test.MockUtxoSupplier;
import com.samourai.whirlpool.client.tx0.AbstractTx0ServiceV1Test;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0x2CahootsResult;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCahootsTest extends AbstractTx0ServiceV1Test {
  private static final Logger log = LoggerFactory.getLogger(AbstractCahootsTest.class);

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  protected WhirlpoolWallet whirlpoolWalletSender;
  protected WhirlpoolWallet whirlpoolWalletCounterparty;
  protected CahootsWallet cahootsWalletCounterparty;

  protected MockUtxoSupplier utxoProviderSender;
  protected MockUtxoSupplier utxoProviderCounterparty;

  protected static String[] SENDER_RECEIVE_84;
  protected static String[] COUNTERPARTY_RECEIVE_84;
  protected static String[] COUNTERPARTY_RECEIVE_44;
  protected static String[] COUNTERPARTY_RECEIVE_49;
  protected static String[] COUNTERPARTY_RECEIVE_POSTMIX_84;
  protected static String[] SENDER_CHANGE_84;
  protected static String[] SENDER_CHANGE_POSTMIX_84;
  protected static String[] COUNTERPARTY_CHANGE_44;
  protected static String[] COUNTERPARTY_CHANGE_49;
  protected static String[] COUNTERPARTY_CHANGE_84;
  protected static String[] COUNTERPARTY_CHANGE_POSTMIX_44;
  protected static String[] COUNTERPARTY_CHANGE_POSTMIX_84;
  protected static String[] SENDER_PREMIX_84;
  protected static String[] COUNTERPARTY_PREMIX_84;

  protected Tx0x2Service tx0x2Service;

  public AbstractCahootsTest() throws Exception {
    super();
  }

  public void setup() throws Exception {
    super.setup();

    tx0x2Service = new Tx0x2Service(bipFormatSupplier, tx0Service);

    WhirlpoolWalletConfig whirlpoolWalletConfigSender = computeWhirlpoolWalletConfig(false);
    whirlpoolWalletSender =
        computeWhirlpoolWallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR, whirlpoolWalletConfigSender);
    utxoProviderSender = new MockUtxoSupplier(whirlpoolWalletSender);

    WhirlpoolWalletConfig whirlpoolWalletConfigCounterparty = computeWhirlpoolWalletConfig(false);
    whirlpoolWalletCounterparty =
        computeWhirlpoolWallet(
            SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY, whirlpoolWalletConfigCounterparty);
    utxoProviderCounterparty = new MockUtxoSupplier(whirlpoolWalletCounterparty);
    cahootsWalletCounterparty = whirlpoolWalletCounterparty.getCahootsWallet();

    SENDER_RECEIVE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_RECEIVE_84[i] =
          whirlpoolWalletSender
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_84[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_44[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP44)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_49 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_49[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP49)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              whirlpoolWalletCounterparty
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.RECEIVE.getIndex(), i)
                  .getHdAddress());
    }

    SENDER_CHANGE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_CHANGE_84[i] =
          whirlpoolWalletSender
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    SENDER_CHANGE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_CHANGE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              whirlpoolWalletSender
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_CHANGE_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_44[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP44)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_49 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_49[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP49)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_84[i] =
          whirlpoolWalletCounterparty
              .getWalletSupplier()
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              whirlpoolWalletCounterparty
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_CHANGE_POSTMIX_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_POSTMIX_44[i] =
          BIP_FORMAT.LEGACY.getAddressString(
              whirlpoolWalletCounterparty
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    SENDER_PREMIX_84 = new String[40];
    for (int i = 0; i < 40; i++) {
      SENDER_PREMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              whirlpoolWalletSender
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.PREMIX_BIP84)
                  .getAddressAt(Chain.RECEIVE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_PREMIX_84 = new String[40];
    for (int i = 0; i < 40; i++) {
      COUNTERPARTY_PREMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              whirlpoolWalletCounterparty
                  .getWalletSupplier()
                  .getWallet(BIP_WALLET.PREMIX_BIP84)
                  .getAddressAt(Chain.RECEIVE.getIndex(), i)
                  .getHdAddress());
    }
  }

  protected Cahoots cleanPayload(String payloadStr) throws Exception {
    Cahoots copy = Cahoots.parse(payloadStr);
    CahootsTestUtil.cleanPayload(copy);
    return copy;
  }

  protected void verify(String expectedPayload, String payloadStr) throws Exception {
    payloadStr = cleanPayload(payloadStr).toJSONString();
    Assertions.assertEquals(expectedPayload, payloadStr);
  }

  protected void verify(
      String expectedPayload,
      ManualCahootsMessage cahootsMessage,
      boolean lastStep,
      CahootsType type,
      CahootsTypeUser typeUser)
      throws Exception {
    verify(expectedPayload, cahootsMessage.getCahoots().toJSONString());
    Assertions.assertEquals(lastStep, cahootsMessage.isDone());
    Assertions.assertEquals(type, cahootsMessage.getType());
    Assertions.assertEquals(typeUser, cahootsMessage.getTypeUser());
  }

  protected CahootsResult doCahoots(
      AbstractCahootsService cahootsService,
      CahootsContext cahootsContextSender,
      CahootsContext cahootsContextCp,
      String[] EXPECTED_PAYLOADS)
      throws Exception {
    int nbSteps =
        EXPECTED_PAYLOADS != null
            ? EXPECTED_PAYLOADS.length
            : ManualCahootsMessage.getNbSteps(cahootsContextSender.getCahootsType());

    // sender => _0
    String lastPayload = cahootsService.startInitiator(cahootsContextSender).toJSONString();
    if (log.isDebugEnabled()) {
      log.debug("#0 SENDER => " + lastPayload);
    }
    if (EXPECTED_PAYLOADS != null) {
      verify(EXPECTED_PAYLOADS[0], lastPayload);
    }

    // counterparty => _1
    lastPayload =
        cahootsService
            .startCollaborator(cahootsContextCp, Cahoots.parse(lastPayload))
            .toJSONString();
    if (log.isDebugEnabled()) {
      log.debug("#1 COUNTERPARTY => " + lastPayload);
    }
    if (EXPECTED_PAYLOADS != null) {
      verify(EXPECTED_PAYLOADS[1], lastPayload);
    }

    for (int i = 2; i < nbSteps; i++) {
      if (i % 2 == 0) {
        // sender
        lastPayload =
            cahootsService.reply(cahootsContextSender, Cahoots.parse(lastPayload)).toJSONString();
        if (log.isDebugEnabled()) {
          log.debug("#" + i + " SENDER => " + lastPayload);
        }
      } else {
        // counterparty
        lastPayload =
            cahootsService.reply(cahootsContextCp, Cahoots.parse(lastPayload)).toJSONString();
        if (log.isDebugEnabled()) {
          log.debug("#" + i + " COUNTERPARTY => " + lastPayload);
        }
      }
      if (EXPECTED_PAYLOADS != null) {
        verify(EXPECTED_PAYLOADS[i], lastPayload);
      }
    }
    Cahoots cahoots = Cahoots.parse(lastPayload);

    // sender broadcasts signed cahoots
    CahootsResult cahootsResult =
        cahootsService.computeCahootsResult(cahootsContextSender, cahoots);
    cahootsResult.pushTx(pushTx);
    return cahootsResult;
  }

  protected void verifyTx(
      Transaction tx, String txid, String raw, Map<String, Long> outputsExpected) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(tx.toString());
    }

    Map<String, Long> outputsActuals = new LinkedHashMap<>();
    for (TransactionOutput txOutput : tx.getOutputs()) {
      if (!txOutput.getScriptPubKey().isOpReturn()) {
        String address = bipFormatSupplier.getToAddress(txOutput);
        outputsActuals.put(address, txOutput.getValue().getValue());
      }
    }
    // sort by value ASC to comply with UTXOComparator
    outputsActuals = sortMapOutputs(outputsActuals);
    outputsExpected = sortMapOutputs(outputsExpected);
    if (log.isDebugEnabled()) {
      log.debug("outputsActuals: " + outputsActuals);
    }
    Assertions.assertEquals(outputsExpected, outputsActuals);

    if (txid != null) {
      Assertions.assertEquals(txid, tx.getHashAsString());
    }
    if (raw != null) {
      Assertions.assertEquals(raw, txUtil.getTxHex(tx));
    }
    // verify bip69 sorted
    Assertions.assertTrue(txUtil.isBip69Sorted(tx));
  }

  protected Map<String, Long> sortMapOutputs(Map<String, Long> map) {
    return map.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  protected void assertTx0x2(
      Tx0x2Result tx0x2Result,
      String poolId,
      String txid,
      String raw,
      int nbInputs,
      int nbPremixSender,
      int nbPremixCounterparty,
      long premixValue,
      long changeValueSender,
      long changeValueCp,
      long samouraiFeeValue,
      int senderChangeIndex,
      int counterpartyChangeIndex,
      int senderPremixIndex,
      int counterpartyPremixIndex)
      throws Exception {

    // verify TX0
    Tx0 tx0 = tx0x2Result.getTx0Result().getByPoolId(poolId).get();
    assertTx0(
        tx0,
        poolId,
        false,
        nbPremixSender + nbPremixCounterparty,
        Arrays.asList(changeValueSender, changeValueCp));

    // verify tx0x2
    Tx0x2CahootsResult tx0x2CahootsResult = tx0.getTx0x2CahootsResult();
    Assertions.assertEquals(nbPremixSender, tx0x2CahootsResult.getNbPremixSender());
    Assertions.assertEquals(nbPremixCounterparty, tx0x2CahootsResult.getNbPremixCounterparty());
    Assertions.assertEquals(changeValueSender, tx0x2CahootsResult.getChangeAmountSender());
    Assertions.assertEquals(changeValueCp, tx0x2CahootsResult.getChangeAmountCounterparty());
    Assertions.assertEquals(nbPremixSender + nbPremixCounterparty, tx0.getNbPremix());
    Assertions.assertEquals(samouraiFeeValue, tx0.getFeeValue());
    Assertions.assertEquals(premixValue, tx0.getPremixValue());

    Tx0x2 cahoots = tx0x2Result.getCahoots();
    Transaction tx = cahoots.getTransaction(poolId);
    Assertions.assertEquals(nbInputs, tx.getInputs().size(), "inputs");
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx.getOutputs().size(), "outputs");

    // verify TX
    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[counterpartyChangeIndex], changeValueCp);
    if (log.isDebugEnabled()) {
      log.debug(
          "COUNTERPARTY_CHANGE_84["
              + counterpartyChangeIndex
              + "] = "
              + COUNTERPARTY_CHANGE_84[counterpartyChangeIndex]);
    }
    outputs.put(SENDER_CHANGE_84[senderChangeIndex], changeValueSender);
    if (log.isDebugEnabled()) {
      log.debug(
          "SENDER_CHANGE_84[" + senderChangeIndex + "] = " + SENDER_CHANGE_84[senderChangeIndex]);
    }
    for (int i = senderPremixIndex; i < (senderPremixIndex + nbPremixSender); i++) {
      outputs.put(SENDER_PREMIX_84[i], premixValue);
      if (log.isDebugEnabled()) {
        log.debug("SENDER_PREMIX_84[" + i + "] = " + SENDER_PREMIX_84[i]);
      }
    }
    for (int i = counterpartyPremixIndex;
        i < (counterpartyPremixIndex + nbPremixCounterparty);
        i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], premixValue);
      if (log.isDebugEnabled()) {
        log.debug("COUNTERPARTY_PREMIX_84[" + i + "] = " + SENDER_PREMIX_84[i]);
      }
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, samouraiFeeValue);
    if (log.isDebugEnabled()) {
      log.debug("MOCK_SAMOURAI_FEE_ADDRESS = " + MOCK_SAMOURAI_FEE_ADDRESS);
    }
    verifyTx(tx, txid, raw, outputs);
  }

  protected void assertTx0x2State(
      int senderChangeIndex,
      int counterpartyChangeIndex,
      int nbPremixSender,
      int nbPremixCounterparty) {
    // DEPOSIT receive indexs
    Assertions.assertEquals(
        0,
        whirlpoolWalletSender
            .getWalletSupplier()
            .getWallet(BIP_WALLET.DEPOSIT_BIP84)
            .getIndexHandlerReceive()
            .get(),
        "senderDepositIndex");
    Assertions.assertEquals(
        0,
        whirlpoolWalletCounterparty
            .getWalletSupplier()
            .getWallet(BIP_WALLET.DEPOSIT_BIP84)
            .getIndexHandlerReceive()
            .get(),
        "counterpartyDepositIndex");

    // DEPOSIT change indexs
    Assertions.assertEquals(
        senderChangeIndex,
        whirlpoolWalletSender
            .getWalletSupplier()
            .getWallet(BIP_WALLET.DEPOSIT_BIP84)
            .getIndexHandlerChange()
            .get(),
        "senderChangeIndex");
    Assertions.assertEquals(
        counterpartyChangeIndex,
        whirlpoolWalletCounterparty
            .getWalletSupplier()
            .getWallet(BIP_WALLET.DEPOSIT_BIP84)
            .getIndexHandlerChange()
            .get(),
        "counterpartyChangeIndex");

    // PREMIX receive indexs
    Assertions.assertEquals(
        nbPremixSender,
        whirlpoolWalletSender
            .getWalletSupplier()
            .getWallet(BIP_WALLET.PREMIX_BIP84)
            .getIndexHandlerReceive()
            .get(),
        "senderPremixIndex");
    Assertions.assertEquals(
        nbPremixCounterparty,
        whirlpoolWalletCounterparty
            .getWalletSupplier()
            .getWallet(BIP_WALLET.PREMIX_BIP84)
            .getIndexHandlerReceive()
            .get(),
        "counterpartyPremixIndex");
  }
}
