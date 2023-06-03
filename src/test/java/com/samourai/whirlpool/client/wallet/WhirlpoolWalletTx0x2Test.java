package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2Context;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Context;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
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

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy()}
   */
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
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973146L);
    outputs.put(SENDER_CHANGE_84[0], 975766L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "af1783601244ce85392a4ca57dda06dbf1620cc717e673bfcfcd0ad95c10c280";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae35ad90e0000000000160014657b6afdeef6809fdabce7face295632fbd94feb96e30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f020446430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402200a7f7ccf273ea6394aa7e8679f9f8893b8a735d7ac84184914dea9ee2783b8250220603738a85058c7040fca06fd190fc37c3af1ea53ba46e897224c396dd9df34e8012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100b1e856b903538d31002270efd15bb1ce2279f2fde94a128331b0060c1423aaad022012374c9732f23eaa245f293e1eb56d06da98de3b83a7ea59e777d428a6537aa40121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 14968240L);
    outputs.put(SENDER_CHANGE_84[0], 4968240L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "eef39fb7db957447f6ab2189c6911515f52f508e3556824562a875daef872ef0";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff4a0000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae346430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f0000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f46430f00000000001600141439df62d219314f4629ecedcbe23e24586d3cd346430f000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141bcc24b74b6d68a6d07a34b14e6d4fd72e998a6246430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f000000000016001423631d8f88b4a47609b6c151d7bd65f27609d6d046430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f00000000001600142525a95f3378924bc5cec937c6a7a1b489c5ff8646430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f0000000000160014378ac72b08d43acd2d9e70c6791e5f186ec395dc46430f00000000001600143ac59e5cdf902524b4d721b5a633a82526c5359746430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f000000000016001440d04347d5f2696e4600a383b154a619162f542846430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f000000000016001441a73bec4bd8c083c62746fcf8617d060b3c391a46430f00000000001600144288958e3bb02ba9c6d6187fe169279c71caa4e646430f00000000001600144518c234185a62d62245d0adff79228e554c62de46430f000000000016001445cc6ccf7b32b6ba6e5f29f8f8c9a5fe2b55952946430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f0000000000160014482e4619fb70e25918bdb570b67d551d3d4aab9f46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600144ecd8a26f6fc2ae301bbc52358d95ff50137ee6b46430f0000000000160014524a759e76003300ccb475eb812e65817c6653c546430f00000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d5646430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001462e123682b149978f834a5fce14f4e71cdd133e246430f0000000000160014635a4bb83ea24dc7485d53f9cd606415cdd99b7846430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600146ff0703b7b540c70625baa21448110f560bcb25c46430f00000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc552446430f000000000016001476b64af1eb81d03ee7e9e0a6116a54830e72957346430f00000000001600147dfc158a08a2ee738ea610796c35e68f202cf06c46430f0000000000160014851204bc2e59ace9cfbe86bbc9e96898721c060d46430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf46430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e346430f0000000000160014ac64d97c6ee84eff2ce8373dfe5186f6dda8e3ac46430f0000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c46430f0000000000160014b3332b095d7ddf74a6fd94f3f9e7412390d3bed946430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b696b85812d9b961967ba20fa8790d08f8b9340b46430f0000000000160014b6e1b3638c917904cc8de4b86b40c846149d353046430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c1c95595d7b48b73f5b51414f807c5bd9f23798546430f0000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe46430f0000000000160014c88fb64ea3063496876c224711e8b93c18d4bb5346430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014cdf3140b7268772bd46ffc2d59fa399d63ecb8ba46430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014e7056147da987fc9ca73003d5b807ec145e1b4ce46430f0000000000160014e736d0bbc2bcfbec2c577223c1f75d096440fd0146430f0000000000160014e9339ff8d935d4b9205706c9db58c03b03acc35646430f0000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014f0e99871ae8ce7b56a9e91a5bea7d5e4bffcb8cc46430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde146430f0000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a46430f0000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae30cf4b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204b065e40000000000160014657b6afdeef6809fdabce7face295632fbd94feb02473044022041eb30927238c436c8419006a8361996b23b8d7695d094b4f3e44595fa41ad620220225bebd6b76443f14f348821cd3522617213d73e9fb808a69d2738405efa56a9012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402206536191a4f5856d6c001a500ef88e7e776c5b191b41bbfd63c6603f9918b3f16022059d9534c31f7623ac40228ac401369e0d955f3bfb666d038754a32641e9a05430121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx, txid, raw, outputs);
  }

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_pool001()}
   */
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
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 95426L);
    outputs.put(SENDER_CHANGE_84[0], 95426L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    String txid = "08e09f16180424081302a5ac797167aa54f334a903201b4b5a6cacc54d9c6ca9";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff110000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3c2740100000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204c274010000000000160014657b6afdeef6809fdabce7face295632fbd94feba6870100000000001600140343e55f94af500cc2c47118385045ec3d00c55aa68701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739a687010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997a6870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fa687010000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3da68701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaa6870100000000001600146be0c5c092328f099f9c44488807fa5894131396a6870100000000001600149f657d702027d98db03966e8948cd474098031efa687010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311a687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbca687010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002483045022100895779ae04eda729b566fb6c08b50dbb00649917773bb9ea620f21eff583af5902200b18c44386ddab3c947330b5307658ae436521a3b18349637dc0e65e067b5180012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402207fa27cb111687362e2a126ec1b2558d5cd19830bce9f43c91d4844b8e3cdfb1e022078ef8dcbad6bd435efd437cc20457dfa37d51879551d0bbe65588c332220a4560121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool01()}
   * Change values might differ slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_cascade_pool01() throws Exception {
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
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973146L);
    outputs.put(SENDER_CHANGE_84[0], 975766L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "af1783601244ce85392a4ca57dda06dbf1620cc717e673bfcfcd0ad95c10c280";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae35ad90e0000000000160014657b6afdeef6809fdabce7face295632fbd94feb96e30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f020446430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402200a7f7ccf273ea6394aa7e8679f9f8893b8a735d7ac84184914dea9ee2783b8250220603738a85058c7040fca06fd190fc37c3af1ea53ba46e897224c396dd9df34e8012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100b1e856b903538d31002270efd15bb1ce2279f2fde94a128331b0060c1423aaad022012374c9732f23eaa245f293e1eb56d06da98de3b83a7ea59e777d428a6537aa40121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 69142L);
    outputs.put(SENDER_CHANGE_84[1], 69142L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex001 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex001 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "cfe1698dc2271a7743a23fa4c4b5a9ec0707654ebf43f4c25b902982964d00f7";
    raw =
        "02000000000102a0ccfefdf86b76701191aecbb7f7d1ab4fe31567530fe0d9e9c71415602f08dc1f00000000fdffffff11d94402578c32ef36b5806707b2411b668c1ad3d10b58d9441307533fcf6bf21300000000fdffffff160000000000000000536a4c5052570b4993a266b406c2278ad264ad6a092f0b4576d60c0c6a703ad7c63b6021f17f5c0cefa488381dc4dddb8bc1036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3160e01000000000016001440852bf6ea044204b826a182d1b75528364fd0bd160e01000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229a687010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267fa68701000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442da6870100000000001600143ac59e5cdf902524b4d721b5a633a82526c53597a687010000000000160014524a759e76003300ccb475eb812e65817c6653c5a6870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56a68701000000000016001462e123682b149978f834a5fce14f4e71cdd133e2a6870100000000001600146ff0703b7b540c70625baa21448110f560bcb25ca6870100000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc5524a6870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfa687010000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e3a687010000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154ca687010000000000160014b6e1b3638c917904cc8de4b86b40c846149d3530a687010000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafea687010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb53a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700ba687010000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408aa687010000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae0247304402203f240a675518fd3943945e44ded004e00d62e8e8e4fd852cc83b7311a25d499b022025bb4d169598df425c271c7014ecd2c51661b7682f07aaf11b17456b2f3bc92d012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022020521d4c55c770a46f262d6e00ade5a331adc7d719a8c12101fb84c4e235e6e5022069a60e7dd3d2121a480c3ac58fa14f61659823f05773651670022a57e27908fe0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05()}
   * Change values might differ slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_cascade_pool05() throws Exception {
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
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924621L);
    outputs.put(SENDER_CHANGE_84[0], 4925145L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "1899b155d20e25484a2bddd8a2880b230e4182748ab859de41e79f7765f4c537";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3cd244b0000000000160014657b6afdeef6809fdabce7face295632fbd94febd9264b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204464c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a464c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d464c4c00000000001600149f657d702027d98db03966e8948cd474098031ef464c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100b34ec987561c3ecb78f7f50982a83322596d311a84a3ab6b33188a4b6686e33002204e0d9d20d81541f75e55ef39616e975e4d9f3d488504c812dc56fd3920f7d3e4012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022075d658f988030065bc057928d6205f142e0fefd90c2825e17562295df925cd08022062565135e1d6b13e86074f6f7ee5a3b38afb057c1d60efde7398ff871bff95080121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 923287L);
    outputs.put(SENDER_CHANGE_84[1], 881311L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "e716bb35fc42e8a9466a8c6af5c885fce7238f411d46eb21134ce9b0241eea26";
    raw =
        "020000000001029ad3a37da1e6a4f27cd866dbae89f4ab212aa653656207e55feff9960a5c8c590300000000fdffffffca40806cbadffa0a4c4aebe2e820c64b6f30f34257d5832de1bb3ba708545d5a0700000000fdffffff0c0000000000000000536a4c506b8568bc27ee9b3abb68232b9efaff5866db381443577247ce527ae1215c77e44739a88c4d510945e8415a134b7a036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae39f720d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c822997160e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300247304402201b4211eb0185fcea8b6ed5b45c0dbb03d682f46818458cce5329b34153d095ce022059cfc12b712fa371332d1c7109b2249a58d81039286dd69648ca0996b21f5cb60121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e0247304402202c72057c44dee42ff9ec1468674232485d74f2bcac9e84faa58c3532eee2c15502203e0890ee848da7186eff9e5d7167124b3a435cf8ea86ba653c6c5e094e7f2744012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47150L);
    outputs.put(SENDER_CHANGE_84[2], 47150L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "a75c3df997146fa724b849788c68261ac356187cc2e46c4cf6adec251c357687";
    raw =
        "02000000000102ea6f2efcea504bca70228bd4f6b11137540d0cf849328fd5448f0a6454094b070b00000000fdffffffe9dabbd9381335712c9fb75c58b5f6d152165cfc4c9903a08977e3d5396680e40400000000fdffffff150000000000000000536a4c5052739a04bfd29be57e18c8e24ce269e417839e41f879b81924c65379a01a596013bb1f107455b99b99786c3c46f5036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae32eb80000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac322eb8000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce8412a687010000000000160014017424f9c82844a174199281729d5901fdd4d4bca68701000000000016001429eeb74c01870e0311d2994378f865ec02b8c984a6870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029a6870100000000001600143f0411e7eec430370bc856e668a2f857bbab5f01a6870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffa687010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383baba687010000000000160014524a759e76003300ccb475eb812e65817c6653c5a6870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56a6870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f182a6870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1a6870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfa687010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fa687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2a687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402204135e97021eb2d3a3c021bbc81768bbd3fd17c88fb334b4132575d8ba804fb3a02206c889c8fdcb3797baed5cd721731933f17bb14cce29b77379309fa7e0d802e7c012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100ab44572fd7d02eb22377f093b04bf46ebc9b383d0833963a98aae13ba0ce06d30220342a8a7af6a935d900cd600f4401e2c7af43fb19b7403edbcaa98a29e6453ec00121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }


  // TODO: These 2 tests below need to be reevaluated.
  //  - Currently only mixes in lower pool if Sender's / Iniatior's change is large enough to mix

  /**
   * Sender's change is not large enough to mix in 0.01btc pool.
   * Counterparty's change is large enough to mix in 0.01btc pool.
   * 0.01btc pool skipped and continues to 0.001btc pool.
   *
   * When change split in bottom pool 0.001btc, Counterparty loses ~0.02 btc. TODO
   */
  @Test
  public void tx0x2_cascade_pool05_senderSkip01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05 & 0.001");

    int account = 0;
    Pool pool = pool05btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 6000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    Assertions.assertEquals(2, txs.size());

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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924621L);
    outputs.put(SENDER_CHANGE_84[0], 925145L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "3f373b773768a18581e0916ad55a140e20b290e460f79b01d44157de49431553";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d91d0e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204cd244b0000000000160014657b6afdeef6809fdabce7face295632fbd94feb464c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a464c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d464c4c00000000001600149f657d702027d98db03966e8948cd474098031ef464c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc024730440220168d82598b569049669296163400bd7a8f91435a8321610907dda359817e60e302205cc18ce9deb82dbeb9b499e42d0529c2c774cc71f1530d7f10a4cc0e2676a23e012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100ba4505a22cfcc26100a1a9798255958d53800ce241551b5907794858df966a6e022077f6056f635b76aea07f853cb40cec9b938fe5e68f33c9ac96d2d8946d2e51c60121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2)cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    nbPremixSender = 9;
    nbPremixCounterparty = 12;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    Assertions.assertEquals(
        tx001.getOutputs().get(2).getValue().getValue(),
        tx001.getOutputs().get(3).getValue().getValue()); // Change outputs equal

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 1869142L);
    outputs.put(SENDER_CHANGE_84[1], 1869142L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "5b33ffa9749451dfce7c4c868e002843e5657e136523da3e56921d4da9f3ffa6";
    raw =
        "020000000001029ad3a37da1e6a4f27cd866dbae89f4ab212aa653656207e55feff9960a5c8c590300000000fdffffff03e3ca4404802102c990551a9ab807967edf7044d1a485d21b1850666c7c44dd0700000000fdffffff190000000000000000536a4c5013d5a0fd4c7db80e8f38b6fea5299404d951e3770758e3a406d2ba58e3b5583cb3a469c13aa65c72d290ae80b6f0036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3a687010000000000160014017424f9c82844a174199281729d5901fdd4d4bca68701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739a687010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997a68701000000000016001429eeb74c01870e0311d2994378f865ec02b8c984a6870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029a6870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffa687010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383baba6870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fa6870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f182a68701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaa68701000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662ba6870100000000001600146be0c5c092328f099f9c44488807fa5894131396a6870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1a687010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311a687010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fa687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2a687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830a687010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde156851c000000000016001440852bf6ea044204b826a182d1b75528364fd0bd56851c000000000016001485963b79fea38b84ce818e5f29a5a115bd4c822902473044022048d94092a82c47e07b5f29afb8fc03a57f8a39308b6c928b88510f733518f97c0220157e0f93805e6fae619200976f16210976ab2f747c14612c0b3c9a98efd176020121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e02483045022100cfaa683f6fd2ce2fa4c979dc7b75e4383034c40346e745fbf2b7d4b7b48f474a02206e092a74accb52c36a026acdc5b0e22c144596434fab0650f21b39caf46c4d76012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  // TODO
  /**
   * Sender's change is large enough to mix in 0.01btc pool.
   * Counterparty's change is not large enough to mix in 0.01btc pool.
   * 0.01btc pool is done for sender; miner fees subtracted for counterparty.
   */
  @Test
  public void tx0x2_cascade_pool05_counterpartyNo01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05, 0.01, & 0.001");

    int account = 0;
    Pool pool = pool05btc;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 20000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 6000000);

    // mock Tx0Data for reproductible test
    mockTx0Datas();
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService(false);
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    String xpub = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84).getPub();
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
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
    int nbPremixSender = 3;
    int nbPremixCounterparty = 1;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 925145L);
    outputs.put(SENDER_CHANGE_84[0], 4924621L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "bbdf3842fe613cdaa79c75088e1c00994c7bd0baeaaf73e57931c1592944f7c4";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d91d0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febcd244b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204464c4c000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739464c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d464c4c000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea464c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02473044022072a6e73a03ca11e2cfa2a54581fda2ba2cdc5e81c12a84d3efd79e1cccc9882b02201006807f5fc9ce482fce36e82cc51ae1c0040d94679984e3bd16e6a3251d0ac1012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100e01386a43529a7761f5b3d07fe9d004eceef5d4a9b6c2de2d36b2f53302cae8a02200bf73e128d60c5183320d7b8c48812e2b8dfdab032cee88f0d89a3fab4a425100121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2)cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    nbPremixSender = 4;
    nbPremixCounterparty = 0;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 924927L);
    outputs.put(SENDER_CHANGE_84[1], 880855L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "f9a67ed7b767266bf09e68e9ec134549f5e9833da030d8f70cf017fb1a7fcf81";
    raw =
        "02000000000102c5478b1e8f4b1879abee2c029a7b7e4490a08551a5ad30e22a352ab3388d3a280100000000fdffffff99e5e1728391e29d9889c04fb021ddea67d04b547e239046b88a7480fb498fc40700000000fdffffff080000000000000000536a4c50774a3d60a0a5a33a1b9c37b26f3d06ab81f85d0bb4d4732a03c162e5296bbf0ffbba0d2ee33b08c76cd3d700f0ce036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d7700d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229ff1c0e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd46430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f02483045022100a41b688a4bfe0a60c68b36ffb375b113f19fc908cf3fdc538f62947dc3e025100220613548924a8ae2413090a81c1e6f37a1d99a8c092f948246c9330eb08220adfe0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e024730440220288befdbd1160d53aa8042e77fcc41b335237579e9dff5d374ba7cebdaeb57250220368d5879311b8b03e2ebdc7c4f81bcebda6a08aa8b167308b46cac32bbf2cb2d012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47742L);
    outputs.put(SENDER_CHANGE_84[2], 47742L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100262L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100262L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "09e6328be75519cb66a308820e16247e161885ca181945384f281400f9632b75";
    raw =
        "02000000000102f6abf85d48a56a852b517b0419f8a39f39aa9d218ff3387a3a05fe477d3c716c0700000000fdfffffff025a2663283be2cd7d69e38ad4616c4182ca9c64b313cf1c07b6203625475e60000000000fdffffff150000000000000000536a4c50b384495c0eef2f4229038565c29cf5dffa004890dcdda3f453beb2d025ebec96ffd9018d888e6920c894f728fa1e036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae37eba0000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac327eba000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce8412a687010000000000160014017424f9c82844a174199281729d5901fdd4d4bca6870100000000001600140343e55f94af500cc2c47118385045ec3d00c55aa687010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267fa687010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997a6870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029a687010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383baba6870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fa687010000000000160014524a759e76003300ccb475eb812e65817c6653c5a6870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56a6870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfa6870100000000001600149f657d702027d98db03966e8948cd474098031efa687010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311a687010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fa687010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb53a687010000000000160014d43293f095321ffd512b9705cc22fbb292b1c867a687010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356a687010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002473044022055a989d264c3d3ced4bd357ea7fcbe59c8ba480270e144bc3196174cf48ac37402203a927810d10e99878761811901583802d2256e0a4a01408e472352f1d4951e7f012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100b3c8ea9cbc6039b4fb99e82305d6b9afaffafca86cbad82ac33a470328ea5546022000d055913917274faf787cf30de4ecc5d54a66779e040e00aa77b3e50b1ccd400121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  protected Collection<Pool> findPoolsLowerOrEqual(String maxPoolId, PoolSupplier poolSupplier) {
    Pool highestPool = poolSupplier.findPoolById(maxPoolId);
    return poolSupplier.getPools().stream()
        .filter(pool -> pool.getDenomination() <= highestPool.getDenomination())
        .collect(Collectors.toList());
  }
}
