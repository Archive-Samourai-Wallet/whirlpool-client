package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.cahoots.Cahoots;
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
    UTXO utxoSender1 =
        utxoProviderSender.addUtxo(
            account, "senderTx1", 1, 10000000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    utxoProviderCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 20000000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

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
    outputs.put(COUNTERPARTY_CHANGE_84[0], 973148L); // TODO TX0X2 adjust value
    outputs.put(SENDER_CHANGE_84[0], 975768L); // TODO TX0X2 adjust value
    for (int i = 0; i < nbPremixSender; i++) {
      outputs.put(SENDER_PREMIX_84[i], 1000262L); // TODO TX0X2 adjust value
    }
    for (int i = 0; i < nbPremixCounterparty; i++) {
      outputs.put(COUNTERPARTY_PREMIX_84[i], 1000262L); // TODO TX0X2 adjust value
    }
    outputs.put(MOCK_SAMOURAI_FEE_ADDRESS, 42500L); // TODO TX0X2 adjust value

    String txid = "1cc0bcdd42e8d7c9957690b50c5726e8c9a03eb3e01b1fb929facac18cd046e2";
    String raw = "02000000000102d54f4c6e366d8fc11b8630d4dd1536765ec8022bd3ab8a62fefc2ee96b9ccf140100000000fdffffffad05bb9c893f5cb9762ea57729efaf4a4b8eb1e377533fddc49d15d01fb307940100000000fdffffff200000000000000000536a4c50fe2d02b53fb8ae665028f965250af6b2e1fe0bf67cba738bd3daa3c3206eaea60714e42c909c49f0e3277c58cca2036e350b47817fe80c931d2e7317d46b6017af2427f201bec425e41ae8d89a029d0104a60000000000001600144b5fcb661533a26619d7917748cd6abf2fea5ae35cd90e0000000000160014657b6afdeef6809fdabce7face295632fbd94feb98e30e00000000001600144e4fed51986dbaf322d2b36e690b8638fa0f020446430f0000000000160014017424f9c82844a174199281729d5901fdd4d4bc46430f00000000001600140343e55f94af500cc2c47118385045ec3d00c55a46430f000000000016001418e3117fd88cad9df567d6bcd3a3fa0dabda573946430f00000000001600141a37775cede4d783afe1cb296c871fd9facdda3046430f00000000001600141f66d537194f95931b09380b7b6db51d64aa943546430f0000000000160014247a4ca99bf1bcb1571de1a3011931d8aa0e299746430f000000000016001429eeb74c01870e0311d2994378f865ec02b8c98446430f00000000001600143db0ef375a1dccbb1a86034653d09d1de2d8902946430f00000000001600143f0411e7eec430370bc856e668a2f857bbab5f0146430f00000000001600144110ac3a6e09db80aa945c6012f45c58c77095ff46430f0000000000160014477f15a93764f8bd3edbcf5651dd4b2039383bab46430f00000000001600144a4c5d096379eec5fcf245c35d54ae09f355107f46430f00000000001600145ba893c54abed7a35a7ff196f36a154912a6f18246430f0000000000160014615fa4b02e45660153710f4a47ed1a68ea26dd3d46430f000000000016001461e4399378a590936cd7ab7d403e1dcf108d99ea46430f000000000016001468bd973bee395cffa7c545642b1a4ae1f60f662b46430f00000000001600146be0c5c092328f099f9c44488807fa589413139646430f00000000001600148b6b1721fc02decbf213ae94c40e10aba8230bd146430f00000000001600149f657d702027d98db03966e8948cd474098031ef46430f0000000000160014a12ebded759cb6ac94b6b138a9393e1dab3fd31146430f0000000000160014b6033f0f44c6fa14a55d53950547349ed7ff572f46430f0000000000160014b819e4adf525db52ff333a90e8d2db6f5d49276f46430f0000000000160014bc8a5ee7ee21f56b1e3723bcddc4c787f6087be246430f0000000000160014c987135a12804d2ee147ccf2746e5e1cdc1e18a146430f0000000000160014d43293f095321ffd512b9705cc22fbb292b1c86746430f0000000000160014d9daf2c942d964019eb5e1fd364768797a56ebbc46430f0000000000160014ef4263a4e81eff6c8e53bd7f3bb1324982b3583046430f0000000000160014fb4d10bd3fa9c712118c7eaa5cbaa6d65b10cde10247304402200344276e930dbaa5b84bd5509a9c1ca6cfd7ed778ee9a229eb1a1d42d3d7d5200220599ef2f6b771a5d8e3a98dacf95769771f513dc0c54497ac29a9e69ad890bb2c012102e37648435c60dcd181b3d41d50857ba5b5abebe279429aa76558f6653f1658f2024730440220639453e649205718d8861d1e5c30d430360ff028719167951d140b26d16867d602207451090fb212d2e856b2b976ade254161885304cadb59d760e5eb40bb28e2308012102e37648435c60dcd181b3d41d50857ba5b5abebe279429aa76558f6653f1658f2d2040000";
    verifyTx(tx, txid, raw, outputs);// TODO TX0X2 adjust value

    // TODO? verifySpendTX() ?
  }
}
