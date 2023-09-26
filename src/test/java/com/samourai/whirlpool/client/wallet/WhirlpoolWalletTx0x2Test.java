package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2;
import com.samourai.wallet.cahoots.tx0x2.MultiTx0x2Context;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Context;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.test.AbstractCahootsTest;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
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
  public void setup() throws Exception {
    super.setup();
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy()} */
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

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0 tx0Initiator = tx0Service.tx0(walletSupplierSender, pool, tx0Config, utxoProviderSender);

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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973304L);
    outputs.put(SENDER_CHANGE_84[0], 975854L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "6b48ec2da968618c4f0d716d68872f817ceca52b3c3867dc2ec05839c11d1fa5";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f8d90e0000000000160014657b6afdeef6809fdabce7face295632fbd94febeee30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102483045022100fd904412cf246a5c5ee01b253d73494b4f50cafcf9b17beb53f859068125d5850220622d023c4e91977bb6ba737e9322d91bb44d6b0515814bec7142b8b382c7388a012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402202eda1e3d81a3f4884a916bbdc45e5d63878ba6b6649c123351e09d30a91c9bfb02207148a41b36c65e1d0881b77fe06790b15e580597ee220a0a640287781f7562170121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0 tx0Initiator = tx0Service.tx0(walletSupplierSender, pool, tx0Config, utxoProviderSender);

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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 14968573L);
    outputs.put(SENDER_CHANGE_84[0], 4968573L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "5789c23db36915aa5b5146237a12f2b93c7965c56018c7586c04764d1e8e1460";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff4a0000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae33f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f0000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f3f430f00000000001600141439df62d219314f4629ecedcbe23e24586d3cd33f430f000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141bcc24b74b6d68a6d07a34b14e6d4fd72e998a623f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f000000000016001423631d8f88b4a47609b6c151d7bd65f27609d6d03f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600142525a95f3378924bc5cec937c6a7a1b489c5ff863f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f0000000000160014378ac72b08d43acd2d9e70c6791e5f186ec395dc3f430f00000000001600143ac59e5cdf902524b4d721b5a633a82526c535973f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f000000000016001440d04347d5f2696e4600a383b154a619162f54283f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f000000000016001441a73bec4bd8c083c62746fcf8617d060b3c391a3f430f00000000001600144288958e3bb02ba9c6d6187fe169279c71caa4e63f430f00000000001600144518c234185a62d62245d0adff79228e554c62de3f430f000000000016001445cc6ccf7b32b6ba6e5f29f8f8c9a5fe2b5595293f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f0000000000160014482e4619fb70e25918bdb570b67d551d3d4aab9f3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600144ecd8a26f6fc2ae301bbc52358d95ff50137ee6b3f430f0000000000160014524a759e76003300ccb475eb812e65817c6653c53f430f00000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d563f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001462e123682b149978f834a5fce14f4e71cdd133e23f430f0000000000160014635a4bb83ea24dc7485d53f9cd606415cdd99b783f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600146ff0703b7b540c70625baa21448110f560bcb25c3f430f00000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc55243f430f000000000016001476b64af1eb81d03ee7e9e0a6116a54830e7295733f430f00000000001600147dfc158a08a2ee738ea610796c35e68f202cf06c3f430f0000000000160014851204bc2e59ace9cfbe86bbc9e96898721c060d3f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf3f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e33f430f0000000000160014ac64d97c6ee84eff2ce8373dfe5186f6dda8e3ac3f430f0000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c3f430f0000000000160014b3332b095d7ddf74a6fd94f3f9e7412390d3bed93f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b696b85812d9b961967ba20fa8790d08f8b9340b3f430f0000000000160014b6e1b3638c917904cc8de4b86b40c846149d35303f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c1c95595d7b48b73f5b51414f807c5bd9f2379853f430f0000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe3f430f0000000000160014c88fb64ea3063496876c224711e8b93c18d4bb533f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014cdf3140b7268772bd46ffc2d59fa399d63ecb8ba3f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014e7056147da987fc9ca73003d5b807ec145e1b4ce3f430f0000000000160014e736d0bbc2bcfbec2c577223c1f75d096440fd013f430f0000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3563f430f0000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014f0e99871ae8ce7b56a9e91a5bea7d5e4bffcb8cc3f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde13f430f0000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a3f430f0000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae7dd04b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204fd66e40000000000160014657b6afdeef6809fdabce7face295632fbd94feb0247304402207397b82156ccdd79c3140b580f9ab70c8c6da2122d97757694ed2e90fbff3eb2022016b8b329e1f95ae0b525b95c5005cf2f375c32c9a8d9189a0491f41fd88b322a012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402207549bbdc470da93f36cc31b45351ba920133ac5540bfaec20e92cb789a3cf19202204b8adba282fd9b201350399c03fb2c525d6155e19836324ce27b204128e6f3870121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx, txid, raw, outputs);
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test} */
  @Test
  public void tx0x2_pool001() throws Exception {
    log.info("Testing Tx0x2 for pool 0.001");

    int account = 0;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 500000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 1000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.001btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(1, txs.size());

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2) cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    long nbPremixSender = 4;
    long nbPremixCounterparty = 9;
    long expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 95474L);
    outputs.put(SENDER_CHANGE_84[0], 95474L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    String txid = "be52c2d4aae669d2722d4427b77128cf768c029d20334cfa3e79ee31849729d9";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff110000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f2740100000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204f274010000000000160014657b6afdeef6809fdabce7face295632fbd94feb9f870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a9f8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57399f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f87010000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d9f8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600149f657d702027d98db03966e8948cd474098031ef9f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc9f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002483045022100cab33f87320ec7b7d46f700d8e3f7bd27d98199f91ee42ebfb3a801172d1619e0220053a75b7fee3c1e2828007c2e8b4ef220fd809d7a9b24d33348434ae58a44faf012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402207e0cf446a0e0e5bf980e2928355699c48e440b47a4e447b5fd86529355abefb902201ed2983fb6c524a5d2a62b5035a4198487687d9a1d4dd7e53a31ffad4edb05a90121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setDecoyTx0x2(false);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.01btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);
    Assertions.assertEquals(2, tx0Initiators.size());

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(2, txs.size());

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2) cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973304L);
    outputs.put(SENDER_CHANGE_84[0], 975854L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "6b48ec2da968618c4f0d716d68872f817ceca52b3c3867dc2ec05839c11d1fa5";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f8d90e0000000000160014657b6afdeef6809fdabce7face295632fbd94febeee30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102483045022100fd904412cf246a5c5ee01b253d73494b4f50cafcf9b17beb53f859068125d5850220622d023c4e91977bb6ba737e9322d91bb44d6b0515814bec7142b8b382c7388a012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402202eda1e3d81a3f4884a916bbdc45e5d63878ba6b6649c123351e09d30a91c9bfb02207148a41b36c65e1d0881b77fe06790b15e580597ee220a0a640287781f7562170121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2) cahoots).getTransaction("0.001btc");
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 69338L);
    outputs.put(SENDER_CHANGE_84[1], 69338L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "14d324b96df2738512b8e0b969a4a2dd548dfad96d879fb454d9f5228aca9243";
    raw =
        "02000000000102e70454a83b4a771efbc60be6e8492f3a64346d25f805bf987bd873c6db8049421f00000000fdffffff430c662ecb70eef8641a98bd6ac21ab3e4dfae860b6f0ac2c7d35a046ed6bdda1300000000fdffffff160000000000000000536a4c508b1be9fe56593d72b875e9e76c39b5b08e3deb68e7fb378396e0a3ba0f312a476059ad16a608983517fe25dd7bd7036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3da0e01000000000016001440852bf6ea044204b826a182d1b75528364fd0bdda0e01000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f8701000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d9f870100000000001600143ac59e5cdf902524b4d721b5a633a82526c535979f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f8701000000000016001462e123682b149978f834a5fce14f4e71cdd133e29f870100000000001600146ff0703b7b540c70625baa21448110f560bcb25c9f870100000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc55249f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e39f87010000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c9f87010000000000160014b6e1b3638c917904cc8de4b86b40c846149d35309f87010000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b9f87010000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a9f87010000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae02483045022100a6fe8050b570328d281cc726afcae04c77213c8f8c1b1e2b4a0dd853c95016d502204c232eb6758a24ddd13bf0ab6e2eddca37780fe150e12a941a43954adef8eff9012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022028614d71d6098967644a27b0775fa769133b8aed23021b76b317f31b5be4b8f902207c58e222e24b0112e09edc1f5541a5e49b4bf375b0f854619877d40b4d089b130121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setDecoyTx0x2(false);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);
    Assertions.assertEquals(3, tx0Initiators.size());

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(3, txs.size());

    // 0.05btc pool
    Transaction tx05 = ((MultiTx0x2) cahoots).getTransaction("0.05btc");
    Assertions.assertEquals(2, tx05.getInputs().size());
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924631L);
    outputs.put(SENDER_CHANGE_84[0], 4925141L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "e5a03887b59e1ff9ef006c65b14d7cd1c85ca35b8b3f72c68e3e547a883e2396";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d7244b0000000000160014657b6afdeef6809fdabce7face295632fbd94febd5264b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100e948f8dabdc9cbf11e3ae3f5c36a6e6008758d8016c9f1ee3a327dd8fdd2049602204b9f3ec0cb295e31c99fd8d95ef53dd6d9effc07540091f7edb8106fbac17790012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402206b75530bb19dffb37c2dc58e6e38795f0a301ccdb6e522a46ef9a0eaff36dcb4022055ea9c946090e3c752b6e2b6f685a457cbdd36e43b0d490a31b2f4cf84e3ead10121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2) cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    nbPremixSender = 4;
    nbPremixCounterparty = 4;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 923320L);
    outputs.put(SENDER_CHANGE_84[1], 881330L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "26530a10b0c7c1b4448c32d60ad65c0917d862f1acbc41a42acb42de2e192596";
    raw =
        "02000000000102616b5bf2856ac0a62550b718327ecb85948f1849ddfae4dde5f38837c2843d660300000000fdffffff0e735d4b2dfb66d468394d3b440382c37b6b8693757d58cf6a546b6a64629fcd0700000000fdffffff0c0000000000000000536a4c502dfca5f2388dca1860db36fc36954e95bb989a2052fb7c472bae7fa57bd5c73799f604bcd65b13c8c6fb50ecd9ef036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3b2720d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229b8160e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002483045022100958267772c8a8bd05168a90702686a83d771a18e97bf419e5c061c145f05c6c302202135459d5319ad8127f367d758a1eac6e61baad492458d2245fc972ac35cc9df0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e0247304402200db38ec0579281ff139f767f302ba1ba3c7579b40f4254572a7d227103460aaa02203c6d09811d7c78bba46f1e57dea28ff4e5c8bc649b45fdcb54e1d8929281b36a012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2) cahoots).getTransaction("0.001btc");
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47242L);
    outputs.put(SENDER_CHANGE_84[2], 47242L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "0225005ac3a3785c6f93040060c1549185c5b9d0e13f73bb5eb25f4e854bf2a4";
    raw =
        "02000000000102f85219925f776dbc2eb2fd36a4483b8da67d6b61600df9e64324a7f117f9c0710b00000000fdffffff837576647bbd9c7eaffae888faff104b5e300a796ac5bbd45537f1f8651909ce0400000000fdffffff150000000000000000536a4c50e117229c8611834915f45f73f42288f1bea7cf5e0f94800f7c35216d08531d6362cc855db4fcb7087d9976785769036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae38ab80000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac328ab8000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600143f0411e7eec430370bc856e668a2f857bbab5f019f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde1024730440220601f8c5d9cc2cb0330967dc5cb8828efe0fe5dc82bc6ca20dbe0ca03a0de230a02203e7b07352ca7ebf9c687380c617d59145afedc992788cafb112e7a26f732c834012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022076f05cac4c62689ae6c194b9c23b109870c63e79ba90bb2571e54ddc5c9f39a602203c01ef80e7983c51bf005e71355009905a0685f3180cf570204b64622c7ccb720121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  /**
   * Sender's change is not large enough to mix in 0.01btc pool. Counterparty's change is large
   * enough to mix in 0.01btc pool. 0.01btc pool skipped and continues to 0.001btc pool.
   *
   * <p>Change is not splitted in bottom pool 0.001btc, to avoid Counterparty loss of ~0.02 btc.
   *
   * <p>Compare with deocy tx0x2 test: {@link
   * WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05_skip01()} Change values might differ
   * slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_cascade_pool05_senderSkip01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05 & 0.001");

    int account = 0;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 6000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 20000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setDecoyTx0x2(false);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);
    Assertions.assertEquals(2, tx0Initiators.size());

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(2, txs.size());

    // 0.05btc pool
    Transaction tx05 = ((MultiTx0x2) cahoots).getTransaction("0.05btc");
    Assertions.assertEquals(2, tx05.getInputs().size());
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924631L);
    outputs.put(SENDER_CHANGE_84[0], 925141L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "74f71ee1d8f46a98d37b37c82642f561cf7fa33314e66d75d31867e4eafaa5b5";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d51d0e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204d7244b0000000000160014657b6afdeef6809fdabce7face295632fbd94feb3f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100ead819bd6f90547413442ea06d2a812b8fc025aab90be19ace5b6af58c93739002206a13103a5025d2f4ae1c998d7da38ea05b561a6139f807d5d1180426df23993e012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5024730440220653bf0befaf52e264ebce2cd42790e2993cd125cab31b7c0474c7cf19a07b19802205a55bcf12af203d4648901eb5cd8f446eb93fd3032bb384050523ab8efd013d90121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2) cahoots).getTransaction("0.001btc");
    Assertions.assertEquals(2, tx001.getInputs().size());
    nbPremixSender = 9;
    nbPremixCounterparty = 12;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx001.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 3721094L);
    outputs.put(SENDER_CHANGE_84[1], 17369L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "329e9de432252a75f9b803d800dba7e20c4ac4d1d6c9d80a8bcf832b9acaae53";
    raw =
        "02000000000102601195e3d5856fedad8ef076b5b2b99dcb5f1f9a45bcc0ec5da92ef1edf5aa250700000000fdffffff616b5bf2856ac0a62550b718327ecb85948f1849ddfae4dde5f38837c2843d660300000000fdffffff190000000000000000536a4c50baf1b5878436c37a11039cb94a28fd7aed7c7d5f76593c82e7b36ece3f8b88a769b401b6403ff3e470ec15b91b52036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d94300000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57399f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea9f8701000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358309f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde186c738000000000016001440852bf6ea044204b826a182d1b75528364fd0bd02473044022035f2215e075016ed48fac65ada9201c9c25a9e4d59617ac8fea30894c9e3c9da02205e1c85e01c6d713a9d8bb4e5e7febe605c96a62cf3a3bda14a2f7606e86f0d4e012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50248304502210091ed22c591bd8ce64a23d8cf8cbae7dc402ff505714298853a89bec76999eb6302201629124768cd3e52a74e6c60a0a92da229a7490aca0f35bc86a9c63a787f4d2c0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  // TODO
  /**
   * Sender's change is large enough to mix in 0.01btc pool. Counterparty's change is not large
   * enough to mix in 0.01btc pool. 0.01btc pool is done for sender; miner fees subtracted for
   * counterparty.
   *
   * <p>Compare with decoy tx0x2 test: {@link
   * WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05_skip01()} Change values might differ
   * slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_cascade_pool05_counterpartyNo01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05, 0.01, & 0.001");

    int account = 0;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 20000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 6000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setDecoyTx0x2(false);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);
    Assertions.assertEquals(3, tx0Initiators.size());

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(3, txs.size());

    // 0.05btc pool
    Transaction tx05 = ((MultiTx0x2) cahoots).getTransaction("0.05btc");
    Assertions.assertEquals(2, tx05.getInputs().size());
    int nbPremixSender = 3;
    int nbPremixCounterparty = 1;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 925141L);
    outputs.put(SENDER_CHANGE_84[0], 4924631L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "e025523da888ecab7d72e1ebb440947186f1b5160baf85d8cc17d9dfc297b8d8";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d51d0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febd7244b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc0247304402202377ff1692fdfed8924fe5e5a3b01b8013c9ed72eb539b00c6d1522742c41178022008e7635cfbb5c30f24d455de46a4e2521f848c1e032d4507376892b0d7bff3e8012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100c98f01199263fd807d83887a9894feaf1353c211f060ccbf7ecb1fe02a423cb802205a6117451e2da07fbb628fce9a26828055681d64d97eb39ea5c1efd3643512fd0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2) cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    nbPremixSender = 4;
    nbPremixCounterparty = 0;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 924912L);
    outputs.put(SENDER_CHANGE_84[1], 880882L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "39c08c6db5ccb7c88adf31272693c292b8d187acbc310c9c54229c7c7890d2f6";
    raw =
        "02000000000102f4a530ce5e3e8371a49b1181802c110ab52963ecc9aa837e7e63cba0019d5c3a0100000000fdffffff88ce02988b1f7b815875f2b4d03cad728f5887b680eb66cf82971fa3a2ff69a60700000000fdffffff080000000000000000536a4c50655e92aa2f9480938e37353b98d65c2bb603ec85b5109fddb7cc3ed224f85a34a861b8d539dbaf8b49daf3f5ec5e036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f2700d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229f01c0e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd3f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f0247304402201e82147549d86e6d522a2391ddc31305481087e00426bab482f452959efd09ac02204b21bf896a3d2ea7f81834695f91593336a063f29d29951bda68a2048767ef9e0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e0247304402201813b3113a6f3e1ee940abbec57936a9457adc934297ae386f7283af22b3f40f02204bfc032fc263536234786e36dfba72e7a7a1f59eb67e5947308825def6504de0012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool
    Transaction tx001 = ((MultiTx0x2) cahoots).getTransaction("0.001btc");
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47814L);
    outputs.put(SENDER_CHANGE_84[2], 47814L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "f645ec284fa9c9bfd40659408b0d688bee39c7f429fbde2773adcbe2921383ac";
    raw =
        "0200000000010218e5ae96d801e57222269bba9e99f71a2bb080558da7a2ce80b00c388899105e0700000000fdffffff38bed4ee6a473795b92a2e124252b3aeaab547efa3ae9dba6181d9fb722231e00000000000fdffffff150000000000000000536a4c50b90a78e20a8774dcb5fa54ad73cdbfb42781f679ef6ed953877b69fed55e82f7a77207f8329b4d223a1af7845e42036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3c6ba0000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac32c6ba000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a9f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f870100000000001600149f657d702027d98db03966e8948cd474098031ef9f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300247304402207ff3b8d60cd21d94b293cd06888b493cbab132cb9820bbf2bf9050db6d5cc01f022049d32a4f9b01556bda7777744f28a9afa69dbe0311ceed6d19239eb8ae8d2c73012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100e18f56e1c79c0e33371dc2040d3af9f0c0505c15522c9260b5fb2686b519f28a022032d1e7ab3763041851290c8008f4c740c19014c323fb0da23dea8e55f39a642c0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx001, txid, raw, outputs);
  }

  /**
   * Doesn't reach 0.001 pool but change outputs are splitted anyway.
   *
   * <p>Compare with decoy tx0x2 test {@link
   * WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05_no001()} Change values differ slightly
   */
  @Test
  public void tx0x2_cascade_pool05_no001() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05 & 0.01. Doesn't reach pool 0.001.");

    int account = 0;

    // setup wallets
    BipWallet bipWalletSender = walletSupplierSender.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    BipWallet bipWalletCounterparty =
        walletSupplierCounterparty.getWallet(BIP_WALLET.DEPOSIT_BIP84);
    UTXO utxoSender1 = utxoProviderSender.addUtxo(bipWalletSender, 9200000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(bipWalletCounterparty, 19130000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setDecoyTx0x2(false);

    Collection<Pool> pools = findPoolsLowerOrEqual("0.05btc", whirlpoolWallet.getPoolSupplier());
    List<Tx0> tx0Initiators =
        tx0Service.tx0Cascade(walletSupplierSender, pools, tx0Config, utxoProviderSender);
    Assertions.assertEquals(2, tx0Initiators.size());

    // run Cahoots
    MultiTx0x2Context cahootsContextSender =
        MultiTx0x2Context.newInitiator(
            cahootsWalletSender, account, FEE_PER_B, tx0Service, tx0Initiators);
    MultiTx0x2Context cahootsContextCp =
        MultiTx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Cahoots cahoots = doCahoots(multiTx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TXs
    List<Transaction> txs = ((MultiTx0x2) cahoots).getTransactions();
    Assertions.assertEquals(2, txs.size());

    // 0.05btc pool
    Transaction tx05 = ((MultiTx0x2) cahoots).getTransaction("0.05btc");
    Assertions.assertEquals(2, tx05.getInputs().size());
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;
    int senderIndex005 = nbPremixSender;
    int counterpartyIndex005 = nbPremixCounterparty;
    int expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx05.getOutputs().size());

    Map<String, Long> outputs = new LinkedHashMap<>();
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4054631L);
    outputs.put(SENDER_CHANGE_84[0], 4125141L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "7b47652bea97dd58b2aaf1e89308c86f7488157fdd4c2ba883a132dbbd9dd38c";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae367de3d0000000000160014657b6afdeef6809fdabce7face295632fbd94febd5f13e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc0247304402202c5f590886aee99cda7ffc6ee2b99189f3dfc4dd1506a8b89838f950bf1717bf02200c0a72050f16005ce851cedfcdaa33a1c08f91f7099921b46e681a4a3e409917012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022013beef41eb99d6a6d768d852461f82da0c9e1b829fdbeeda2e56d3727d679c87022038883ef591ab14ac63dc1c6e4b6302f9c9892cfaee2f1219d9c7fb2213f9adf30121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
    verifyTx(tx05, txid, raw, outputs);

    // 0.01btc pool
    Transaction tx01 = ((MultiTx0x2) cahoots).getTransaction("0.01btc");
    Assertions.assertEquals(2, tx01.getInputs().size());
    nbPremixSender = 4;
    nbPremixCounterparty = 4;
    int senderIndex001 = nbPremixSender;
    int counterpartyIndex001 = nbPremixCounterparty;
    expectedOutputs =
        nbPremixSender + nbPremixCounterparty + 2 + 1 + 1; // 2 changes + samouraiFee + opReturn
    Assertions.assertEquals(expectedOutputs, tx01.getOutputs().size());

    outputs.clear();
    outputs.put(COUNTERPARTY_CHANGE_84[1], 67325L);
    outputs.put(SENDER_CHANGE_84[1], 67325L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "1873a564fe5025e61e3c0e0cda35a169ae8623f739d288d6b4d02ff84bd33ed1";
    raw =
        "02000000000102b9873d21f4c5e693fa156b006be1d215ae2f894cbdad6e605cc61a0fda1438180300000000fdffffff05e21b43201c141a120651cc0698b52989c662629fe03c1ea8bd96bbc0db54e80700000000fdffffff0c0000000000000000536a4c50e1b48aacc5050cdcc26a33b8de3dc88774fded5922be10198a98d232e85bbb15cf00ca34c871f8c612fd6edaf43e036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3fd0601000000000016001440852bf6ea044204b826a182d1b75528364fd0bdfd0601000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82293f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300247304402205799ceb99c865289eae6d4930e249445b0c89b797fc5ce335db086cfeca934d00220172863f539c95a8b7afd5ef3a74f2daa5c6973e774b5e0e668ac6dd1035a83670121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e0247304402207f806b378b4f01c8609109fd8a1302f910429a7165b5a894123fff07244d998602205612ab99c82bfafe1b6a359a86fa28e320af041f05119a7efa04d3c397afb0b2012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
    verifyTx(tx01, txid, raw, outputs);

    // 0.001btc pool not reached
  }

  protected Collection<Pool> findPoolsLowerOrEqual(String maxPoolId, PoolSupplier poolSupplier) {
    Pool highestPool = poolSupplier.findPoolById(maxPoolId);
    return poolSupplier.getPools().stream()
        .filter(pool -> pool.getDenomination() <= highestPool.getDenomination())
        .collect(Collectors.toList());
  }
}
