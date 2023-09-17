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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973320L);
    outputs.put(SENDER_CHANGE_84[0], 975870L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "d683546e059f9d74e4fdd43bbfbbc301030cc39c11a4735dfe5914416ac2b890";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae308da0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febfee30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102473044022050b3da82f01a3f67d97cf847bd205c0588d9844e2b6e0778a69b82a741de4fe002207cd8b298990060d961ccaf8b7a75038f0c3460eda166af1cf9a40641dce1b5df012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50248304502210097c46cdd69999a7aff30c5cdb25111f03d0b93a5107918a118cf3be89127f16a02205d5d3741f7a6c8db2ecca9254ff02a3cd3381ebb817c39b6d9fd70088382f21b0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 14968589L);
    outputs.put(SENDER_CHANGE_84[0], 4968589L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "801fe9899191814af3cffbc0ed6222550dfee512fc953618aa5c8c80b555f138";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff4a0000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae33f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f0000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f3f430f00000000001600141439df62d219314f4629ecedcbe23e24586d3cd33f430f000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141bcc24b74b6d68a6d07a34b14e6d4fd72e998a623f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f000000000016001423631d8f88b4a47609b6c151d7bd65f27609d6d03f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600142525a95f3378924bc5cec937c6a7a1b489c5ff863f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f0000000000160014378ac72b08d43acd2d9e70c6791e5f186ec395dc3f430f00000000001600143ac59e5cdf902524b4d721b5a633a82526c535973f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f000000000016001440d04347d5f2696e4600a383b154a619162f54283f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f000000000016001441a73bec4bd8c083c62746fcf8617d060b3c391a3f430f00000000001600144288958e3bb02ba9c6d6187fe169279c71caa4e63f430f00000000001600144518c234185a62d62245d0adff79228e554c62de3f430f000000000016001445cc6ccf7b32b6ba6e5f29f8f8c9a5fe2b5595293f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f0000000000160014482e4619fb70e25918bdb570b67d551d3d4aab9f3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600144ecd8a26f6fc2ae301bbc52358d95ff50137ee6b3f430f0000000000160014524a759e76003300ccb475eb812e65817c6653c53f430f00000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d563f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001462e123682b149978f834a5fce14f4e71cdd133e23f430f0000000000160014635a4bb83ea24dc7485d53f9cd606415cdd99b783f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600146ff0703b7b540c70625baa21448110f560bcb25c3f430f00000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc55243f430f000000000016001476b64af1eb81d03ee7e9e0a6116a54830e7295733f430f00000000001600147dfc158a08a2ee738ea610796c35e68f202cf06c3f430f0000000000160014851204bc2e59ace9cfbe86bbc9e96898721c060d3f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf3f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e33f430f0000000000160014ac64d97c6ee84eff2ce8373dfe5186f6dda8e3ac3f430f0000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c3f430f0000000000160014b3332b095d7ddf74a6fd94f3f9e7412390d3bed93f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b696b85812d9b961967ba20fa8790d08f8b9340b3f430f0000000000160014b6e1b3638c917904cc8de4b86b40c846149d35303f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c1c95595d7b48b73f5b51414f807c5bd9f2379853f430f0000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe3f430f0000000000160014c88fb64ea3063496876c224711e8b93c18d4bb533f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014cdf3140b7268772bd46ffc2d59fa399d63ecb8ba3f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014e7056147da987fc9ca73003d5b807ec145e1b4ce3f430f0000000000160014e736d0bbc2bcfbec2c577223c1f75d096440fd013f430f0000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3563f430f0000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014f0e99871ae8ce7b56a9e91a5bea7d5e4bffcb8cc3f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde13f430f0000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a3f430f0000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae8dd04b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02040d67e40000000000160014657b6afdeef6809fdabce7face295632fbd94feb02483045022100dc673b8f2c1d170fb9e50263d0f5ec4ce71d88acd8190419fe29f0ffbde831fa02207f6244b9c93f97df9551cd31734431a39ce065cc4ed924e97d6f011052668852012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100f475c72fba97f07eeeac1eb2f1154f9c50b9edbca11132fedbd144b9ba4db06d02207f8467f52754dc531f140a7f94592b414923c002748abf2d9266179c9b19b8bf0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 95489L);
    outputs.put(SENDER_CHANGE_84[0], 95489L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    String txid = "fb25ff61da3ebb411c8f86fd031a7e87c037cdb6da67837755cfcee6f8e0f675";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff110000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae301750100000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02040175010000000000160014657b6afdeef6809fdabce7face295632fbd94feb9f870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a9f8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57399f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f87010000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d9f8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600149f657d702027d98db03966e8948cd474098031ef9f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc9f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002473044022036bfb808de8f441f80243772f48e3ade97f104c1421aef22d82abd82eb8fa64a02204e6adf40efcbd0f6d24f390605ff32f23c3fb8e96d8c7ed2e8e439dcb3513548012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100e811ca60b9c1c3f04f73917c60abc7f0b59b2ad205ab92201a750b0f938a012f02202bb666fc09bbf1660d94808b9cad6d4d55d7b9a050f49696cd6b4c9a8307d25a0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973320L);
    outputs.put(SENDER_CHANGE_84[0], 975870L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    String txid = "d683546e059f9d74e4fdd43bbfbbc301030cc39c11a4735dfe5914416ac2b890";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae308da0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febfee30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc3f430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda303f430f00000000001600141f66d537194f95931b09380b7b6db51d64aa94353f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d890293f430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f013f430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff3f430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab3f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f1823f430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd13f430f00000000001600149f657d702027d98db03966e8948cd474098031ef3f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f3f430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f3f430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be23f430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a13f430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c8673f430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc3f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358303f430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102473044022050b3da82f01a3f67d97cf847bd205c0588d9844e2b6e0778a69b82a741de4fe002207cd8b298990060d961ccaf8b7a75038f0c3460eda166af1cf9a40641dce1b5df012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50248304502210097c46cdd69999a7aff30c5cdb25111f03d0b93a5107918a118cf3be89127f16a02205d5d3741f7a6c8db2ecca9254ff02a3cd3381ebb817c39b6d9fd70088382f21b0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 69370L);
    outputs.put(SENDER_CHANGE_84[1], 69370L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "4a61dec39bb8dd955cfdd13a221cb2cab9192a52aebdcf4cdda8740558ca1d9d";
    raw =
        "0200000000010260ba6de06d75786c4c4e86143a1de5f7be0f9619ba6c8b2a5e0c7ec7eb0f6b0d1f00000000fdffffff430c662ecb70eef8641a98bd6ac21ab3e4dfae860b6f0ac2c7d35a046ed6bdda1300000000fdffffff160000000000000000536a4c508b1be9fe56593d72b875e9e76c39b5b08e3deb68e7fb378396e0a3ba0f312a476059ad16a608983517fe25dd7bd7036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3fa0e01000000000016001440852bf6ea044204b826a182d1b75528364fd0bdfa0e01000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f8701000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d9f870100000000001600143ac59e5cdf902524b4d721b5a633a82526c535979f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f8701000000000016001462e123682b149978f834a5fce14f4e71cdd133e29f870100000000001600146ff0703b7b540c70625baa21448110f560bcb25c9f870100000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc55249f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e39f87010000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c9f87010000000000160014b6e1b3638c917904cc8de4b86b40c846149d35309f87010000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b9f87010000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a9f87010000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae02483045022100aa525e1107833fb8f433296b4788c219b723980d8f3ab3ea68711422afd5943e022055adb7b7a4fab2df645c1be4ddaebd509c1c12f92f5568b64b097a1590c6b37f012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5024730440220430452f18797a9702e6823893aedda97c258c0290cb7158ff89a491b62d13ad5022061240c8bf1995a54534a1c664a140388e2aa78d69aeb29983c19a7487f6161bb0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924647L);
    outputs.put(SENDER_CHANGE_84[0], 4925157L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "34fe5deb907b5979a86cf89a374326d19fc9ca140722a6cb782de2372bc723af";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3e7244b0000000000160014657b6afdeef6809fdabce7face295632fbd94febe5264b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02473044022019b76606eb6a427e75d8a4818ce3c1e39b8bd32f74af957cb5d45a0a962ed0ae02202640c3d67a23a8b60a648843bc6b5cf55b220011ccf09c27c6519986fb8b10ad012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5024730440220136475d26b74940fb07d973edb1856630106db194088ce9de8604ebf1cf80edd02206dc1cb19e744775c0ef31a4531b69b0ccc7d3177cea64e4afbac5f86d5d7e7480121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 923352L);
    outputs.put(SENDER_CHANGE_84[1], 881362L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "9d3671f8945bc94cd08297aec2f505822151943bac27ee855b7ae4c89653e9f1";
    raw =
        "02000000000102616b5bf2856ac0a62550b718327ecb85948f1849ddfae4dde5f38837c2843d660300000000fdffffff1e3699e2f0a7bf96e535c99b0d401be6dee5aa68ff3a0b8339a71c74fde867f40700000000fdffffff0c0000000000000000536a4c502dfca5f2388dca1860db36fc36954e95bb989a2052fb7c472bae7fa57bd5c73799f604bcd65b13c8c6fb50ecd9ef036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3d2720d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229d8160e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd3f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300247304402205e78eefb61c88ad25cdd6acdf54472e502c1c5925440ce2f26e515c2fbb00c9202204796acb5fb71d4f9f07cb6f0a41d133fe64e2f14d71dbd9e4538b2190bf5c95a0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e0247304402204ff4a898a354bc77ed596639fba2c4d2a92d1fa74363633d68898b6ac856e3d502200d39a5b45c36d1fc93770fe53b8724001f5fb7d43866c551b26423e99381c7e9012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47290L);
    outputs.put(SENDER_CHANGE_84[2], 47290L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "1f9ba48a0f87fab2e3c0d215f67c5b69bb13cc48af613ef361e56f0798ce95bb";
    raw =
        "0200000000010239b6de580944496da9357afb58660a5ae05669d3dea1bf4b7b4caa83c0e0d84a0b00000000fdffffff837576647bbd9c7eaffae888faff104b5e300a796ac5bbd45537f1f8651909ce0400000000fdffffff150000000000000000536a4c50e117229c8611834915f45f73f42288f1bea7cf5e0f94800f7c35216d08531d6362cc855db4fcb7087d9976785769036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3bab80000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac32bab8000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600143f0411e7eec430370bc856e668a2f857bbab5f019f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102483045022100cbb7cf11ddefbf3303d6969dc7dade2f81183c3f1a670188aa3f99fee09f3b3602207c2c53dfcea32554131fb7925ff79bfb3437456bea9f2064a7cc3a2cc35cd2b3012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022048f04122a25fec029aca63adfb56e70d4b317d37b0cbad4930f9b464209fcf3f022059ff9c8f4d4476072bb53414eed1d9a94b042b7df84b2ab76ab63d6b4a49c2580121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4924647L);
    outputs.put(SENDER_CHANGE_84[0], 925157L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "14479cf2ece271b6c30b15de23350c622b29273efdd853912c6bba27a971dc18";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3e51d0e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204e7244b0000000000160014657b6afdeef6809fdabce7face295632fbd94feb3f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc024730440220786de8e391da69173175a4afe97632ddf44d20c3515d0f9fbb6560075c49c52902205a5a8fe9a9c56c5b0988bd798455898b16acb18988a6fbb8a476404205dfd3f2012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100a51fa21426017033c2d754fe222a40c159305362205dda28e70625a51078237d022005c6949a0064d35b5cc8bfa024fd312c6f9ec0860b151f953bb619dbe2c16a410121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 3721126L);
    outputs.put(SENDER_CHANGE_84[1], 17401L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "dfe9cfa315eb3fe1edd9ab99d6c90335d7b1e4398ccaafc49c2af32702e5731d";
    raw =
        "02000000000102d8e352d0689f2b86c78e1c561056ef6346a5a827bce52b7c22a90535ae2db7140700000000fdffffff616b5bf2856ac0a62550b718327ecb85948f1849ddfae4dde5f38837c2843d660300000000fdffffff190000000000000000536a4c50baf1b5878436c37a11039cb94a28fd7aed7c7d5f76593c82e7b36ece3f8b88a769b401b6403ff3e470ec15b91b52036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f94300000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57399f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea9f8701000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358309f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde1a6c738000000000016001440852bf6ea044204b826a182d1b75528364fd0bd024730440220058c97ace926ff4457347a9f703a6690e56a1aadfe6c84e49f4a4b8f482997bf02203a5ff79bc4f0cfa6bd953d4564eedb54365051c88fa61f33e36d0829e55e77e2012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402205ba5f619be4e9704c2f9f3fa7196a0a789f7a0887f39d52865e32a4b0514b19b02202f464d9d7089c66ca3771dd00403122b70f760148826ccf9960805b85f5ea9090121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 925157L);
    outputs.put(SENDER_CHANGE_84[0], 4924647L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "8b6a82e6f5d2dd98da0836d98eb9bf8542097f955b1f82d567281030dc4dbbc2";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3e51d0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febe7244b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc0247304402200112db6cff796891c56a96217369cff4b08e8174ee276a70612dd0d3dfd6a39702200f888aa15373a1a541852bbb2416a806241b86423bfe1654f9d8c171f26cd979012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402207b991a042ef3f026c72f448f820563ea6bac0575d14fb0aab04a13d2cddb176502205354d38dfdff23c9d021b4db23645c3baed8db4b811827f3705ad554548f9d1f0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 924944L);
    outputs.put(SENDER_CHANGE_84[1], 880914L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "c399e8533c926a49f31c1b17fd57a0f317ce9db306c96d430483fccd258411bb";
    raw =
        "02000000000102d622aa12d6810b40bc74cd63027bb1f77448bcc0f514bc0e03f1841b1de13d360700000000fdfffffff4a530ce5e3e8371a49b1181802c110ab52963ecc9aa837e7e63cba0019d5c3a0100000000fdffffff080000000000000000536a4c50655e92aa2f9480938e37353b98d65c2bb603ec85b5109fddb7cc3ed224f85a34a861b8d539dbaf8b49daf3f5ec5e036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae312710d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c8229101d0e000000000016001440852bf6ea044204b826a182d1b75528364fd0bd3f430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c9843f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f02483045022100baa38226a2e8b366d30b39af79520b1e25ec270071dd342a28ddebd3b01c8618022070af266e1ff2337e5494f0cc42dc5f790b0af7a5650e2f56ab6cc5b175552296012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100c594710632d3ef9274bff05820bc077dfe23312a2df2598fad2e207f3a33a2210220063954a10ff4fc4e74997596a9a45ca086edd3a9d6b835520682e2c71fb2b5f20121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[2], 47862L);
    outputs.put(SENDER_CHANGE_84[2], 47862L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + senderIndex001 + i], 100255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + counterpartyIndex001 + i], 100255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 5000L);

    txid = "937354712ed32dcb835281fe59d70405983cf92354fb7bb62dd504e5f641ae22";
    raw =
        "020000000001023ecdcf6d31dd0dab317d0ba4a06a1245653fe2c9ba15f507020077ff34caf4650700000000fdffffff38bed4ee6a473795b92a2e124252b3aeaab547efa3ae9dba6181d9fb722231e00000000000fdffffff150000000000000000536a4c50b90a78e20a8774dcb5fa54ad73cdbfb42781f679ef6ed953877b69fed55e82f7a77207f8329b4d223a1af7845e42036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0188130000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae3f6ba0000000000001600145fadc28295301797ec5e7c1af71b4cee28dfac32f6ba000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a9f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f870100000000001600149f657d702027d98db03966e8948cd474098031ef9f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002473044022061c7cf5d91057d46ee5269514e080ba64e0798378aa529289e193692fa44c16d02200a0dba7033555525bd9a4e57cbdbee5903bead2a94ee50c6596be0b7bd2f0d1f012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402205ca58b1b087a4d3b6b80a426752c6e243539fd865d9e1f8c070a9a1cbaad4b48022037fce167d734a9e71284f19332b7e688ddd393563b0f29bf48d3c2177b71d4be0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 4054647L);
    outputs.put(SENDER_CHANGE_84[0], 4125157L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 5000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 5000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 148750L);

    String txid = "54bd07725efdea5d53b5a5e59f59dcca5fc0d9783bc31172ba045c932704eb43";
    String raw =
        "02000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000fdffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000fdffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e450200000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae377de3d0000000000160014657b6afdeef6809fdabce7face295632fbd94febe5f13e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02043f4c4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55a3f4c4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d3f4c4c00000000001600149f657d702027d98db03966e8948cd474098031ef3f4c4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100c7748f1e9d7770785cd1c5dd9f9158e6f10d4ede74a4e18e1402ebb1951ec5d102202c2f493e354f271e4fea9a43e412aeb944b394668995f1edb30c4925b88ddc3a012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402201274c42fb61a659b9c33ae883cd8c941e17e8fe9af86d70374650b6f6bf8d69602205296c51d4355f6d6b0b6a5152d7be4edd65955672a49ce799c507ce49e56860b0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49ed2040000";
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
    outputs.put(COUNTERPARTY_CHANGE_84[1], 67357L);
    outputs.put(SENDER_CHANGE_84[1], 67357L);
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[senderIndex005 + i], 1000255L);
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[counterpartyIndex005 + i], 1000255L);
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L);

    txid = "0b29203845cbbe4a0b53492f143f61ff1f9c88b61f3a81a68c051501ddb698c2";
    raw =
        "02000000000102b9873d21f4c5e693fa156b006be1d215ae2f894cbdad6e605cc61a0fda1438180300000000fdffffff18e07c3aabe3f4427f8ffde32f9c8454b39096420ea86c02f3c16c337b3f47560700000000fdffffff0c0000000000000000536a4c50e1b48aacc5050cdcc26a33b8de3dc88774fded5922be10198a98d232e85bbb15cf00ca34c871f8c612fd6edaf43e036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae31d0701000000000016001440852bf6ea044204b826a182d1b75528364fd0bd1d0701000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82293f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002483045022100b72f81c9934508145d3992057d25cd43c981e70f140b139f9647c2563f3aaf60022033cd288ef2db56fa451151e8eba2bcf1b59c1f4ab08037f28cae9f48b4cfded50121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e024830450221008052b63c6e5b37d04893d0c33e7ed49b1332d59613d8019cc556db9b781b983a02205c0234686f560a1f617925571f2dc5f2bac43f46ab1cef947d4a92883779cdde012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5d2040000";
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
