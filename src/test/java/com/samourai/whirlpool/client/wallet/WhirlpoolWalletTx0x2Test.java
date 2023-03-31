package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2Context;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Context;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.test.AbstractCahootsTest;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImpl;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletTx0x2Test extends AbstractCahootsTest {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0x2Test.class);

  public WhirlpoolWalletTx0x2Test() throws Exception {
    super();
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void tx0x2() throws Exception {
    int account = 0;
    Pool pool = pool001btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs(xpub);
    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
    Tx0 tx0Initiator =
        tx0Service.tx0(spendFroms, walletSupplierSender, pool, tx0Config, utxoProviderSender);

    // run Cahoots
    Tx0x2Context cahootsContextSender =
        Tx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiator);
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TX
    Transaction tx = cahoots.getTransaction();
    Assertions.assertEquals(2, tx.getInputs().size());

    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973148L);
    outputs.put(SENDER_CHANGE_84[0], 975768L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "abaf1c5b70ba0f60730cc771d27043065414925bdc8a52a12dae9a3d549b763d";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae35cd90e0000000000160014657b6afdeef6809fdabce7face295632fbd94feb98e30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f020446430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402206d3cbb0eae41ce522e1fcb186f2986c0edd0bedbbabc9cd62c102b7d82f5090a02205636ee57277025e8057ed0d681b46ca3501c2b4f920427063a8d0cf28f1f6900012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100918e82387071141a50de6dae946bde66767f2671dc0c7f88060d8c755eef8b5202205a0c7df65231d6bdcbbe4e50c2ae6323e92b8d3ca5f1e807fe58b1dadc45b09a0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx, txid, raw, outputs);
  }

  @Test
  public void tx0x2_maxOutputsEach() throws Exception {
    int account = 0;
    Pool pool = pool001btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 40000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 50000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = bipWalletSender.getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs(xpub);
    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
    Tx0 tx0Initiator =
        tx0Service.tx0(spendFroms, walletSupplierSender, pool, tx0Config, utxoProviderSender);

    // run Cahoots
    Tx0x2Context cahootsContextSender =
        Tx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiator);
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TX
    Transaction tx = cahoots.getTransaction();
    Assertions.assertEquals(2, tx.getInputs().size());

    int nbPremixSender = 35;
    int nbPremixCounterparty = 35;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 14968242L);
    outputs.put(SENDER_CHANGE_84[0], 4968242L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "8f931c48d4e15aebbc695d998e002498c1d3b380808557dcf2fab0c5c683b5eb";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff4a0000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae346430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f0000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f46430f00000000001600141439df62d219314f4629ecedcbe23e24586d3cd346430f000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141bcc24b74b6d68a6d07a34b14e6d4fd72e998a6246430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f000000000016001423631d8f88b4a47609b6c151d7bd65f27609d6d046430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f00000000001600142525a95f3378924bc5cec937c6a7a1b489c5ff8646430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f0000000000160014378ac72b08d43acd2d9e70c6791e5f186ec395dc46430f00000000001600143ac59e5cdf902524b4d721b5a633a82526c5359746430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f000000000016001440d04347d5f2696e4600a383b154a619162f542846430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f000000000016001441a73bec4bd8c083c62746fcf8617d060b3c391a46430f00000000001600144288958e3bb02ba9c6d6187fe169279c71caa4e646430f00000000001600144518c234185a62d62245d0adff79228e554c62de46430f000000000016001445cc6ccf7b32b6ba6e5f29f8f8c9a5fe2b55952946430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f0000000000160014482e4619fb70e25918bdb570b67d551d3d4aab9f46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600144ecd8a26f6fc2ae301bbc52358d95ff50137ee6b46430f0000000000160014524a759e76003300ccb475eb812e65817c6653c546430f00000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d5646430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001462e123682b149978f834a5fce14f4e71cdd133e246430f0000000000160014635a4bb83ea24dc7485d53f9cd606415cdd99b7846430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600146ff0703b7b540c70625baa21448110f560bcb25c46430f00000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc552446430f000000000016001476b64af1eb81d03ee7e9e0a6116a54830e72957346430f00000000001600147dfc158a08a2ee738ea610796c35e68f202cf06c46430f0000000000160014851204bc2e59ace9cfbe86bbc9e96898721c060d46430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf46430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e346430f0000000000160014ac64d97c6ee84eff2ce8373dfe5186f6dda8e3ac46430f0000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c46430f0000000000160014b3332b095d7ddf74a6fd94f3f9e7412390d3bed946430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b696b85812d9b961967ba20fa8790d08f8b9340b46430f0000000000160014b6e1b3638c917904cc8de4b86b40c846149d353046430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c1c95595d7b48b73f5b51414f807c5bd9f23798546430f0000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe46430f0000000000160014c88fb64ea3063496876c224711e8b93c18d4bb5346430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014cdf3140b7268772bd46ffc2d59fa399d63ecb8ba46430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014e7056147da987fc9ca73003d5b807ec145e1b4ce46430f0000000000160014e736d0bbc2bcfbec2c577223c1f75d096440fd0146430f0000000000160014e9339ff8d935d4b9205706c9db58c03b03acc35646430f0000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014f0e99871ae8ce7b56a9e91a5bea7d5e4bffcb8cc46430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde146430f0000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a46430f0000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae32cf4b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204b265e40000000000160014657b6afdeef6809fdabce7face295632fbd94feb02483045022100ece96348de4c5dfa74f30403f40acae62246ba7aed445a4f2feabb9feb818165022040de3885ddd9c0c828483bf72039e9eefd8749648a768d603c8b921c6cfb10e0012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402200fcb8b76ece8953032c3142d77d6e7232dede550547b3a4901d6fc79abcd17ff022034f01bdfaa25bd1083b6b2c55335657db2639d719e9a59f878feb3c1c658b3310121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx, txid, raw, outputs);
  }

  @Test
  public void tx0x2_pool001() throws Exception {
    log.info("Testing Tx0x2 for pool 0.001");

    int account = 0;
    Pool pool = pool001btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 500000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 1000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs(xpub);
    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.001btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(
            spendFroms, walletSupplierSender, pools, tx0Config, utxoProviderSender);

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2)cahoots).getTransactions();
    Assertions.assertEquals(1, txs.size());

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2)cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    long nbPremixSender = 4;
    long nbPremixCounterparty = 9;
    long expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 95428L);
    outputs.put(SENDER_CHANGE_84[0], 95428L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    String txstring = tx001.getHashAsString();
    String hash = TxUtil.getInstance().getTxHex(tx001);

    String txid = "5c5b9b3ee3be6566fc63fd1ef9a26c987467de75ba90c3651e184fcd7fd63e70";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff110000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3c4740100000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204c474010000000000160014657b6afdeef6809fdabce7face295632fbd94feba6870100000000001600140343e55f94af500cc2c47118385045ec3d00c55aa68701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739a687010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997a6870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fa687010000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3da68701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaa6870100000000001600146be0c5c092328f099f9c44488807fa5894131396a6870100000000001600149f657d702027d98db03966e8948cd474098031efa687010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311a687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbca687010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830024830450221009445483e4734ae38743b95440595815ca1eab7b5e433aa67339401b90cacf5c0022012de1b7619eb9eec39243216cd14738f720b9ee436898a6b7d0b4519bac4a144012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100b5fabe927d1a297ceaca65abb4a538f5317bd3fe62d8842ceb640e737f632301022068e561e4065857c4708d4ba57f63c3fb798d34563d2f4c6a9c7d9b762f9493b60121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  @Test
  public void tx0x2_cascading_pool01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.01 & 0.001");

    int account = 0;
    Pool pool = pool01btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs(xpub);
    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.01btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(
            spendFroms, walletSupplierSender, pools, tx0Config, utxoProviderSender);

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2)cahoots).getTransactions();
    Assertions.assertEquals(2, txs.size());

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2)cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973148L);
    outputs.put(SENDER_CHANGE_84[0], 975768L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "abaf1c5b70ba0f60730cc771d27043065414925bdc8a52a12dae9a3d549b763d";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae35cd90e0000000000160014657b6afdeef6809fdabce7face295632fbd94feb98e30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f020446430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402206d3cbb0eae41ce522e1fcb186f2986c0edd0bedbbabc9cd62c102b7d82f5090a02205636ee57277025e8057ed0d681b46ca3501c2b4f920427063a8d0cf28f1f6900012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100918e82387071141a50de6dae946bde66767f2671dc0c7f88060d8c755eef8b5202205a0c7df65231d6bdcbbe4e50c2ae6323e92b8d3ca5f1e807fe58b1dadc45b09a0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2)cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    nbPremixSender = 9;
    nbPremixCounterparty = 9;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    Assertions.assertEquals(
        tx001.getOutputs().get(2).getValue().getValue(),
        tx001.getOutputs().get(3).getValue().getValue()); // Change outputs equal

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 69458L);
    outputs.put(SENDER_CHANGE_84[1], 69458L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex001 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex001 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "261ddedb1ed41996aef0c4d313634fa9f7bc5a8eed25e63bd925ddeb35553fd3";
    raw =
        "0200000000010247f2cf4f176a69b9b85f0da756bc9a0cdf0fe079681d8fc174dfba99c9843c571f00000000fdffffff11d94402578c32ef36b5806707b2411b668c1ad3d10b58d9441307533fcf6bf21300000000fdffffff160000000000000000536a4c5033fcad518d60161b04c63cb46a2a5e856932f60fc658ca632d3225dae9c2883b6a2c5ad446334c3169b6beeff01b036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3520f01000000000016001440852bf6ea044204b826a182d1b75528364fd0bd520f01000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229a687010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267fa68701000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442da6870100000000001600143ac59e5cdf902524b4d721b5a633a82526c53597a687010000000000160014524a759e76003300ccb475eb812e65817c6653c5a6870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56a68701000000000016001462e123682b149978f834a5fce14f4e71cdd133e2a6870100000000001600146ff0703b7b540c70625baa21448110f560bcb25ca6870100000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc5524a6870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfa687010000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e3a687010000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154ca687010000000000160014b6e1b3638c917904cc8de4b86b40c846149d3530a687010000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafea687010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb53a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700ba687010000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408aa687010000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae0247304402202ad6a614e42db57815593ef8626389f8a1b8ae562bc34bc889818464914275f502204d159cccceb3bebb399305160a1e202e563b0a911685b1b3e78cabb18a2be1c8012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100d7e57d2592c3975701b67bd8380c8be2ff36e172ff7291ce4fd3dcde8adc3c07022009eebde77d179d3695f0851114d42eb7c655ce78897039eb88af22fc07114f790121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  @Test
  public void tx0x2_cascading_pool05() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05, 0.01, & 0.001");

    int account = 0;
    Pool pool = pool05btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs(xpub);
    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(
            spendFroms, walletSupplierSender, pools, tx0Config, utxoProviderSender);

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2)cahoots).getTransactions();
    Assertions.assertEquals(3, txs.size());

    // 0.05btc pool
    Transaction tx05 = ((MultiTx0x2)cahoots).getTransaction("0.05btc");
    Assertions.assertEquals(2, tx05.getInputs().size());
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924623L);
    outputs.put(SENDER_CHANGE_84[0], 4925147L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "0c6042a89740a1e702587f2642f37e4d6441f4cadfa4a13b6c6acd8140c5957f";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3cf244b0000000000160014657b6afdeef6809fdabce7face295632fbd94febdb264b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204464c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a464c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d464c4c00000000001600149f657d702027d98db03966e8948cd474098031ef464c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc0247304402206462c824ed72e33d771713d32622cf6903e1e8f188bd0e6d0d4c040f8291ac390220598fa874ecf8ea9165ac902dfe115c548627bb1c40505a043db229e2a90470f3012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402205b8bc10eb3d914bc8343c3e3f92f16ca1173f3a72f084ef66d895eb8a3ad26060220797b6086be7262b408d75d41d3e49e995f0d3c1f49b1d1855dabd27dd38372a30121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2)cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    nbPremixSender = 4;
    nbPremixCounterparty = 4;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 923507L);
    outputs.put(SENDER_CHANGE_84[1], 881315L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "2c4da67e5380d21b88c83721fa28f031d68801348c7dbe12bc166dda722c8159";
    raw =
        "020000000001023d7f23ab6dcfdce74c56a8010f5b437ff230237c895c26de0bed6165a64649270700000000fdffffff9ad3a37da1e6a4f27cd866dbae89f4ab212aa653656207e55feff9960a5c8c590300000000fdffffff0c0000000000000000536a4c50d585a7c944e9cf3f359ecd21917a05bc4611c8c8f7b9cedeaf9fbbc43a5ef75bcbcb9fd25154cbde1f8df93a6004036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3a3720d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c822973170e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300247304402203aa5c54206255533c16c496ce956b78d978c05c0bdacf7814bb738064b5585b20220422ad112d4d6b209a51d90afbf84d1c6c3f0dccb154a60a32cdf02bff49d2eaa012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100b576e1baecd782a5b935145abf9b568e9b2094cdfa5d476ef670edcced316b1a022074ad2031236ab38c0a08b2bfe9ba6ae6b88465135ddfa0233745ee8f2952eae70121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2)cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    nbPremixSender = 8;
    nbPremixCounterparty = 9;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    Assertions.assertEquals(
        tx001.getOutputs().get(2).getValue().getValue(),
        tx001.getOutputs().get(3).getValue().getValue()); // Change outputs equal

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47406L);
    outputs.put(SENDER_CHANGE_84[2], 47406L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "0552fa837a34021698cded82b1ed70c1488b4a6645a2dd0282df49761cb11982";
    raw =
        "0200000000010282eeb84c00bada384375794d8d6a5303f3ba08eb13172863f22213f6d37a0fde0b00000000fdffffffe9dabbd9381335712c9fb75c58b5f6d152165cfc4c9903a08977e3d5396680e40400000000fdffffff150000000000000000536a4c50a0ca7d452cb6f87450406873a19cc0a461eca666141e35dc0f298d2ff302911027799fbd081c0cd01a1e9e085d13036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae32eb90000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac322eb9000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce8412a687010000000000160014017424f9c82844a174199281729d5901fdd4d4bca68701000000000016001429eeb74c01870e0311d2994378f865ec02b8c984a6870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029a6870100000000001600143f0411e7eec430370bc856e668a2f857bbab5f01a6870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffa687010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383baba687010000000000160014524a759e76003300ccb475eb812e65817c6653c5a6870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56a6870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f182a6870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1a6870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfa687010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fa687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2a687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102473044022000fbcfe143a3bfeadc996f47134f0423583cb75d6540d980d4a534bcc6e2c6320220108bf33f4d93312bdecbe428d288aeba7a7e00a4db71cddd8589924698eefc65012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022052d5b0c0633a4056647c2ae3cb6884a10f63b47bf746a32e76e87b4e91e4169b022071e0e7598687ba8e5b759b46f26dc321471271fc2a4a583ffbf288dbf848d1df0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  protected Collection<Pool> findPoolsLowerOrEqual(String maxPoolId, PoolSupplier poolSupplier) {
    Pool highestPool = poolSupplier.findPoolById(maxPoolId);
    return poolSupplier.getPools().stream()
        .filter(pool -> pool.getDenomination() <= highestPool.getDenomination())
        .collect(Collectors.toList());
  }
}
