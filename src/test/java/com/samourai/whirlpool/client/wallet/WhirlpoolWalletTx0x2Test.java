package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.constants.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.whirlpool.client.test.AbstractCahootsTest;
import com.samourai.whirlpool.client.tx0.*;
import com.samourai.whirlpool.client.tx0x2.Tx0x2Context;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.feeOpReturn.FeeOpReturnImpl;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletTx0x2Test extends AbstractCahootsTest {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0x2Test.class);
  private Tx0Info tx0Info;

  public WhirlpoolWalletTx0x2Test() throws Exception {
    super();
  }

  @BeforeEach
  @Override
  public void setup() throws Exception {
    super.setup();
    this.tx0Info = whirlpoolWallet.getWhirlpoolInfo().fetchTx0Info(null);
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
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService;
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
    Tx0Config tx0Config = tx0Info.getTx0Config(Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);

    Tx0 tx0Initiator =
        tx0Info.tx0(
            whirlpoolWallet.getWalletSupplier(),
            whirlpoolWallet.getUtxoSupplier(),
            spendFroms,
            tx0Config,
            pool);

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
    Tx0PreviewService tx0PreviewService = mockTx0PreviewService;
    FeeOpReturnImpl feeOpReturnImpl = computeWhirlpoolWalletConfig().getFeeOpReturnImpl();
    feeOpReturnImpl.setTestMode(true);
    Tx0Service tx0Service = new Tx0Service(params, tx0PreviewService, feeOpReturnImpl);

    // initiator: build initial TX0
    Collection<UnspentOutput> spendFroms = utxoSender1.toUnspentOutputs();
    Tx0Config tx0Config = tx0Info.getTx0Config(Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0 tx0Initiator =
        tx0Info.tx0(
            whirlpoolWallet.getWalletSupplier(),
            whirlpoolWallet.getUtxoSupplier(),
            spendFroms,
            tx0Config,
            pool);

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
}
