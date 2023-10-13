package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.cahoots.AbstractCahootsTest;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Context;
import com.samourai.wallet.cahoots.tx0x2.Tx0x2Result;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
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
  public void tx0x2_opReturnSender() throws Exception {
    int account = 0;
    Pool pool = pool01btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TX
    String poolId = "0.01btc";

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;

    String txid = "f4d4945095c6156490d26a7277117f65a4d52a0af8aca130b1a60a1b8e8813fe";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd647e00e0000000000160014657b6afdeef6809fdabce7face295632fbd94febebe60e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea420f0000000000160014017424f9c82844a174199281729d5901fdd4d4bcea420f00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea420f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739ea420f00000000001600141a37775cede4d783afe1cb296c871fd9facdda30ea420f00000000001600141f66d537194f95931b09380b7b6db51d64aa9435ea420f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997ea420f000000000016001429eeb74c01870e0311d2994378f865ec02b8c984ea420f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029ea420f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f01ea420f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffea420f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383babea420f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fea420f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f182ea420f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea420f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaea420f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662bea420f00000000001600146be0c5c092328f099f9c44488807fa5894131396ea420f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1ea420f00000000001600149f657d702027d98db03966e8948cd474098031efea420f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311ea420f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fea420f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fea420f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2ea420f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a1ea420f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c867ea420f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbcea420f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830ea420f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102483045022100850e27c62d75c5342b14ae7442534e5222bd370bf4da8f3d7aa8106c3d00acc8022003164b811b2af5af510e842381657b57439ad551411655718b6f5b56b09ed662012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100ce4bcbc963e9354fb6e2c9945cf135a228073a38ebcf2309f0789bf7ddaf9626022020076b6f22bf2eb4265cda10943d61f5cad54ec7d25938d4dfca0210506eb8d40121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        1000170L,
        976619L,
        974919L,
        42500L,
        0,
        0,
        0,
        0);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy()} */
  @Test
  public void tx0x2_opReturnCounterparty() throws Exception {
    int account = 0;
    Pool pool = pool01btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 1);
    UTXO utxoCounterparty2 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 1);
    UTXO utxoCounterparty3 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);
    // utxoCounterparty3 will be the first TX0 input (sorted by BIP69)

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);

    // verify TX
    String poolId = "0.01btc";

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;

    String txid = "915c7692c2358d85fde1e4c8f739eebfbac612bfcfd9df70a184d9bdd2db4984";
    String raw =
        "010000000001049eac9e2cc0ee4a650f450b8226ab4d18db7c83118e2732f2a347157bcdb95f8f0300000000ffffffffd1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff63bbe1df2714feb8292e92eec61785f848785ddd53d69449e7e5845034d056c00200000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff200000000000000000536a4c50ec67fae3459d10eefc88506cae73ea28dbd275037304b22efc2ca0d2324d13a16224dc3941d56e984fa3d13b5165036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd604e00e0000000000160014657b6afdeef6809fdabce7face295632fbd94feba6e60e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea420f0000000000160014017424f9c82844a174199281729d5901fdd4d4bcea420f00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea420f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739ea420f00000000001600141a37775cede4d783afe1cb296c871fd9facdda30ea420f00000000001600141f66d537194f95931b09380b7b6db51d64aa9435ea420f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997ea420f000000000016001429eeb74c01870e0311d2994378f865ec02b8c984ea420f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029ea420f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f01ea420f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffea420f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383babea420f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fea420f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f182ea420f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea420f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaea420f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662bea420f00000000001600146be0c5c092328f099f9c44488807fa5894131396ea420f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1ea420f00000000001600149f657d702027d98db03966e8948cd474098031efea420f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311ea420f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fea420f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fea420f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2ea420f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a1ea420f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c867ea420f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbcea420f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830ea420f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102473044022065c21277e70a0195ee1bd8c556bf5c2aa8fde2b4b069b537e1b64cf2137f252a0220373d3b638de71eb636d3c578e0e1253d9ab36565e276c5a218897badd638a99801210377ff2235365da98e9c2d1f527b541548fbabfca5cea45a9810a7f62df19571b502483045022100e94b453b90751d59b05d7aba97073835897afe61372d9d5c1dd7b0cb1193e9b602206798e28bf982cf1cb3e3b37bfe52aa1f45b072906e220d78d7b70676f9a20a5d012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100ea626c4e8b75a62669f972d0089bcc4ad58e8a68f0c5646fb4d823402b760e19022069b38e044a1960919d0d3db31cde1dc4a2566bae179d90e09a38415bcb03be40012102c0730a3041d3ac8605feb7bfb55ff1c64564fc6866cf089e77116db802e0351202473044022035293eef29f8c708d15f8ba8a5c7bbbecba75d7e5e8025f39ad47e4458115f35022063fd92d340ced94356b8b31cc95ece75391318f0b204d9e1de915aa7319430a80121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        4,
        nbPremixSender,
        nbPremixCounterparty,
        1000170L,
        976550,
        974852,
        42500L,
        0,
        0,
        0,
        0);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }

  @Test
  public void tx0x2_maxOutputsEach() throws Exception {
    int account = 0;
    Pool pool = pool01btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 40000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 50000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(1, cahoots.getTransactions().values().size());

    String poolId = "0.01btc";
    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    int nbPremixSender = 35;
    int nbPremixCounterparty = 35;

    String txid = "b2c4b799a415eca295e0ec81257367947f639e612ee09a945def9aef4bb2a7dd";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff4a0000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6ea420f0000000000160014017424f9c82844a174199281729d5901fdd4d4bcea420f00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea420f0000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267fea420f00000000001600141439df62d219314f4629ecedcbe23e24586d3cd3ea420f000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442dea420f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739ea420f00000000001600141a37775cede4d783afe1cb296c871fd9facdda30ea420f00000000001600141bcc24b74b6d68a6d07a34b14e6d4fd72e998a62ea420f00000000001600141f66d537194f95931b09380b7b6db51d64aa9435ea420f000000000016001423631d8f88b4a47609b6c151d7bd65f27609d6d0ea420f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997ea420f00000000001600142525a95f3378924bc5cec937c6a7a1b489c5ff86ea420f000000000016001429eeb74c01870e0311d2994378f865ec02b8c984ea420f0000000000160014378ac72b08d43acd2d9e70c6791e5f186ec395dcea420f00000000001600143ac59e5cdf902524b4d721b5a633a82526c53597ea420f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029ea420f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f01ea420f000000000016001440d04347d5f2696e4600a383b154a619162f5428ea420f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffea420f000000000016001441a73bec4bd8c083c62746fcf8617d060b3c391aea420f00000000001600144288958e3bb02ba9c6d6187fe169279c71caa4e6ea420f00000000001600144518c234185a62d62245d0adff79228e554c62deea420f000000000016001445cc6ccf7b32b6ba6e5f29f8f8c9a5fe2b559529ea420f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383babea420f0000000000160014482e4619fb70e25918bdb570b67d551d3d4aab9fea420f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fea420f00000000001600144ecd8a26f6fc2ae301bbc52358d95ff50137ee6bea420f0000000000160014524a759e76003300ccb475eb812e65817c6653c5ea420f00000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d56ea420f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f182ea420f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea420f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaea420f000000000016001462e123682b149978f834a5fce14f4e71cdd133e2ea420f0000000000160014635a4bb83ea24dc7485d53f9cd606415cdd99b78ea420f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662bea420f00000000001600146be0c5c092328f099f9c44488807fa5894131396ea420f00000000001600146ff0703b7b540c70625baa21448110f560bcb25cea420f00000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc5524ea420f000000000016001476b64af1eb81d03ee7e9e0a6116a54830e729573ea420f00000000001600147dfc158a08a2ee738ea610796c35e68f202cf06cea420f0000000000160014851204bc2e59ace9cfbe86bbc9e96898721c060dea420f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1ea420f00000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bfea420f00000000001600149f657d702027d98db03966e8948cd474098031efea420f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311ea420f0000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e3ea420f0000000000160014ac64d97c6ee84eff2ce8373dfe5186f6dda8e3acea420f0000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154cea420f0000000000160014b3332b095d7ddf74a6fd94f3f9e7412390d3bed9ea420f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fea420f0000000000160014b696b85812d9b961967ba20fa8790d08f8b9340bea420f0000000000160014b6e1b3638c917904cc8de4b86b40c846149d3530ea420f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fea420f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2ea420f0000000000160014c1c95595d7b48b73f5b51414f807c5bd9f237985ea420f0000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafeea420f0000000000160014c88fb64ea3063496876c224711e8b93c18d4bb53ea420f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a1ea420f0000000000160014cdf3140b7268772bd46ffc2d59fa399d63ecb8baea420f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c867ea420f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbcea420f0000000000160014e7056147da987fc9ca73003d5b807ec145e1b4ceea420f0000000000160014e736d0bbc2bcfbec2c577223c1f75d096440fd01ea420f0000000000160014e9339ff8d935d4b9205706c9db58c03b03acc356ea420f0000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700bea420f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830ea420f0000000000160014f0e99871ae8ce7b56a9e91a5bea7d5e4bffcb8ccea420f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde1ea420f0000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408aea420f0000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae1cdc4b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02049c72e40000000000160014657b6afdeef6809fdabce7face295632fbd94feb0247304402201dedc9d90af9b17c362f63fb873379440939a7df824b8d51d7d1a18ee58dc59302202746d7ffc9b47b0baca903e157cf08000ec1d8e5e250827d76002b01ec822d8f012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100f1e54600bddb2c17afb7869e66bbf0c13ff79fdb1055716ef9f54be6391c094b02206c4859760ce6bcc9430d3722d086213eea055d97da3b323e54ff6a413c541d720121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        1000170L,
        4971548L,
        14971548L,
        42500L,
        0,
        0,
        0,
        0);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletDecoyTx0x2Test} */
  @Test
  public void tx0x2_cascade_pool001() throws Exception {
    log.info("Testing Tx0x2 for pool 0.001");

    int account = 0;
    Pool pool = pool001btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 500000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 1000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(1, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.001btc pool
    String poolId = "0.001btc";
    int nbPremixSender = 4;
    int nbPremixCounterparty = 9;

    String txid = "b7037eb7adca2f2871f153d1ecdaaa79107f5f2d150cc33b76c3e1de416135ca";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff110000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d018813000000000000160014751e76e8199196d454941c45d1b3a323f1433bd61b770100000000001600144e4fed51986dbaf322d2b36e690b8638fa0f02041b77010000000000160014657b6afdeef6809fdabce7face295632fbd94feb4a870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a4a8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57394a87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29974a870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f4a87010000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d4a8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea4a870100000000001600146be0c5c092328f099f9c44488807fa58941313964a870100000000001600149f657d702027d98db03966e8948cd474098031ef4a87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3114a87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f4a87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8674a87010000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc4a87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583002483045022100cad4ef5aef95a1578137ce6401cdabe9a59ddbe236edca323fb5aa76c39a751a02205e9a01074abbadfc41c731d806970ede1f3d799739efa0ab8ea3ac8ea777f343012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100f0552800fc4ba38adc222c3620d0d5ca6932385a37aacf8b51a7ff5bc51612760220606436a43e2eb3c42bb4d176d336b9172d71108819cb54b89448e2c3a24a379d0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        100170L,
        96027L,
        96027L,
        5000L,
        0,
        0,
        0,
        0);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
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
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setCascade(true);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(2, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.01btc pool
    String poolId = "0.01btc";
    int nbPremixSender = 9;
    int nbPremixCounterparty = 19;

    String txid = "f4d4945095c6156490d26a7277117f65a4d52a0af8aca130b1a60a1b8e8813fe";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff200000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd647e00e0000000000160014657b6afdeef6809fdabce7face295632fbd94febebe60e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea420f0000000000160014017424f9c82844a174199281729d5901fdd4d4bcea420f00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea420f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739ea420f00000000001600141a37775cede4d783afe1cb296c871fd9facdda30ea420f00000000001600141f66d537194f95931b09380b7b6db51d64aa9435ea420f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e2997ea420f000000000016001429eeb74c01870e0311d2994378f865ec02b8c984ea420f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d89029ea420f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f01ea420f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ffea420f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383babea420f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107fea420f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f182ea420f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea420f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaea420f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662bea420f00000000001600146be0c5c092328f099f9c44488807fa5894131396ea420f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd1ea420f00000000001600149f657d702027d98db03966e8948cd474098031efea420f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd311ea420f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572fea420f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276fea420f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be2ea420f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a1ea420f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c867ea420f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbcea420f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830ea420f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde102483045022100850e27c62d75c5342b14ae7442534e5222bd370bf4da8f3d7aa8106c3d00acc8022003164b811b2af5af510e842381657b57439ad551411655718b6f5b56b09ed662012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100ce4bcbc963e9354fb6e2c9945cf135a228073a38ebcf2309f0789bf7ddaf9626022020076b6f22bf2eb4265cda10943d61f5cad54ec7d25938d4dfca0210506eb8d40121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        1000170L,
        976619L,
        974919L,
        42500L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.001btc pool
    poolId = "0.001btc";
    nbPremixSender = 9;
    nbPremixCounterparty = 9;

    txid = "404bcbd93c0ccf603769fc207b37fe440aa9dc291274cec8af1ff5f6d5ba06e1";
    raw =
        "010000000001028d76b8eace5eb2c5d2c7421253160502c75652fd2490838bc8d1fbd65ed10a0f0200000000ffffffff8d76b8eace5eb2c5d2c7421253160502c75652fd2490838bc8d1fbd65ed10a0f0300000000ffffffff160000000000000000536a4c5000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008813000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6801301000000000016001440852bf6ea044204b826a182d1b75528364fd0bd801301000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f8701000000000016001415b36f0218556c90ea713f78d4a9d9e8f6b5442d9f870100000000001600143ac59e5cdf902524b4d721b5a633a82526c535979f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f8701000000000016001462e123682b149978f834a5fce14f4e71cdd133e29f870100000000001600146ff0703b7b540c70625baa21448110f560bcb25c9f870100000000001600147055ad1d5f86f7823ff0c4c7915d6b3147cc55249f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014a7511c3778c3e5bc1b16f95945e4d52be430e7e39f87010000000000160014aea5b03bcc8bdc4940e995c24a7ffe774f57154c9f87010000000000160014b6e1b3638c917904cc8de4b86b40c846149d35309f87010000000000160014c72ae606b371fc9fbf6bf8618374096e9b4caafe9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014e9989a636c0f3cae20777ac0766a9b6220e4700b9f87010000000000160014fbcdad4696c0e0e9dbb4c40772ac55683463408a9f87010000000000160014ff4a86dbd7efe4a7ab616c987685229db24d91ae0002483045022100fa3fab396a7cf0b697301f1145a070de3467822fd784774d96b895cb878cd65302204948535546b7570eb2c85cb62c7d321af804dc1f79a49fc40b88a05e88df55ab0121027f2555837391b7a8217beb2794cbc12835793f8eb91a0a184dafd1fc5fca308b00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        100255L,
        70528L,
        70528L,
        5000L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
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
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 10000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    tx0Config.setCascade(true);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(3, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.05btc pool
    String poolId = "0.05btc";
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;

    String txid = "df29752185605e080b2de89d6f5be17aa2a5f42f23a77caca04c317d7b2837c3";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e45020000000000160014751e76e8199196d454941c45d1b3a323f1433bd6d6254b0000000000160014657b6afdeef6809fdabce7face295632fbd94feb2a274b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea4b4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea4b4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea4b4c00000000001600149f657d702027d98db03966e8948cd474098031efea4b4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100aaae415f13423db99d1a3a53ec024575976286da210164c1cbb94ccf158e336b02206fbd0eec74cdf055babe708e41b257c1a5087f5d6168cf3cc8bb42fb0b02e2c9012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d50247304402202e58b4fa3024aacb17f227b658120d5a8aa9c5036069b89925e1b668f8914cd90220207e53c663210c7abaa0285e4df689df85346168f5c2db317ebdb6ef97296ea30121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        5000170,
        4925226,
        4924886,
        148750L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.01btc pool
    poolId = "0.01btc";
    nbPremixSender = 4;
    nbPremixCounterparty = 4;

    txid = "17b24ab293f93482e7d3b1f112e8283e706e3736fb297d4199fad75bb73d3d8b";
    raw =
        "01000000000102111553c70a0ffadc2ff8c0eca741fc5e5aa4bd2a1e357effac59af043b0d05630200000000ffffffff111553c70a0ffadc2ff8c0eca741fc5e5aa4bd2a1e357effac59af043b0d05630300000000ffffffff0c0000000000000000536a4c50000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6b5c40d000000000016001440852bf6ea044204b826a182d1b75528364fd0bd09c60d000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82293f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358300002483045022100eb39b217c1410058e0022a22f0c8fe43398358e5e33ac8e8ac75ef9963e3dc6f022052344df864d5003d0cda526ceee6880297e4fb496f81918fb0b770455e2987800121027f2555837391b7a8217beb2794cbc12835793f8eb91a0a184dafd1fc5fca308b00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        1000255,
        902665,
        902325,
        42500,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.001btc pool
    poolId = "0.001btc";
    nbPremixSender = 8;
    nbPremixCounterparty = 8;

    txid = "dab7aa70817819455e43cd2e79255d93a856d2167212b4fffc4f7984eef1e230";
    raw =
        "010000000001020051e2c71c11767e01ea2a21e9e6587ef99c82400009b77b8a3a6437bd1d6fea0200000000ffffffff0051e2c71c11767e01ea2a21e9e6587ef99c82400009b77b8a3a6437bd1d6fea0300000000ffffffff140000000000000000536a4c5000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008813000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6047d0100000000001600145fadc28295301797ec5e7c1af71b4cee28dfac32047d010000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde100024730440220222f0f88ff25ae80419e9c2b91f97adec0afcc5ca210d02878c87faf2c27b663022049be7039c88da28b0b377726ca3ccb76c27c86f1fed181390ee337d7dd0206c4012102bc4770f5ee1db88891e2b139e1fab48566a8e7ec80656db33674c9e2c5193b8800000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        100255,
        97540, // Change outputs equal
        97540, // Change outputs equal
        5000L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
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
    Pool pool = pool05btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 6000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);
    tx0Config.setCascade(true);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(2, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.05btc pool
    String poolId = "0.05btc";
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;

    String txid = "0bb8eba46565b62ec87ff4659c9053b6ffdb59c51a16286dd932d9b499e23cea";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e45020000000000160014751e76e8199196d454941c45d1b3a323f1433bd62a1e0e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204d6254b0000000000160014657b6afdeef6809fdabce7face295632fbd94febea4b4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea4b4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea4b4c00000000001600149f657d702027d98db03966e8948cd474098031efea4b4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100fd97fa284d579ea2d274ecbd15acafbdfe45f23bdb7af666cadb0d2f0d901c6d02202cb6be859748a91da1d660f6895c3580f4b470b3a95d6fdaa050848ccd169621012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502473044022041943fd65ce4b705171cc9f14b1424badcca60444fcd677f22a15065f12f742602204640a0935da9e53d5c97d039668d0618d12ecc9fec5028a7a82ceea25d0096aa0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        5000170,
        925226,
        4924886,
        148750L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.01btc pool skipped
    counterpartyChangeIndex++;

    // 0.001btc pool
    poolId = "0.001btc";
    nbPremixSender = 9;
    nbPremixCounterparty = 12;

    txid = "f1eb37a41d2ca3dc145ccb61f1c8f7606c6cd430864d0d0f19001c7f08b8c1d1";
    raw =
        "01000000000102dc68344a52da5fac2c52e96aff8f62ec3e0c7147634f3b0e613cec6b677a78b40200000000ffffffffdc68344a52da5fac2c52e96aff8f62ec3e0c7147634f3b0e613cec6b677a78b40300000000ffffffff190000000000000000536a4c50189580b5e723cb1a0f82327330b0a1436df15c07e9bc1f37a702b7d4f449ed8b7cb5eb22316b43b029e6e7bccec2036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d018813000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6e34d00000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82299f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f8701000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57399f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f870100000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff9f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f870100000000001600145ba893c54abed7a35a7ff196f36a154912a6f1829f8701000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea9f8701000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd19f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be29f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b358309f87010000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde1b2be380000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce841202483045022100d5bd019a248166ab93e389967ae1a1e7ff372fd1788aaa977bf20e5ffed1501802204ff53a126ba3cc9865234d13e9d571c63e7e267450f10d964908475aed6bf8b50121027f2555837391b7a8217beb2794cbc12835793f8eb91a0a184dafd1fc5fca308b0000000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        100255,
        19939,
        3718834,
        5000L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }

  /**
   * Sender's change is large enough to mix in 0.01btc pool. Counterparty's change is not large
   * enough to mix in 0.01btc pool. 0.01btc pool is skipped to 0.001btc.
   *
   * <p>Compare with decoy tx0x2 test: {@link
   * WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05_skip01()} Change values might differ
   * slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_cascade_pool05_counterpartyNo01() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05 & 0.001");

    int account = 0;
    Pool pool = pool05btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 20000000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 6000000);

    // initiator: build initial TX0
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);
    tx0Config.setCascade(true);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(2, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.05btc pool
    String poolId = "0.05btc";
    int nbPremixSender = 3;
    int nbPremixCounterparty = 1;

    String txid = "e8127942134770966c18aa838d8ab7b1793e053f6eb3af2b883e9bb15b5f8a09";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e45020000000000160014751e76e8199196d454941c45d1b3a323f1433bd62a1e0e0000000000160014657b6afdeef6809fdabce7face295632fbd94febd6254b00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea4b4c000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda5739ea4b4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea4b4c000000000016001461e4399378a590936cd7ab7d403e1dcf108d99eaea4b4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc0247304402203ebb1fb59ac798d08165546a6f9efa6417189d8a209bfa0bd438a70a0f45810d0220553cc230d9d92ffd412570779dcfbd9228d309e8f60e88f9a933264d246f713d012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d5024830450221008cf19ceea974c4e812588f6ed73d9adc3ce9113acd23802233c0a0776f137207022077f9b371598ef309e3894d65002858b6c93b510aded3a2514dab704b6dd7b8570121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        5000170,
        4924886,
        925226,
        148750L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.01btc pool skipped
    counterpartyChangeIndex++;

    // 0.001btc pool
    poolId = "0.001btc";
    nbPremixSender = 12;
    nbPremixCounterparty = 9;

    txid = "703af34ef46c974561bd28017471df2c784603c13e46a01062becbd170ec965d";
    raw =
        "0100000000010232b4840eae0fd08e3ba8cac833d3ba27626e22506867694c8ee202b9778ee3ff0200000000ffffffff32b4840eae0fd08e3ba8cac833d3ba27626e22506867694c8ee202b9778ee3ff0300000000ffffffff190000000000000000536a4c5000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008813000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6e34d000000000000160014acd8d1c4b03edcd73fa34d9a3431cec69bce84129f87010000000000160014017424f9c82844a174199281729d5901fdd4d4bc9f870100000000001600140343e55f94af500cc2c47118385045ec3d00c55a9f87010000000000160014074d0a20ecbb784cae6e9e78d2bece7e0fed267f9f87010000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29979f8701000000000016001429eeb74c01870e0311d2994378f865ec02b8c9849f870100000000001600143db0ef375a1dccbb1a86034653d09d1de2d890299f87010000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab9f870100000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f9f87010000000000160014524a759e76003300ccb475eb812e65817c6653c59f870100000000001600145343a394e8ff7f4f52c978ec697cdd70062c4d569f8701000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b9f870100000000001600146be0c5c092328f099f9c44488807fa58941313969f870100000000001600149c991b06c08b1a44b69fe2dca56b900fd91fd0bf9f870100000000001600149f657d702027d98db03966e8948cd474098031ef9f87010000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3119f87010000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f9f87010000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f9f87010000000000160014c88fb64ea3063496876c224711e8b93c18d4bb539f87010000000000160014d43293f095321ffd512b9705cc22fbb292b1c8679f87010000000000160014e9339ff8d935d4b9205706c9db58c03b03acc3569f87010000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b35830b2be38000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82290002483045022100bf2187082e495595a57d13d1df68e24cc96f642699d30a5610d489788f7b32c6022057e42756258312370b2075397a1bf788255e2b69b581805a99e20ca5cab696bf0121027f2555837391b7a8217beb2794cbc12835793f8eb91a0a184dafd1fc5fca308b00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        100255,
        3718834,
        19939,
        5000L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }

  /**
   * Doesn't reach 0.001 pool but change outputs are splitted anyway.
   *
   * <p>Compare with decoy tx0x2 test {@link
   * WhirlpoolWalletDecoyTx0x2Test#tx0x2_decoy_cascade_pool05_no001()} Change values differ slightly
   */
  @Test
  public void tx0x2_cascade_pool05_no001() throws Exception {
    log.info("Testing Tx0x2s for pools 0.05 & 0.01 & . Doesn't reach pool 0.001.");

    int account = 0;
    Pool pool = pool05btc;

    // setup wallets
    UTXO utxoSender1 = utxoProviderSender.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 9200000);
    UTXO utxoCounterparty1 = utxoProviderCounterparty.addUtxo(BIP_WALLET.DEPOSIT_BIP84, 19130000);

    // initiator context
    Collection<? extends BipUtxo> spendFroms = utxoSender1.toBipUtxos();
    Tx0Config tx0Config =
        whirlpoolWalletSender.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_24, Tx0FeeTarget.BLOCKS_24);
    Tx0x2Context cahootsContextSender = whirlpoolWalletSender.tx0x2Context(tx0Config);
    tx0Config.setCascade(true);

    // run Cahoots
    Tx0x2Context cahootsContextCp =
        Tx0x2Context.newCounterparty(cahootsWalletCounterparty, account, tx0Service);

    Tx0x2Result cahootsResult =
        (Tx0x2Result) doCahoots(tx0x2Service, cahootsContextSender, cahootsContextCp, null);
    Tx0x2 cahoots = cahootsResult.getCahoots();

    // verify TXs
    Assertions.assertEquals(2, cahoots.getTransactions().values().size());

    int senderChangeIndex = 0;
    int counterpartyChangeIndex = 0;
    int senderPremixIndex = 0;
    int counterpartyPremixIndex = 0;

    // 0.05btc pool
    String poolId = "0.05btc";
    int nbPremixSender = 1;
    int nbPremixCounterparty = 3;

    String txid = "e521f68ef2147e2f29e2dbbf48d84366aecd21359efdc348309ef1e184fd4059";
    String raw =
        "01000000000102d1428941eb7e336ce4975d2be2eb25e52124a01b8da49899072826e62c97fea30100000000ffffffff145dd6494b7f99ef1bc18598bd3cd4b33189f0bc0b025e6c60c6c420a89f73c30100000000ffffffff080000000000000000536a4c50994ee75d59ff12a76f5efce443806dfdbab4acf1d9a13aeed77cc9e46af3018a8d53fae635619c5275fa93577aad036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d010e45020000000000160014751e76e8199196d454941c45d1b3a323f1433bd666df3d0000000000160014657b6afdeef6809fdabce7face295632fbd94feb2af23e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f0204ea4b4c00000000001600140343e55f94af500cc2c47118385045ec3d00c55aea4b4c0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3dea4b4c00000000001600149f657d702027d98db03966e8948cd474098031efea4b4c0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc02483045022100cc624265169cb5e24995c5c62299b30cb28595c3747b63e24a6193653b436f11022005d16b37bd1d4e9c8cf024dc7a591fe0999cd36c76783a6bed6d38421412f1cc012102cf5095b76bf3715a729c7bad8cb5b38cf26245b4863ea14137ec86992aa466d502483045022100a2522d5edde9130ebb638dffd1c22647d9353ab9e1576fa01d66bfcee19ccc1402202d992cd732b66babd9af24a8e7e74fa47b61c26e41e7bd77c8640deb6bf551ca0121035eb1bcb96f29bdb55b0ca6d1ec5136fe5afc893a03ab4a29efd4263214c7f49e00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        5000170L,
        4125226L,
        4054886L,
        148750L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.01btc pool
    poolId = "0.01btc";
    nbPremixSender = 4;
    nbPremixCounterparty = 4;

    txid = "9b72b8659ba42bcfd8059617f29cd4fb9a34b07f034ad4d0352a90b6893b30f4";
    raw =
        "010000000001023e174edfb4b9ab4a56839ac470a3e1fb5ab0b0bb179ae86bd137d6f580081f720200000000ffffffff3e174edfb4b9ab4a56839ac470a3e1fb5ab0b0bb179ae86bd137d6f580081f720300000000ffffffff0c0000000000000000536a4c500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000457e00000000000016001440852bf6ea044204b826a182d1b75528364fd0bd04a6000000000000160014751e76e8199196d454941c45d1b3a323f1433bd6099101000000000016001485963b79fea38b84ce818e5f29a5a115bd4c82293f430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda57393f430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e29973f430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f3f430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea3f430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b3f430f00000000001600146be0c5c092328f099f9c44488807fa58941313963f430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd3113f430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583000024830450221008bf2d54cc2ad7c1161f84c971a28c4e786fbaf64aed7c4e1a044a21f61a82a0a0220491328968f109c360bd8d707473872958fd0dff39ce57a7012939bfd9b61c1c30121027f2555837391b7a8217beb2794cbc12835793f8eb91a0a184dafd1fc5fca308b00000000";

    assertTx0x2(
        cahootsResult,
        poolId,
        txid,
        raw,
        2,
        nbPremixSender,
        nbPremixCounterparty,
        1000255L,
        102665,
        32325,
        42500L,
        senderChangeIndex,
        counterpartyChangeIndex,
        senderPremixIndex,
        counterpartyPremixIndex);

    senderChangeIndex++;
    counterpartyChangeIndex++;
    senderPremixIndex += nbPremixSender;
    counterpartyPremixIndex += nbPremixCounterparty;

    // 0.001btc pool not reached

    counterpartyChangeIndex++; // unused change for pool 0.001btc

    assertTx0x2State(
        senderChangeIndex, counterpartyChangeIndex, senderPremixIndex, counterpartyPremixIndex);
  }
}
