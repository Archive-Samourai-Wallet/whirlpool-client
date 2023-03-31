package com.samourai.whirlpool.client.test;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2Service;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Service;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.provider.MockUtxoProvider;
import com.samourai.wallet.util.TestUtil;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.wallet.AbstractWhirlpoolWalletTest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCahootsTest extends AbstractWhirlpoolWalletTest {
  private static final Logger log = LoggerFactory.getLogger(AbstractCahootsTest.class);

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";
  protected static final int FEE_PER_B = 1;

  protected WalletSupplier walletSupplierSender;
  protected WalletSupplier walletSupplierCounterparty;

  protected CahootsWallet cahootsWalletSender;
  protected CahootsWallet cahootsWalletCounterparty;

  protected MockUtxoProvider utxoProviderSender;
  protected MockUtxoProvider utxoProviderCounterparty;

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

  protected Tx0x2Service tx0x2Service = new Tx0x2Service(bipFormatSupplier, params);
  protected MultiTx0x2Service multiTx0x2Service =
      new MultiTx0x2Service(bipFormatSupplier, params, tx0x2Service);

  public AbstractCahootsTest() throws Exception {
    super();
  }

  public void setUp() throws Exception {
    final HD_Wallet bip84WalletSender =
        TestUtil.computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    walletSupplierSender =
        new WalletSupplierImpl(new MemoryIndexHandlerSupplier(), bip84WalletSender);
    utxoProviderSender = new MockUtxoProvider(params, walletSupplierSender);
    cahootsWalletSender =
        new CahootsWallet(
            walletSupplierSender,
            mockChainSupplier,
            bipFormatSupplier,
            params,
            utxoProviderSender.getCahootsUtxoProvider());

    final HD_Wallet bip84WalletCounterparty =
        TestUtil.computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    walletSupplierCounterparty =
        new WalletSupplierImpl(new MemoryIndexHandlerSupplier(), bip84WalletCounterparty);
    utxoProviderCounterparty = new MockUtxoProvider(params, walletSupplierCounterparty);
    cahootsWalletCounterparty =
        new CahootsWallet(
            walletSupplierCounterparty,
            mockChainSupplier,
            bipFormatSupplier,
            params,
            utxoProviderCounterparty.getCahootsUtxoProvider());

    SENDER_RECEIVE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_RECEIVE_84[i] =
          walletSupplierSender
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_84[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_44[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP44)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_49 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_49[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP49)
              .getAddressAt(Chain.RECEIVE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_RECEIVE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_RECEIVE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              walletSupplierCounterparty
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.RECEIVE.getIndex(), i)
                  .getHdAddress());
    }

    SENDER_CHANGE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_CHANGE_84[i] =
          walletSupplierSender
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    SENDER_CHANGE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      SENDER_CHANGE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              walletSupplierSender
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_CHANGE_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_44[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP44)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_49 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_49[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP49)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_84[i] =
          walletSupplierCounterparty
              .getWallet(BIP_WALLET.DEPOSIT_BIP84)
              .getAddressAt(Chain.CHANGE.getIndex(), i)
              .getAddressString();
    }

    COUNTERPARTY_CHANGE_POSTMIX_84 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_POSTMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              walletSupplierCounterparty
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_CHANGE_POSTMIX_44 = new String[4];
    for (int i = 0; i < 4; i++) {
      COUNTERPARTY_CHANGE_POSTMIX_44[i] =
          BIP_FORMAT.LEGACY.getAddressString(
              walletSupplierCounterparty
                  .getWallet(BIP_WALLET.POSTMIX_BIP84)
                  .getAddressAt(Chain.CHANGE.getIndex(), i)
                  .getHdAddress());
    }

    SENDER_PREMIX_84 = new String[40];
    for (int i = 0; i < 40; i++) {
      SENDER_PREMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              walletSupplierSender
                  .getWallet(BIP_WALLET.PREMIX_BIP84)
                  .getAddressAt(Chain.RECEIVE.getIndex(), i)
                  .getHdAddress());
    }

    COUNTERPARTY_PREMIX_84 = new String[40];
    for (int i = 0; i < 40; i++) {
      COUNTERPARTY_PREMIX_84[i] =
          BIP_FORMAT.SEGWIT_NATIVE.getAddressString(
              walletSupplierCounterparty
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

  protected Cahoots doCahoots(
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
    cahoots.pushTx(pushTx);
    return cahoots;
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
      Assertions.assertEquals(raw, TxUtil.getInstance().getTxHex(tx));
    }
  }

  protected Map<String, Long> sortMapOutputs(Map<String, Long> map) {
    return map.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
