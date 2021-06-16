package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.client.BipWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.utils.MockUtxoKeyProvider;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Arrays;
import java8.util.Lists;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0ServiceTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(Tx0ServiceTest.class);

  private static final long FEE_VALUE = 10000;

  private Tx0Service tx0Service;

  private WhirlpoolWalletConfig config;

  private MockUtxoKeyProvider utxoKeyProvider;

  public Tx0ServiceTest() {
    super();
  }

  @Before
  public void setup() {
    WhirlpoolServer server = WhirlpoolServer.LOCAL_TESTNET;
    config = new WhirlpoolWalletConfig(null, null, null, null, server.getParams(), false, null);
    config.setTx0MaxOutputs(10);
    tx0Service = new Tx0Service(config);
    utxoKeyProvider = new MockUtxoKeyProvider();
  }

  @Test
  public void tx0Preview_scode_noFee() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    ECKey spendFromKey = address.getECKey();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    WhirlpoolUtxo whirlpoolUtxo =
        new WhirlpoolUtxo(
            spendFromUtxo,
            1234,
            WhirlpoolAccount.DEPOSIT,
            AddressType.SEGWIT_NATIVE,
            WhirlpoolUtxoStatus.READY,
            null);

    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = 10;
    long premixValue = 1000201;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = null;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 489987418;

    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);
    Tx0Param tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assert.assertEquals(1000201, tx0Param.getPremixValue());
    Tx0Preview tx0Preview =
        tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(572, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());
  }

  @Test
  public void tx0Preview_overspend() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    ECKey spendFromKey = address.getECKey();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    WhirlpoolUtxo spendFrom =
        new WhirlpoolUtxo(
            spendFromUtxo,
            1234,
            WhirlpoolAccount.DEPOSIT,
            AddressType.SEGWIT_NATIVE,
            WhirlpoolUtxoStatus.READY,
            null);

    Tx0Config tx0Config = new Tx0Config();
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = null;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    // no overspend
    Tx0Param tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assert.assertEquals(1000201, tx0Param.getPremixValue());
    Tx0Preview tx0Preview =
        tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(1000201, tx0Preview.getPremixValue());

    // overspend too low => min
    tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, 1L);
    Assert.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Param.getPremixValue());
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Preview.getPremixValue());

    // overspend too high => max
    tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, 999999999L);
    Assert.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Param.getPremixValue());
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Preview.getPremixValue());
  }

  @Test
  public void tx0Preview_feeTx0() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    ECKey spendFromKey = address.getECKey();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    WhirlpoolUtxo spendFrom =
        new WhirlpoolUtxo(
            spendFromUtxo,
            1234,
            WhirlpoolAccount.DEPOSIT,
            AddressType.SEGWIT_NATIVE,
            WhirlpoolUtxoStatus.READY,
            null);

    Tx0Config tx0Config = new Tx0Config();
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = null;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);
    Tx0Param tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assert.assertEquals(1000201, tx0Param.getPremixValue());

    int TX0_SIZE = 572;

    // feeTx0
    int feeTx0 = 1;
    tx0Param = new Tx0Param(params, feeTx0, feeSatPerByte, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 5;
    tx0Param = new Tx0Param(params, feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 50;
    tx0Param = new Tx0Param(params, feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());
  }

  @Test
  public void tx0Preview_feePremix() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    ECKey spendFromKey = address.getECKey();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    WhirlpoolUtxo spendFrom =
        new WhirlpoolUtxo(
            spendFromUtxo,
            1234,
            WhirlpoolAccount.DEPOSIT,
            AddressType.SEGWIT_NATIVE,
            WhirlpoolUtxoStatus.READY,
            null);

    Tx0Config tx0Config = new Tx0Config();
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = null;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);
    Tx0Param tx0Param = new Tx0Param(params, feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assert.assertEquals(1000201, tx0Param.getPremixValue());

    int TX0_SIZE = 572;

    // feePremix
    int feePremix = 1;
    tx0Param = new Tx0Param(params, feeSatPerByte, feePremix, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(1000201, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 5;
    tx0Param = new Tx0Param(params, feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(1001008, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 20;
    tx0Param = new Tx0Param(params, feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(1004033, tx0Preview.getPremixValue());

    // feePremix max
    feePremix = 99999;
    tx0Param = new Tx0Param(params, feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0Service.tx0Preview(Lists.of(spendFromUtxo), tx0Config, tx0Param, tx0Data);
    Assert.assertEquals(1009500, tx0Preview.getPremixValue());
  }

  private void assertEquals(Tx0Preview tp, Tx0Preview tp2) {
    Assert.assertEquals(tp.getTx0MinerFee(), tp2.getTx0MinerFee());
    Assert.assertEquals(tp.getFeeValue(), tp2.getFeeValue());
    Assert.assertEquals(tp.getFeeChange(), tp2.getFeeChange());
    Assert.assertEquals(tp.getFeeDiscountPercent(), tp2.getFeeDiscountPercent());
    Assert.assertEquals(tp.getPremixValue(), tp2.getPremixValue());
    Assert.assertEquals(tp.getChangeValue(), tp2.getChangeValue());
    Assert.assertEquals(tp.getNbPremix(), tp2.getNbPremix());
  }

  @Test
  public void tx0_5premix_withChange_scode_noFee() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    utxoKeyProvider.setKey(spendFromUtxo.computeOutpoint(params), address.getECKey());

    BipWallet depositWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet premixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet postmixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet badbankWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = 10;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = new byte[] {1, 2};
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 489988499;
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);

    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(spendFromUtxo),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview,
            utxoKeyProvider);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assert.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee + change

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "e11e81e6238a69f5b3c77ad90c59d6c85a45a549fc03400abece68e1818684db", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff0d0000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f00000000001600142540e8d450b7114a8b0b429709508735b4b1bbfbd6420f00000000001600145b1cdb2e6ae13f98034b84957d9e0975ad7e6da5d6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f0000000000160014833e54dd2bdc90a6d92aedbecef1ca9cdb24a4c4d6420f00000000001600148535df3b314d3191037e38c698ddb6bac83ba95ad6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f0000000000160014adb93750e1ffcfcefc54c6be67bd3011878a5aa5d6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c8193a1341d000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87024730440220300bd307637ed85bef106e7dd67db7737a1bae1f167813124d36d4a13250277002207822a332bdf472ac623787ef88c76a5aa8df160a70606dcffbc0e4aae9841d0801210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  /*
  @Test
  public void tx0_maxpremix_withChange() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    ECKey spendFromKey = address.getECKey();
    UnspentOutput spendFrom =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            900000000,
            address); // large balance

    Bip84Wallet depositWallet =
        new Bip84Wallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler());
    Bip84Wallet premixWallet =
        new Bip84Wallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler());
    Bip84Wallet postmixWallet =
        new Bip84Wallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler());
    Bip84Wallet badbankWallet =
        new Bip84Wallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler());
    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = Tx0Service.NB_PREMIX_MAX;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = new byte[] {1, 2};
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 299899999;
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(tx0Data, feeSatPerByte, premixValue, changeValue, nbOutputsExpected);
    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(new UnspentOutputWithKey(spendFrom, spendFromKey.getPrivKeyBytes())),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(feeSatPerByte, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee + change

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "4894aaa78aaf1460098befa81d111b1f2702f71f3134a0365f921d4fc72ffc20", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000fffffffffd5b020000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f0000000000160014004834b886cc8e265643f910b47959b3526b0bb5d6420f000000000016001400e49d92c3e61056ccf1b9e064b635f98605f5efd6420f0000000000160014011a0f8df522ef5fc20c4ce5a927e7a72a8f8ac8d6420f0000000000160014015219732f50c74f1a9e1f4445876044d45aa915d6420f000000000016001401859e438d75b4992eb9b33d0fa5100d63da6a66d6420f000000000016001401ddf76f209c3c5a3edd4d69fe129627dc379ec7d6420f000000000016001401e367903c67215ba9826afc410c1cbc9175c815d6420f0000000000160014033a9a1721c4ec977231b45ae3cf22650f4324edd6420f000000000016001404417785a440321fe61dd9117e08d81e8b7dd34bd6420f000000000016001404ed85bc78241f8c26c75264bbe1d04097e1cbe1d6420f0000000000160014056f853a8292c16869b8ab9bc1a6f35dd4e10e02d6420f000000000016001406621e4053d8efdbc3aa41ef1a8f0097e45391e7d6420f0000000000160014069b265dbe807efc35f76a5eaaa14775fde45f71d6420f00000000001600140774c98855e76c63d2992700ef0f676e90e718c7d6420f00000000001600140808a4b1d7fd1906d05afb6c601a8a74f5ccc8a2d6420f00000000001600140892f2617945684132af0c60eb1280a45d7ccb93d6420f00000000001600140a0d5bea5a4b97b572d638db831e810acb5a622fd6420f00000000001600140a93d0cbf097c2dd54930f6670bc0aed09e7a190d6420f00000000001600140bf7a0a5738108217774487de155875a3a8d0912d6420f00000000001600140bf7e6b30d37af8340f839368ab2a6539ff60efbd6420f00000000001600140c2fee5629a96b266f61e997d6b77a85107f7513d6420f00000000001600140c6199577425d52ec15dd1f9de266025f875eea4d6420f00000000001600140c8b2997fff57214fc569b109541ea7f968c3a4dd6420f00000000001600140d436d5b701aa0a33bde6f7c6e3410748409fce5d6420f00000000001600140dc374a11ed4f414c54855e45914450a8689355ad6420f00000000001600140ec01bb97a1fb5ea525d6ca859ea07bbac2a0d9ad6420f00000000001600140ee4c1014e0bff9cf1819c344253ff008c3400e3d6420f00000000001600140f91a734f8f2c916cdbe675aa2b0f2a7733452ecd6420f00000000001600140f9f71e6150ada2d1c5eb1948c010867b015f3a5d6420f0000000000160014114a1e06998ed8a3284dd4ed8f26d63c00035286d6420f00000000001600141180a930155e640aa695cd19596f30bddc945b5bd6420f000000000016001411bc6d0db0b89b424fa0da809c4ec77b56a71741d6420f000000000016001411dd9e6c1ff0b5539c22aaac5291bd19b928041fd6420f000000000016001411e0e0ae1b89d37b3f35f131516848bbf6729c62d6420f000000000016001411f20dc2186828e015c75cf9ff93a120d6888ea9d6420f000000000016001411f87d4ebed003ffe8de03e149a9840f549a1449d6420f00000000001600141203cb4a530223dfecc3f4b1e3433dd11b04f425d6420f00000000001600141205162cea5285f889b8d33bb264efc9dbbf2a5ad6420f0000000000160014123b5b20cfbb59479a8d231aa5c122f5854a5945d6420f000000000016001412a77554ca179febadfd0b9f55eccaae257caab1d6420f000000000016001412b83b33788bb72723da871634d436e233fbd311d6420f000000000016001412ca9d549ee451e0f70ad37911abd1df993ba708d6420f0000000000160014131bb0ffed0409ce807a0edb7c1eb2f2b54a44e4d6420f000000000016001413d7274ad3aba38aa60b6d86aac66d409bfb613ad6420f00000000001600141400cd59df8d3d9153765384cc922d32d4e0e96bd6420f0000000000160014149347a75090f42e0a9a0d8ceda54ca689b301b0d6420f000000000016001414e7520f2c3073e761671b999683cb08433b7475d6420f000000000016001415a7dd86ba1f56cd25937f0604571230856c9e25d6420f000000000016001415f931abbb52e08d2468efb05cb752e0d31abb33d6420f00000000001600141600b76ad001260a9d695c3a4ddac0b816f5f14ed6420f0000000000160014168a4615351069ac54e6345e56be4181e06ac917d6420f00000000001600141690c620153b1d8673ab15810b4a005b49688aa8d6420f000000000016001416b5b76813905bba45ca927f2573588235d45082d6420f000000000016001417049ad455817e951c923679b6039ad9cc205e7dd6420f00000000001600141707c06b427e639b4c9e1d5952213482e3e6199fd6420f00000000001600141710068ca94b9e3187967da500d567e876062359d6420f0000000000160014177c8d10d8c5030b12dba3e1e7aee5cbe4456d99d6420f0000000000160014179d5a55c8fd629b15252892308e3e3080c808eed6420f000000000016001417b54dbddf4c4df770db4cd46871a9a81cb63ee7d6420f000000000016001417d7569b534f69643f8d795ed273e0ba04972b4fd6420f000000000016001418319c4df768691dbfa61777a1c061b7e9847b37d6420f00000000001600141978176eaf2832b4fdf3d4a5fddd93262844fbc6d6420f000000000016001419c1822b2c2d6bda235ab059105521abe0b17c41d6420f00000000001600141a4f5855d874fb6553264a605f86692b00e0d4fad6420f00000000001600141ad0e7fcdaf51ad0e132681a2535ece25707ebbad6420f00000000001600141b089eea09b5463d8ccae857772faa73828f91a8d6420f00000000001600141b3a963b9857275ef8a1cb557484213209108029d6420f00000000001600141b3fbba986089073d95c6a2fc90acf60cd282e2bd6420f00000000001600141beac2ed90b0ba744ceb15214d543a380218f0b9d6420f00000000001600141c31f0c2f586469bc288c1af8827d0342c960eecd6420f00000000001600141cc951d1849a576e7d4bb9a3416082b9f078f46ed6420f00000000001600141ccc66f917865aba035c2a1472e3b22ec6b13374d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f00000000001600141e19ce4c45b4b25d65b44aaead6a59d7fcca2c1fd6420f00000000001600141f03fe863f76438ea952d75642b0291fff8b6565d6420f00000000001600141fb139333a9534ee3244131db1b8a776365a5f39d6420f00000000001600141fee1832ba1fac73d6bfa252e683568196abc824d6420f000000000016001420a9940b2b90cda2b3c47ba526cbb7726e18ac3ed6420f000000000016001420da1abda62e9e9c287747fd3d9d715141273ebcd6420f000000000016001421e03ec45c60302959324bc8610102c6dcc5e688d6420f000000000016001421ef45e02d56086669496aeb0cea8e7e9e6925f4d6420f000000000016001422956af6fc9f4226f23fd7feaed8193c55ac45d0d6420f000000000016001422f06e3be4593e7725da56819a8978429394ba0ad6420f0000000000160014238e0ea04a88cae5cefd06220cc069915e2b4062d6420f0000000000160014239cde8613defb93c97d5a87e0649a591a004ef5d6420f000000000016001423a8abe034ce6e86c05d500c7eaeab4af1b9a620d6420f000000000016001423df789f19c19ced5771662620ecb12f4975c018d6420f0000000000160014248cb9a55d8ecc205f8fd6fbbb109d0aa6a0be5bd6420f000000000016001424b5d69ed0f02014641d728e75686b29f02a4c6ad6420f000000000016001424c8d8007a06b93107efeab5e1b6a2e37041da63d6420f000000000016001425261cc320f4a4caf71218d7c04c801ce0e79733d6420f0000000000160014253b1d2420d3d94a5c34e89e04932fcfffcf7d2dd6420f0000000000160014253de50796969081262e284d8b018a45da4fec05d6420f00000000001600142540e8d450b7114a8b0b429709508735b4b1bbfbd6420f0000000000160014254ef906293d6c573a70a08d140be40d06cbcabdd6420f0000000000160014262db5d0e67310eed49779d3e2dfadfa0685ac9ad6420f0000000000160014273f4335487889dc77d3473ec1dd5fdd0189867cd6420f0000000000160014279e5ea43996334d160db11e63d77e919dc9f736d6420f000000000016001427d18f9980fd2fe6c9bd650e49c37b16e6f802f0d6420f000000000016001428500d89fd946735b19f752133840133fc722a32d6420f000000000016001428ef67f91474a0df75540ae7ba533b15fcfda7ded6420f000000000016001428f39b6cbe20f72680255a8fea6eaa2a57886232d6420f000000000016001429d3c169a33204cdfafdab1b49430d8d327cd263d6420f000000000016001429e4c1f4276bd0bcd30d55898e271640d1b6a045d6420f00000000001600142a30b5f7933406b358e34d5c3cf54153b340767ed6420f00000000001600142adb610452aae3281a82cad88d553ae418c12763d6420f00000000001600142b063d2b6519af615d5b7df625ebb3461ac11f9ed6420f00000000001600142bf54ab10617c09c9a0465cacb5825718777ffffd6420f00000000001600142c814deaac12fb4b2e805f2733e3412e7f75922bd6420f00000000001600142cc1be6804f613453e50ad6b3b01cb9d586c7788d6420f00000000001600142d2f6f87211261a687098a756dbc9e7c7bf70911d6420f00000000001600142dabd038c1c600be40103b5ffc113168edae33f8d6420f00000000001600142de4d3f2e208ff3df585638b485c11a03af1840ad6420f00000000001600142e4e7f35bdde2e17f50b2dd926b9bf7b0e2a781ad6420f00000000001600142e76357e0b0249c7198b4318b2e0d45a50a2b8d7d6420f00000000001600142f74b74f7d736eb16fd364747aabf2e60f12466fd6420f00000000001600143025f64d9ff77886cc322bb24e23d18aa5f5b7acd6420f0000000000160014302a481388b4fad3a75741daa9c36ec69b4818abd6420f000000000016001430e3b9536e99a934901657bc2fa4d2773cfdfda4d6420f000000000016001431403af0268ebc4f8079ebda620ec9ac427ed537d6420f0000000000160014314e8149b9f38f80fa984525fdbd9eeb086556abd6420f000000000016001431e40feea85d07d7a0cdd011ed457636bd2d5e17d6420f0000000000160014322024aa771e4d7090e6fa32ca593bb5906fb702d6420f000000000016001432311a07434479e6fbac8acd082d3da25a038ee9d6420f000000000016001432a6965eab2e8bf5982c9dbd0760a39b769e9a88d6420f000000000016001432a98b42d4677c7221016235c89a751bb297232ad6420f000000000016001432aeeb4a59084b7c08fec753ab62854504794035d6420f000000000016001432c4d9744fe5c9554393b7981449106f15a3392dd6420f000000000016001433a8ff2bf09535ead6e24a2d9ca69cc2f19075c0d6420f0000000000160014345963e3b1a771315852bd99b0ee5b263c3e8902d6420f00000000001600143496dd6707c3f2857bf76b99c397e7f56e5a05b1d6420f000000000016001434f1fa732c19d82423603eb72dbd746455d5ab75d6420f00000000001600143521baf95b2ca19ea964123dc82141eb07b29c42d6420f0000000000160014354693b310aeccbe3d95c0e3315232641f33fefbd6420f000000000016001435a6eae9538653ad88180ff012fe71c33dc668eed6420f00000000001600143617a2707bbcb90d5d5c3755ccd7345850d62497d6420f0000000000160014364c3f69cf775a3c2cb9c61d504e8c0c16ae92abd6420f000000000016001436620c60f1a82410d52455964fa79b0cfc3539c2d6420f000000000016001436864ff9dfe343251ed46edce37559892b4b358bd6420f000000000016001436f3a9d6b31a2d651efd32f81a6004688abfa280d6420f0000000000160014372e2211f7c3621f1a2b348905445a0eb38c6b55d6420f000000000016001437cc33cd5a16d1280ed2607dbdbdc527be3fe8edd6420f00000000001600143937d925c3392155accd534ee5c0c6ffae94d4b9d6420f00000000001600143a05a5ab71c5da9d3d1b4816d1ea38f002fa341dd6420f00000000001600143aee60782b5d35718dfcbcef688ec33500f8266cd6420f00000000001600143b93669c94f89bf74fce9f0117a8bd0bc44b43efd6420f00000000001600143d48125b6e51bdf8803d10d10250c34f5867dd51d6420f00000000001600143d71df867e69415eb2d3f076b5731e37bdda28a3d6420f00000000001600143d77f2e4fe5ed04eb7ef357111ad54f1d3ff7c1dd6420f00000000001600143e5c1967957779e31618ab35f5eb72bac24159c0d6420f00000000001600143f6146598292db54bc98cb37c54ef9aa5f2b5c81d6420f0000000000160014400671cef1c3127a6ee6db9dc68f493eb12cc753d6420f0000000000160014403eda9f4d5cfc6a2a743e7e47a4040fd29d51ddd6420f0000000000160014405080f3c07878f2abd4ccb6f63c9b9dbd3836fbd6420f00000000001600144058a362bea7fe6d361e4e293ad5924676aabb7cd6420f00000000001600144064d5c8aee1b28e7186ca6e6bd9a7f614306752d6420f00000000001600144080941e1bf97db1ee794e565560360f1e78c529d6420f0000000000160014408f78c45c889b9d5fdaa120567379599480c57cd6420f00000000001600144166669293e4450734af2b744b76882ae1022f30d6420f000000000016001441eda3bd7b764345efbbb3057b87b2d9c7eff837d6420f00000000001600144228608561891f413627b90b009c08cb80da740dd6420f00000000001600144285f78aa961c7edf27d0d3d2044b446788675d2d6420f00000000001600144298b0b8b01ae47dc3f653f4c814d34b70e73523d6420f000000000016001442ac3c3b0ccb3ce5fa5fb15030debd64de6f18f5d6420f000000000016001442f5a3ef0a84c35f9d298c0dda65e13b042ed3afd6420f00000000001600144333425f710e67e4375ee38c1deb6cd39051b1acd6420f000000000016001443da7c781fae1761e9fc406cf4b0dc44737ec68ad6420f000000000016001444989a2b0199ac8dc6820b4a5479e5e974aba211d6420f000000000016001446607ecdc585d7f65ef24a77cedb639dd2c9fd3ed6420f0000000000160014469d8b67848c7391b69d5f09ee15fc7d87770d07d6420f000000000016001446e2a35e6ffd64b34303e0a534d6bdaec0284f4cd6420f000000000016001446f96a653d4230a66833ad1a1adeda033b0736fed6420f000000000016001446f9e9437557eec08f862ebea2a70f0c648d03efd6420f0000000000160014474392b1a70c12525969836b843235adab29d07bd6420f000000000016001447941c4c2a9a5219c1ea52bb21f6cc5aef6ea0f7d6420f00000000001600144a068b6084db2f29a63537c076e1420d677009dcd6420f00000000001600144b2a74489e76d749f466cfc55aba8684ca030db3d6420f00000000001600144c5583b123e2e7ef85ff89285e4df8b7d26439cad6420f00000000001600144e2d967a45a2108415d4fa50ecb77576f17a8a15d6420f00000000001600144ea4eecb73078af2a24e0001c16c7e3e3733660cd6420f00000000001600144f15476c4a1399dfb44f47834ed569f91800934ad6420f00000000001600144fd10d105d436f911740766917f6c3a993af59e9d6420f00000000001600144ff5dca46acfa9f6a62d1e60bd6ea6194252c30bd6420f000000000016001450360db5c57c66c14cb4c1a325a3a95f2f657115d6420f000000000016001451e06138f2c59e9944cc7cb7dd7bd422041d187bd6420f0000000000160014523d0ab138a0c0173c19e0b66eca74a8bdfc7ec5d6420f00000000001600145292a3091ab6c5b1d970adb83769a5dc104afa1fd6420f000000000016001453346ca2e9d554c276f395b8c7d6905babf7ae42d6420f0000000000160014536f83a52442ce39e7d2c463d732d0854b5921b2d6420f000000000016001453f683305e9d20369d44400e2a0ee4d4900f787dd6420f0000000000160014546b7c78304b0f42c671fa0cd2787c41902508b9d6420f000000000016001454abb4ebc46cbf3fc3774207d8fc4d15eb123f6fd6420f00000000001600145529e5c87af0729756027f5e8ba9dbea3c83e81cd6420f000000000016001456620dabe7ae11c4d00df46d70fa0be1a1bff40bd6420f00000000001600145717b4959e33f38ff14f7c6ddf217bd682d75980d6420f000000000016001457a745f746c54bbba31a50e5e27f6c77ee3d7c4ed6420f000000000016001457b71acda683d7db1fb8161a9f42e93cfbd4a0c3d6420f000000000016001457efb91172d797c280cd682e039b3c282e230c7fd6420f000000000016001458213bd1fc523a00c18da2d08b6441f88d9933e2d6420f00000000001600145826d2320de69333b9826a63593322b39f17cf3ed6420f0000000000160014585e84212f4e8ab85febcb04c42b25f9b2e5abd8d6420f000000000016001458be34efb6a736ee543df07b1ba39795faf94073d6420f0000000000160014592395acf4f50e4099fcabee7a4344022279b0ded6420f000000000016001459705cf8073519c2e28a4f795f88ad3b25e91d4fd6420f00000000001600145a3a0aa5a583290b7e967e9f6610d36bdb94bc85d6420f00000000001600145a759647e6dfc4a8464b9a84cbc3d765289c1c19d6420f00000000001600145ad28b9e16ec61f87a9c46ebb60b7bdd7747cba8d6420f00000000001600145b073b2257ae7022c40d2c7e57797b7a0242f00cd6420f00000000001600145b1cdb2e6ae13f98034b84957d9e0975ad7e6da5d6420f00000000001600145b72ba84d6cae608ad378474868ff59e75606db4d6420f00000000001600145bfa07b8250d3748a19a052e460edf43d8a7b5e3d6420f00000000001600145c0e86994fb9632bd5aa66e8b2ee2748202dd031d6420f00000000001600145c336ba5c774f31da50f5659cc623dd7bd65e54dd6420f00000000001600145cad5ebedfc95315586d57f156c825322185daf8d6420f00000000001600145d18658c736572ac40c749df69b0928e9ce0858fd6420f00000000001600145deeffcb3b2ff79f6f5270197db8631844c52593d6420f00000000001600145e4722e9ee07e001f3183a673332fe8daee0bc0bd6420f00000000001600145e60ab61105906c50effe846f21410a4a5cb2b26d6420f00000000001600145f3e0ec53904eb22c34fe52797bc9bd56f4608d2d6420f0000000000160014602feb7910d5c5e2afea07a992cad2a0375dcd14d6420f000000000016001460426c77a8dbf4f2eb43e256f0cb31f61aca0e7fd6420f00000000001600146081e8072529abc280639f74b79723acefc265abd6420f000000000016001460bc4f98e90c4b152ae2815895693df892d23b49d6420f000000000016001460ece2a88b134abb937500ac07b0ce4696c967e8d6420f000000000016001461b350784c7a76a0d54fe98e9aa7ddd417a176dad6420f000000000016001462422c58330b586c63e398d95eb980b45a48a5d5d6420f0000000000160014628019c29566324382aafc00aead15170aacac96d6420f0000000000160014635544736286b6ce83b0d1dcead802798e8793c3d6420f0000000000160014639eaaab3ea04a5d1548f085971b91513bba279cd6420f0000000000160014653b3a212f2c3b490a12e48e065e776fec4d3334d6420f0000000000160014653fefd4363b2573992a194eeaae09cd52c10effd6420f00000000001600146580d852d5ee0a8a3e4f27be58913ceb57993db2d6420f0000000000160014669eac0054254106ba1b05b955071c7d95ef5349d6420f000000000016001467127ef765480661ca7db3dbb5bdd4fea2f1eaf3d6420f0000000000160014672fcd8d03ace1e58f8e6afe4870032937f1609ad6420f00000000001600146824a0c62b5eefd3e5bde4e875f96c4dff1ad444d6420f000000000016001468c2398ffc025844574f24e85373f3f21cde46ecd6420f000000000016001468e4df1547c14df2da3111d2e11da03fb0ccd2fbd6420f000000000016001469079f0d11766ecb8121d7eb71e4e04923498aefd6420f00000000001600146940f1e16bfeb0d3c76172ef1ea1fdbc570f83a5d6420f00000000001600146a011a3918105b28fd7cc6178ae9c7b1330e0009d6420f00000000001600146a829dbbeaada3b974b28836ca51fde016afcbd6d6420f00000000001600146aa2a2accd342cb7d3715bafb28f42e39c8c8c75d6420f00000000001600146b1d67ca705ffe4946baf3bbaa22cc4e9111b716d6420f00000000001600146c72404c8191b1f80d2cb7e635c047afc60404eed6420f00000000001600146d301830f8e0c175d8570dff895248590ae22556d6420f00000000001600146d581f948dbf02d0807127e53b221dd20d067ff0d6420f00000000001600146e197f49b9ab810011159c0a6afc79e828850f5dd6420f00000000001600146e7bc41702e356452168f83ee8c230be4ce5499dd6420f00000000001600146e8321625944676d744d017a4a2d12aad23f8330d6420f00000000001600146eab9cf15ae4e8e2770d24f4b03b70f46a8316c6d6420f00000000001600146ed65a10b885fa58d6fefe8d36a61f93f88a050dd6420f00000000001600146f522023ab31d953929d0a6b44dbcd0b3f0d43b7d6420f00000000001600146f8efe2d2ee1df6d43b529107a97b1990ecf5e23d6420f00000000001600146ff7d0ad04161f3dcb172c29f9804eaa56f8dd7cd6420f0000000000160014703d05e67245de3a3c9b0d6f830aaab45cd8367ad6420f000000000016001470990b926ef75c438adc8f59b5e84d7788459b01d6420f000000000016001470edc682bcdedc14bab8400ba87005d4ec27c9d4d6420f000000000016001470f425191b1b90381ce96b663255fea2c5891979d6420f0000000000160014716918589d623df19406d6a23cecee9217dc48ccd6420f000000000016001471edbd16db6c0544747202c785d3ac887ad0f8e4d6420f000000000016001472c72aee5367aa6254c085d7bf744fb2709715aad6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147336b9ac8626c93da06531fafc0db4dab00de2e1d6420f000000000016001473cd2fdfc0b5f8c637669cac11322d520182bea8d6420f000000000016001473dac72c581809f20123fbbcf0caae3cb60f6c94d6420f000000000016001473e9deabc39a0aca1b09ab983e25646074457a52d6420f0000000000160014744d132cbb0737715b387953f4c0781c116d1416d6420f0000000000160014750293fc6221a5761dd8640221f111725b4cd721d6420f0000000000160014752b0753dfdf8c38ad0f5692579a00702c9757e1d6420f000000000016001475b3eb883427e2897e63684634dbfbf4ef044b41d6420f000000000016001475cf6fa98c3c51ed031c0a4a2941722b33d11961d6420f000000000016001475e27b8f63e57873e15f29a4d078c0553b34aac9d6420f00000000001600147853723ef29696c8623147a097ed96106681d9f7d6420f0000000000160014794af22c1d8b45f180b382de67a104b71edf5b5dd6420f000000000016001479c27ecfba5543186e7d0ea86ed5d97914a2f9d8d6420f00000000001600147a05b67f6ca96d0a38050b8b88c5f19606923387d6420f00000000001600147a20f970b48ef74d9cb6c87177b4885b859c385ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f00000000001600147b997c4788aa2cbd4bbb63e4822c807cf2fb373dd6420f00000000001600147c21a3160c1298f74b8b79426a583452c6190447d6420f00000000001600147c574187130fbfce3fb6fe87455c19861c8bed44d6420f00000000001600147cb5596ef4c4e9b0baac1729256f3b75fdbad381d6420f00000000001600147cd81ca2c9b743afa9961cce1e153578c5efe3c1d6420f00000000001600147cdb6e037ee836592dfdfb12dfa9b6454504c21fd6420f00000000001600147d939ef2b66fa055a57e9ec48b2aa9762a49cf2ed6420f00000000001600147dcea1b30d092021c15bb75ee58ed6476581372ad6420f00000000001600147eb8093d3b99db2e16eb9cc91c45154486651037d6420f00000000001600147ebba905959df71228bee1af0309b9e95885efedd6420f00000000001600147ec48a2a6a1dd5e4c4724b943d9c7c0a81f13715d6420f000000000016001480d97088e26418deecd12333951adbb74f7143b8d6420f0000000000160014815bfa399c6b46dbca676beecfeb85a949597e82d6420f0000000000160014819e751e53064dd2b4e373a01d7b03959ac7fe60d6420f0000000000160014822372db51dcdc04fe2971db34e0c2c1cdebee7bd6420f0000000000160014824e2945c74ad0fc7f303add86d894b97d764d1ad6420f000000000016001482630ab97fce0532ac1e7d1c6474730e72f9d3d9d6420f0000000000160014833e54dd2bdc90a6d92aedbecef1ca9cdb24a4c4d6420f0000000000160014833f2813e3473a66f4db10f7166118521198be4bd6420f000000000016001484102ef791ff133a77dc591a5e3602591953a6a0d6420f0000000000160014847a9a4cf2d306aaadf1435bbcc782a988e2d103d6420f00000000001600148535df3b314d3191037e38c698ddb6bac83ba95ad6420f000000000016001485459944a1c09b35c3f9f9ea3939c631a04c965ed6420f000000000016001485ee95f4a3a4babc30d4933c739a615a6b32b5fad6420f0000000000160014862faf92e7772cc55f3701548493eeaa404b8ab1d6420f000000000016001486921f76120278284d704d03dabc3d393b85430bd6420f00000000001600148761e589e7d45ae8ddb930bc394338f395d83703d6420f000000000016001487cbb0d892a54812c973460a2709feb13607297bd6420f00000000001600148800a61bdd0c3e7bcec6f8a81785cd240426664fd6420f0000000000160014881ec04f30d860db1ab631e19b7f0b3ff6a2d373d6420f0000000000160014883221b2c5d466d2ca48fbd1a81b1d16ea61c301d6420f00000000001600148908e8c8a556887cc08d16cf52f4d49b2e6f777fd6420f0000000000160014894c72c1ad09034876cda0a2e0748afa4b6d4fc8d6420f0000000000160014896f271b8aae207761c5b8bc181c27dae9e1afddd6420f00000000001600148a1ee7db620414995b917efd115a5f19e1fa122ad6420f00000000001600148a3c436e280a61daf41bb65db9e7b7a15a1c1c29d6420f00000000001600148a543507d9922dafc07cf681e9e8672efa7c110dd6420f00000000001600148abd9c7bdba9c67976759c85325834fc6b4e22a5d6420f00000000001600148ae515f2dc15fb8ec56a84d141947a99de6a59fad6420f00000000001600148b06de1e3f8f961c863301166582dcffdc775f4ad6420f00000000001600148b2856700010fff72a64e0e7093b92eeed726bbfd6420f00000000001600148c2ae7a995a44084e5b98414358110ec1c8f6900d6420f00000000001600148cc8764f774a3f5767d098c8990d02f1e54c56bdd6420f00000000001600148d202f838e1a9853000d035775c2868617b37f36d6420f00000000001600148d242091fab14f486ec5ff43147e990fcf1dfc00d6420f00000000001600148d753e05021d86857c4f3f5b21aee53e1bd282f0d6420f00000000001600148f4b1489ae4fd91a1612bc37c8108f35d5ead858d6420f00000000001600148fc78fc009c64d1d434652378788b589bd918fb9d6420f0000000000160014907787bb54bda0e2f748ae303c4c9b7db0928b0cd6420f0000000000160014907e9f175800ceccedae6d79fdac6ba231db6cf2d6420f000000000016001490c6e0ad8fcb5b5f953c26ac6b2c0e5e907f2093d6420f000000000016001490e2235c04a16fb4ef924dbefbc92fee33270ea7d6420f0000000000160014924a8e48b305d06d6d6c8508694dacbc556fb22ad6420f00000000001600149262af2aecfc9b7c4df1aee2ab41036204f5e9bcd6420f000000000016001492643572e0cd2b64b29c751b229c12aaca7826cdd6420f0000000000160014937e1023b6957babb6d441a1e9ad2c49b3291575d6420f000000000016001493ba51e1a343c0a1167db979597990d78178ec19d6420f000000000016001493c9d5c6c67e27fdb158c5179a8ecdd0914e762fd6420f00000000001600149490d7c3be2b4e399d422055b4acc171505364fbd6420f0000000000160014949c1625aac7a964fe32ba7972a35ac2a70d63e0d6420f0000000000160014950ea7145c9aff9fd3ecd65a015c5b772a0a8393d6420f0000000000160014959a64e70829bd15166470c90f5df4cfc4aa62add6420f0000000000160014962821a18fa2e66ce5eb0d102d2d3331822a433cd6420f00000000001600149641508a1a8f80a989cbcf68521c1b526a2738c7d6420f0000000000160014966b66119872f67b300640fc5c321817955dd77fd6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f000000000016001496788d371adcf3ded7e3b9fb29cb14a496264c68d6420f000000000016001496b16e72352761eacaadc7bd7221f9914a902b3cd6420f000000000016001496f643c5066aacd9f56af6f216f21d5fd6eed333d6420f0000000000160014971f568bfb689ca8de45dc3d4f3bb5239ec0f5dcd6420f00000000001600149730f1d946da3cec3becf9b90c1fdf8fd232eb6ed6420f00000000001600149770ef1b05b0bbb2949fb237daad78dad2dcc1d7d6420f000000000016001498173b53f4561eb9476c1657b1e2eaf3d0f183d5d6420f0000000000160014983aca96e4411ca7e31b7feb54c4a1c809592ccad6420f0000000000160014988adf718900019d51d3d58a693d78f8416be2a4d6420f000000000016001499a4f9e271bd3bd8971c5ebe207b58b1b74319f5d6420f000000000016001499a51bd888f73af6a1dcbe01cc6e080f97b950f0d6420f00000000001600149a7a23328212d8f21d657c8bf0bfbd423c582622d6420f00000000001600149a81f3de38ad3daec19341e53d77345646fd9af9d6420f00000000001600149aa12ed5822ec951b254248193ee191a6f6e540ed6420f00000000001600149b6be0eb7235910c2f4be79d27c7555cd16b9059d6420f00000000001600149b7670da8aa8a193df9713d6a2e9ee19d5627e13d6420f00000000001600149c612179a2964f3c6e711bf1e1237476e75f8463d6420f00000000001600149c7f1582e00767d772e77edcd7d9d034b5353e70d6420f00000000001600149c8b0e55bec65d9e5f04117d7289b4aea0d5fcdbd6420f00000000001600149ce5b0cb964ac1aa11a0407f33c06e026a3fdf27d6420f00000000001600149d7995940370a1a31624308394292a974bebb99cd6420f00000000001600149d9c9a46ee6a755dbe327f1ff7cda1600edfed2cd6420f00000000001600149daec1be3f50e99774f884515a940d508d9cb220d6420f00000000001600149e501e54334705c2a00f788763bd2b294f8d533bd6420f00000000001600149ebdb1244c3d7b071ac7356a1f7a47a0b7096ad0d6420f00000000001600149ecfbbe9c2c7b60ee8dd2db4a6b910a8f57f1501d6420f00000000001600149f0a5a1fb0bd7623c2ac0fa997eaa108138ee104d6420f00000000001600149fdf61ef91ba89775d5dea5a15c9e71383eb23e8d6420f0000000000160014a0f7239dab89a0d829844346213a44778fce5d56d6420f0000000000160014a1ed87c1643fa4fd97fc0584cc4238050984706dd6420f0000000000160014a2247fd34e95ec6df8f113837934d1817900feccd6420f0000000000160014a25fd6959ad50fb689478477b56263f186f9e88cd6420f0000000000160014a2bd12c1be09f680c55a5a3391c7fe2f606c13b4d6420f0000000000160014a2d2a0ce6fda5337ae626124eb2aff3a3e62b4add6420f0000000000160014a2fa983f5cb84fbf151a17586fd57e9679180443d6420f0000000000160014a36f1c2ba419f5f61dd69509c39ad8f07925b6b9d6420f0000000000160014a41fe667ba0ebc409a0fb398f3e82be79a4275d1d6420f0000000000160014a426b2b419f2f2f403ead1eb5e74d171882e5a36d6420f0000000000160014a42db9573f9cc15f5d2b4fc04c392d9d73b93c4ad6420f0000000000160014a437c2400b0cac5c3b150a4566c87268e3c87c59d6420f0000000000160014a4f7212fe285d2c9b11a1fc14362ddad065769e7d6420f0000000000160014a51bb1eb3f3745ec8e48b63369d0adaecfe27428d6420f0000000000160014a569c59a28ff3fbaca0aa56cbdc594ff5b07c030d6420f0000000000160014a5771b25d8bfee94a03c0fc4d135971f685a15f0d6420f0000000000160014a678983fe5316dd98fcfb73feefe31d6ba0ff071d6420f0000000000160014a70f78884fbad1db6aafbc6ecc11ece5056f0054d6420f0000000000160014a820faa41563c6a17cbcb11e999f368b5725a5ead6420f0000000000160014a8542ed90def7015c45bf969ff9c36e1064eb3c1d6420f0000000000160014a8edbcfc43f86d15fec901a178848c46ed12683ad6420f0000000000160014a940d8af66b62db1bd6327cb696b320862063912d6420f0000000000160014a94bb2625322633651667f066558e8801f22234fd6420f0000000000160014a9eb97884488d534aad60a6e66712ce296ef3ea9d6420f0000000000160014aa533a13a3404ceefc6c7184e12f498a1945927bd6420f0000000000160014aa9b8be10543d9183cb7bbd26b644fa283a039d1d6420f0000000000160014ab98b48fd1adcb803d2a9ec94d2b018b0efbb757d6420f0000000000160014acd239a906ea8d3e4c188db22a2716ea571c25c0d6420f0000000000160014ad389e9aa1e09a06c4d214ae6c442029ee0cc87ed6420f0000000000160014adb93750e1ffcfcefc54c6be67bd3011878a5aa5d6420f0000000000160014ade55072356f2b40a66f28bc1321f60e2cbf1d9cd6420f0000000000160014ae255172e1292b10cddceed39dcc8c748d9dc3f3d6420f0000000000160014ae375fa53a6901e18cac04fb8744df668c5e5774d6420f0000000000160014ae56269b2bf8fe36811aa9dc5d454c3f5e687c06d6420f0000000000160014ae97253d4bcd3378af434a9fe58aa98d1458af8ad6420f0000000000160014ae9bdf63058bcbb82aa2c7fb8dbee860ff3e5a5dd6420f0000000000160014af869adeb77f56e1107f114b696e0e4736829567d6420f0000000000160014af903912608a3b93f72df89f51db3588850f2da5d6420f0000000000160014b21c13325c88e5357ad841885533c69628e8eae1d6420f0000000000160014b341b1572b19e70b4a68408528cfadf2ddfff0d3d6420f0000000000160014b39a130d600fb6b01108c8eeb45bc5ab59add6e2d6420f0000000000160014b3c3121a47cd5f620afe92bfd393c3581a55cc5ed6420f0000000000160014b3f95df0afd5a54e19bcc556d657d4f36418ffc4d6420f0000000000160014b4313aa56657a65d04e238a5bc5544c917d2207dd6420f0000000000160014b444e0ae4c9557a02ab455b8d0d471e7040988efd6420f0000000000160014b4c878dca1c29556ba62439c3ea63da265fdad1cd6420f0000000000160014b4d4040ba19c6f38e030cd9d30237e3ad2e91904d6420f0000000000160014b6cfee33782684423ea83c177b0a87d7885ee20cd6420f0000000000160014b740176415ee7df1e1452e4e35ab7f6699d20f8dd6420f0000000000160014b75a8e3985738c8a0d4fe3d753003b065af5f075d6420f0000000000160014b7664c9bbdaf6fea0adc0732c5c4e1b8bd03661fd6420f0000000000160014b7fdfe56ce1febaddfcbdb0393c674dfb060bd16d6420f0000000000160014b816305676693a83eece9961e818b37bf7da73ead6420f0000000000160014b8caf5f7c9f4338e8666a749e8c425236b5c3f06d6420f0000000000160014b9fb3f135f43dc6cd4015b7b81640cf90d626fa8d6420f0000000000160014ba58145c847ab0b87d8593b0f09beeb062afc67ed6420f0000000000160014baa5b097f8262c1cd4caea34c0df8f5ced816027d6420f0000000000160014babf23262614db37d6b86262d55c0497dcf9f062d6420f0000000000160014bad004e2d43221785ab4a5365040f02be9d410bcd6420f0000000000160014bb2ee1c3471ac340aae45623bb3b970871c84496d6420f0000000000160014bb3c2a4fbf688916aad9bb1131ec32e91ca7b2b8d6420f0000000000160014bb99d9bac2c8561de7378e10a2a81f8c791edb7dd6420f0000000000160014bbf14d192a175420cfaa7d2cfcde7b08767697b6d6420f0000000000160014bc9380e35e47e1c749b00d8f786c5c3114f667ebd6420f0000000000160014bd0317a29a0c0886fda4a89b9bc69cf8ce298976d6420f0000000000160014be1e41522659832c271a81ac266287f646b8c070d6420f0000000000160014be8e53072da8d3b4b45f1e9d660fe0e1131f7085d6420f0000000000160014beea44277242df81d042bb3bf82524d610f655add6420f0000000000160014bf4fefcece2d7838617631e110d53d5cd57dc92fd6420f0000000000160014bfb76d6d6480f47b67b3ff3759ddbb1272b87f65d6420f0000000000160014bff702958d538c67b506e43f2a9af88314edd79bd6420f0000000000160014c173a9811bc94a86414973454f747b400a04291dd6420f0000000000160014c18ca684ec2c3cc54bcfc8e4c84566f7c2df6f97d6420f0000000000160014c23e8f3980e5dddea8d16ac088f41b950123c1cbd6420f0000000000160014c24afdbe9712f8d08c9a09151aa9add384d08eb0d6420f0000000000160014c273854f7fde07135edce08299284c3ede4d1a16d6420f0000000000160014c33a425a0f0064ab20321a4483f0df12893eca71d6420f0000000000160014c35ef020d980aede4b453e65ca0c3dee019a5b4ad6420f0000000000160014c401f683af1ca29dbafac5779f33ada5096419bcd6420f0000000000160014c4b474eb50acc61497a0c6284148d0e21b990909d6420f0000000000160014c4c2ad4adcf8ac5f754a8c15bae7ff6847822eaad6420f0000000000160014c4c787d5f549c8a21a2b7544a581c7f215be3182d6420f0000000000160014c623c401059e26d9312ff7dce009aa642a3dba91d6420f0000000000160014c6d10fe2eca4abe33eeace5d81a67e9c2124d80ed6420f0000000000160014c7160a68b863e7584204c8f333d339ba8bf5e236d6420f0000000000160014c71e5f1063ebab194fd9a9311503db27d2852610d6420f0000000000160014c810f57f7c763ca15c63d1a82e9ddea0db8707a4d6420f0000000000160014c81fb2d9692a4e60b8a45b5e7340bed053a17970d6420f0000000000160014c88b16fb80458d24731ff1db9a2d08c8cb266d98d6420f0000000000160014c89fe071ef83a5d2fd1389a1524c6d67d8420509d6420f0000000000160014c910c487f32e1f1a23cc27542e70229cf38fd00bd6420f0000000000160014c95f8b35b77c9174b81a9f0090104d9a45c5254cd6420f0000000000160014c96e5a86b3535d9bbff3a10943333fca30540457d6420f0000000000160014c988a46b6039952c83ca4e901486cf6dcd456a0ad6420f0000000000160014ca66fccd9ff1f913adf121180b2524601755080cd6420f0000000000160014ca790d8b81366f39129ee5422d95b9426af001abd6420f0000000000160014cac07e3b658c2f263ca41e3d624532a55238915ad6420f0000000000160014caddb4ed988633ea3f87f11a5644ec9d3a4d5b6dd6420f0000000000160014cb32082874b8b6d27600d43db3bd548b908f69a5d6420f0000000000160014cb32aef7533881c59f723a722c5d67e3d1a01b27d6420f0000000000160014cc0361e502ab57edac2a21e20a5b72306fa5b592d6420f0000000000160014cc115901c8a1b5bffdb9c3349c81192940096664d6420f0000000000160014cc4e4e55c12dd8c1ddea16a51dbab6c2c43a6bbad6420f0000000000160014cd4427085369f484498a13ad20ebdf49fba4f148d6420f0000000000160014cd9777fa8ac7a9d24d9814b6525d0fcb3b943d63d6420f0000000000160014cde285e2a33ad32ea74138885b0a8c2e3e8b7b2ad6420f0000000000160014ce26f610509c3e2469cd2424a6f7995ebd9c3431d6420f0000000000160014cff5fd8f452d8096fc0a13115b147174c71d6ddbd6420f0000000000160014d040a6df3d85e96d3b9f8d3af49fc9938a664742d6420f0000000000160014d0575539fb8dd1d30351f2464b79c3525fbf002cd6420f0000000000160014d0e8ffe1097ca4a77028690ee165b193c51ea7f4d6420f0000000000160014d0f18cba83af66632f75402d4987f49301d77b48d6420f0000000000160014d1135c855206ee373b1b891d017bba1b0a6b7204d6420f0000000000160014d151ed69dbebdedf6750be416a40ef621b56acdad6420f0000000000160014d1f246637e6096ccb5e9e3fb08a3c0e07d062912d6420f0000000000160014d23ef45b3045c7d58b6154d9964c151672ea244ed6420f0000000000160014d34bc884b83e7c14cb430d3e775206a54e69247fd6420f0000000000160014d429edc5d8c3890152aaee6ea16bed8927a9604bd6420f0000000000160014d4569176129f3b80d65771d19f4874296dfa40a2d6420f0000000000160014d56703a557a98a11a8134e59ce5b9f1ef1fa12b1d6420f0000000000160014d62213403fe773d0f2117c83ef3505975ff22c3fd6420f0000000000160014d646baf96d352aedeab40724589a92d384696d19d6420f0000000000160014d676810230703bfec0e281d4ced9613fa96c3fcad6420f0000000000160014d6d2f3a87d4b1a1435e7987beea111909e82d85dd6420f0000000000160014d7317c8bcc88c5e07c0df944057d1a1e1dcf4140d6420f0000000000160014d742a19d1e7a202757dc6b587adc8156ba19526ed6420f0000000000160014d80ee4ae17e7304bad6dfff169005c716ebe412dd6420f0000000000160014d83111b54cbffa1b430829248df627ba42677e81d6420f0000000000160014d8428c25b6023f62f75600981a57363af96568ecd6420f0000000000160014d908f3ad66aff76078635f24f6a03b2a5761d0dad6420f0000000000160014d91e5378da1a95bf3685d18bac9ebe79a182d3b2d6420f0000000000160014d935bbb13eb63641cae5f127a0663414bf4dfba9d6420f0000000000160014d9c7410a5e615b2435fee0c895d2e4ab068268c2d6420f0000000000160014d9d251c122cdbd90888fa48df89aaf6a209945f6d6420f0000000000160014d9e36080a6f81bab96ddeeaf6087bb62f20465a5d6420f0000000000160014da53f69a0f96a90764f7ec4c7eba22ab49c87120d6420f0000000000160014da6b5536fe2412f38217c5c212be6ca8de5d4ad0d6420f0000000000160014da8975db225f82d66c07e5e8f116f85c66860951d6420f0000000000160014daf8e7896e1e76379a4f7f48b9b97a1fb5709b26d6420f0000000000160014db0a662bd80a5a8bff0adfbbb5bdbf557bb9f6fad6420f0000000000160014db2df85cb952e76057eee759bc1b729449c15b39d6420f0000000000160014dbce13ab139cbeb6402492f025e428c331db26d2d6420f0000000000160014dc20ee2db87c254d9d944e316f62a029d6f99f1dd6420f0000000000160014dda838ab4c75c3900f07da9429057d616c2d5645d6420f0000000000160014de0cd436f085b9bb60f5dfccf1a99413f5dbcf1fd6420f0000000000160014de9fb7cda37b6c19fc7f6d1ddce2c3649db3dfd6d6420f0000000000160014def20b6beee601ff2c4e6da931996d54faeb7ad8d6420f0000000000160014dfc93c13e7994152f9913a943d66f0c5a10526ebd6420f0000000000160014dfdbcf149e6dfc4eb225ce01957135217d2ceb38d6420f0000000000160014e05040a818ac5c163c73f1c8de1908dfa7ebf580d6420f0000000000160014e0b9cf9a3dd8f7eca7337049d5b1fb130c2048cbd6420f0000000000160014e0dbc981387bedd04df15eaa04885073fee26d5ed6420f0000000000160014e101c4a5ce3c377f5a8479f41a0d426356f46ab1d6420f0000000000160014e116d89a6aefc396128f0df76eba38069062bdd2d6420f0000000000160014e26d65ddd4f8b9a5a5edbbd98c9e14308fcc195cd6420f0000000000160014e27569f1fde10f3183941802628907a00e264fc2d6420f0000000000160014e3843f63db10847621261201398f1d8d160601a1d6420f0000000000160014e41024c64df6f2c11c459a700d40ac6b864c92cdd6420f0000000000160014e485601e393409d62141aab8e25de90460841160d6420f0000000000160014e4b2bb40650477ae00b354a068aeaaa520a2e394d6420f0000000000160014e4ef17a26e460520b26cd73241a4739f7beca3a3d6420f0000000000160014e59264008331ffbcffcd77dda55976acc6d324aed6420f0000000000160014e5c8dc922a88ade429bd5062db8095ebe7efecb0d6420f0000000000160014e631cea7cd978bd4cc067d1a2c60b9e8bf08c42fd6420f0000000000160014e7e68ed2440cbaf05e9d100addf1f7d62be8b7b6d6420f0000000000160014e8b92efd50fbbc90874eafa9b36b839fa0b15669d6420f0000000000160014e8cc65660d5f3d3624df16b33c72600b06fa292ed6420f0000000000160014e8e31467de2052f18616c6c1f719843b42f43adcd6420f0000000000160014e978e1fafd89428f27f75b9a943498d35b77c21fd6420f0000000000160014e9d65409e311e98c209e1af96a5335780fca0909d6420f0000000000160014e9e167f582781d4978968142fce76d933f16811bd6420f0000000000160014ea7f0c80100d858019c0492c6cae9795c6a8c53cd6420f0000000000160014ea8f773a21921eb4adf9845e32d9128078b3ee52d6420f0000000000160014eac556d40cb9c0c0ab26fdb04f4f2877b2dfb53ed6420f0000000000160014ead26d39d8a04d41956a552041aae9898c38b690d6420f0000000000160014eb277006f30cdcb06c8facef0e21d5c3b724e300d6420f0000000000160014ebcd6b5d52d27cbab8122fe3e2e8c96807f7228dd6420f0000000000160014ebd6d811893a9fbbc9b7b430747f4410934d14abd6420f0000000000160014ec3b40fc78983fcd811c88aea23a567bfc32fe67d6420f0000000000160014ec3d58c78241e7804eade9affe2a6af3792663f7d6420f0000000000160014ec638caa1151e3e30f876a51939e494e3de7196ad6420f0000000000160014ec6e0800a0a5f414b525eae6e8e63a2b9595a88cd6420f0000000000160014ec795d36ce08ce8ae3d2244bb989e0781b989528d6420f0000000000160014ecf8fcbd2dafadb8d31e834942d46dc4cd5b285bd6420f0000000000160014ed8a4ca21c7d3123ad0f15e6daba06cd5b95ec23d6420f0000000000160014ee7778b490617c6ad0589a8d52dd0c42bf03dc4ad6420f0000000000160014ee99777d004e0c171151964770d0d5c4dc1c2a64d6420f0000000000160014eec4ed3a52fff3903a7bfec0c34762b7d6992d32d6420f0000000000160014eef98b384bac5f7a2d74853a620d1fcbf0ba12ead6420f0000000000160014eefa5a5063acf0725e0e5e2ec5a99a56afa5aad0d6420f0000000000160014ef3476f8d501b1070717d2694b7118e836537acbd6420f0000000000160014f0da843e560365a7eee1c3309aab46668d0461bcd6420f0000000000160014f11b0fc3d9f8956e15ce5696fe7b92b4e1b6fc41d6420f0000000000160014f1a62d55bb79805a024ce4ce0eb3ea616dc3ffa1d6420f0000000000160014f1ac29ce39ade630ea02ccb07b0811eea65caafcd6420f0000000000160014f2271ef5a60da67e1cb1045d7b19cff791a1b4a4d6420f0000000000160014f2664714d7bec6f08b3c893bb52f4daf18ae8175d6420f0000000000160014f3a712d58a8549c09e6843d57257048c87011b70d6420f0000000000160014f3f0c87cee7a5d955765bd437b37f0e9c26ba8a9d6420f0000000000160014f4043d599a02c1ff59273e21d31bfd7d043ca0a4d6420f0000000000160014f418fa504f78d2c6efb26705ee9337497284cd00d6420f0000000000160014f467ca27ae0f8b4d4fa158d689a1b1fa8d0c0477d6420f0000000000160014f4d048354783d85f1b3f6e9a196cf06a749760c5d6420f0000000000160014f4d4a126d79120e15d3c7809763972318dc90192d6420f0000000000160014f5ce04f6b17be191faec28d0968e0baaa9d3d242d6420f0000000000160014f5d60d6ac38f7b90a30c999b5db714944b4203f5d6420f0000000000160014f63e980d81e3706812d64672dcb967bf6197abdcd6420f0000000000160014f8c64d20de88af1e9fb85f10086b30069a271302d6420f0000000000160014f8f1747128ba8a942dabcdc9a9fafcaca51cab4fd6420f0000000000160014f9181f8008bd8190eb464d899245a6ce2ecf6ff9d6420f0000000000160014f9bf807c7988050a161ad2db9ab180524de479f1d6420f0000000000160014fa4e11decb556495658fc02b562d39e4a707aa76d6420f0000000000160014fb1114539aa735ee9a4dd181fa6b564b848fdafed6420f0000000000160014fb48cc912c8d4cc2660642e0b31ac139687722dad6420f0000000000160014fca2b2d3a3d2c58e227036dc2b5c5957478aa38ed6420f0000000000160014fcaf4ed8d3cab498df9be7fec42fb8918968f6f4d6420f0000000000160014fcdaad7adadbf70ac5bfbb5a2a7964a41df0e33dd6420f0000000000160014fceca503378f76facd722a45e4fdb6799a24f372d6420f0000000000160014fda7ebf81c7975b99fe1155e24213759a78c7a37d6420f0000000000160014fe2e91b4f95d6da3e65d1bc6c3d3841430624573d6420f0000000000160014fe79ed014b4f7d497e06a8a3252bde8b093d37a8d6420f0000000000160014fe890970f13f20e0f11f936f9db30b6d7bab218ed6420f0000000000160014ff4105a5d4986dac321ed7ec79d33f422e602fb8d6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c81d6420f0000000000160014ff82af5e620782c08094e1d5813dfa2df17cde71d6420f0000000000160014ff92944730ae6eeb533be24e40399fa86f709b98d6420f0000000000160014ffadf1dc697a9d3633e1708c5f675a22fec0b1c05f1ce011000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac8702473044022032add7167414d02ff9b4ba18c9c8a81e26b517736d6f48c144f670c62bc7bbbb02202e6e2cddc9c9cec026dd886a1132615c11e510b435b25784033e27c99d75f0d801210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }*/

  /*@Test
  public void tx0_1premix_noChange() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    ECKey spendFromKey = bip84w.getAccountAt(0).getChain(0).getAddressAt(61).getECKey();
    TransactionOutPoint spendFromOutpoint =
        new TransactionOutPoint(
            params,
            1,
            Sha256Hash.wrap("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae"),
            Coin.valueOf(1010498)); // exact balance
    Bip84Wallet depositWallet =
        new Bip84Wallet(bip84w, 0, new MemoryIndexHandler(), new MemoryIndexHandler());
    Bip84Wallet premixWallet =
        new Bip84Wallet(
            bip84w, Integer.MAX_VALUE - 2, new MemoryIndexHandler(), new MemoryIndexHandler());
    int nbOutputsPreferred = 5;
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = new byte[] {1, 2};
    Tx0Data tx0Data =
        new Tx0Data(feePaymentCode, feePayload, "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym", 0);

    Tx0 tx0 =
        tx0Service.tx0(
            spendFromKey.getPrivKeyBytes(),
            spendFromOutpoint,
            depositWallet,
            premixWallet,
            feeSatPerByte,
            nbOutputsPreferred,
            premixValue,
            FEE_VALUE,
            tx0Data);

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 2, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "239f59f6ada2835bf34cd04eea2e81f0bacd924c5483233f25365c05e67ecd53", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff030000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d02483045022100815871dad73b7fdb6c8cf5aeec754e23e784170d45d1f1f3206a6a43773a9e87022031105b0e4faa319d46d2c7b9d546f128b19f1d027be11ea60f166d8856f43cef01210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }*/

  @Test
  public void tx0_1premix_withChange_scode_nofee() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    utxoKeyProvider.setKey(spendFromUtxo.computeOutpoint(params), address.getECKey());

    BipWallet depositWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet premixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet postmixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet badbankWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = new byte[] {1, 2};
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 11246;

    // SCODE 0% => deposit
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            tx0MinerFee,
            premixMinerFee,
            mixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(spendFromUtxo),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview,
            utxoKeyProvider);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assert.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "54e9521a79bc1c28c608bd55b5b50cfd375c53759151c03fe795dfe8d584e254", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402205725d685dff73b35bf517efcc828e838a461d4658862a3addd0557d89bf27a4802206e6c0382a5e9ee103c4180e683d2f14dcd32fe70ad8f07db205a3bd554bf8e5a01210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_fee() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change

    utxoKeyProvider.setKey(spendFromUtxo.computeOutpoint(params), address.getECKey());

    BipWallet depositWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet premixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet postmixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet badbankWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = new byte[] {1, 2};
    long feeValue = FEE_VALUE / 2;
    long feeChange = 0;
    int feeDiscountPercent = 50;
    long changeValue = 16246;

    // SCODE 50% => samouraiFee
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(spendFromUtxo),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview,
            utxoKeyProvider);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assert.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "937ddf45822997a49e8844cce1dcd3cceff14d25c0f51cae99b2383a692ae6b0", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f68591788130000000000001600149747d7abc760e033a19d477d2091582f76b4308b763f000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d02483045022100eeaf7c07dd2bd9c81b1b9944fa5410f71425fdca1b06a5f619a913fec61c5384022047f77046ff978d7e885c0793911fed4f2a38a519c8808926541c3ada7088123401210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_noScode() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    utxoKeyProvider.setKey(spendFromUtxo.computeOutpoint(params), address.getECKey());

    BipWallet depositWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet premixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet postmixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet badbankWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    Tx0Config tx0Config = new Tx0Config();
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 11246;

    // no SCODE => samouraiFee
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            null,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(spendFromUtxo),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview,
            utxoKeyProvider);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assert.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "958c28db07b16091d8f6e69209159fc3a8098485c8900e6d40880a14266a8ad3", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9ab17f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402206d35d9aff2b1a637ec31fbef5aeceb7b384101a4fb9eb975e5eab3d7461f8d9f02205ad4fef2b2a552bde4d2fda4274344a2946bfeab4daef23eb61fcc3ef6204ab701210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChangePostmix_noScode() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    HD_Address address = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    utxoKeyProvider.setKey(spendFromUtxo.computeOutpoint(params), address.getECKey());

    BipWallet depositWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet premixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.PREMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet postmixWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    BipWallet badbankWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.BADBANK,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
    Tx0Config tx0Config = new Tx0Config().setChangeWallet(WhirlpoolAccount.POSTMIX);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 11246;

    // no SCODE => samouraiFee
    Tx0Data tx0Data =
        new Tx0Data(
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            null,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            0);

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 =
        tx0Service.tx0(
            Lists.of(spendFromUtxo),
            depositWallet,
            premixWallet,
            postmixWallet,
            badbankWallet,
            tx0Config,
            tx0Preview,
            utxoKeyProvider);

    assertEquals(tx0Preview, tx0);
    Assert.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assert.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assert.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assert.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assert.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assert.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assert.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assert.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assert.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assert.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = new String(Hex.encode(tx.bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "1a1ff49a285a4b2131e7155e25341d575e7e6e9278c5f00bbc90dec362412334", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9ab17f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b000000000000160014d49377882fdc939d951aa51a3c0ad6dd4a152e26d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402204e37d89e31eb2242049605dabc803579c717f41eea9e53e7a460e8ac7a3806800220460816a471b9dd9cae5b937368da68166d7b2d28a946a01bc1d6317018e3063801210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void computeChangeValues() {
    final int MIN_VALUE = config.getTx0FakeOutputMinValue();
    // FAKE_OUTPUT_VALUE_MIN - 10 => 1 output
    doComputeChangeValues(MIN_VALUE - 10, 1);

    // FAKE_OUTPUT_VALUE_MIN => 1 output
    doComputeChangeValues(MIN_VALUE, 1);

    // FAKE_OUTPUT_VALUE_MIN*2 => 1 output
    doComputeChangeValues(MIN_VALUE * 2, 1);

    // FAKE_OUTPUT_VALUE_MIN*2+1 => 2 outputs
    doComputeChangeValues(MIN_VALUE * 2 + 1, 2);

    // more => 2 outputs
    doComputeChangeValues(MIN_VALUE * 2 + 10000, 2);
    doComputeChangeValues(MIN_VALUE * 2 + 123456789, 2);
  }

  private void doComputeChangeValues(long changeValueTotal, int nbOutputsExpected) {
    final int ITERS = 50;
    for (int i = 0; i < ITERS; i++) {
      // verify nb outputs
      long[] changeValues = tx0Service.computeChangeValues(changeValueTotal, true);
      Assert.assertEquals(nbOutputsExpected, changeValues.length);

      // verify sum
      long sum = 0;
      for (long value : changeValues) {
        if (changeValues.length > 1) {
          Assert.assertTrue(value >= config.getTx0FakeOutputMinValue());
        }
        sum += value;
      }
      log.debug("changeValues: " + changeValueTotal + " => " + Arrays.toString(changeValues));
      Assert.assertEquals(sum, changeValueTotal);
    }
  }
}
