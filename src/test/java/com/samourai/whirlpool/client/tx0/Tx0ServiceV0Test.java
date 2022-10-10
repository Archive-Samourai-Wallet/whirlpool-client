package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Arrays;
import org.bitcoinj.core.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0ServiceV0Test extends AbstractTx0ServiceTest {
  private Logger log = LoggerFactory.getLogger(Tx0ServiceV0Test.class);

  public Tx0ServiceV0Test() throws Exception {
    super(64);
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @Override
  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(ServerApi serverApi) {
    WhirlpoolWalletConfig config = super.computeWhirlpoolWalletConfig(serverApi);
    config.setFeeOpReturnImplV0();
    return config;
  }

  @Test
  public void tx0Preview_scode_noFee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

    int nbOutputsExpected = 10;

    long premixValue = 1000175;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = encodeFeePayload(0, (short) 0, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 489987683;

    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");
    Tx0Param tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assertions.assertEquals(1000175, tx0Param.getPremixValue());
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(567, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());
  }

  @Test
  public void tx0Preview_overspend() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = encodeFeePayload(0, (short) 0, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    // no overspend
    Tx0Param tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assertions.assertEquals(1000175, tx0Param.getPremixValue());
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1000175, tx0Preview.getPremixValue());

    // overspend too low => min
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 1L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Param.getPremixValue());
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Preview.getPremixValue());

    // overspend too high => max
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 999999999L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Param.getPremixValue());
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Preview.getPremixValue());
  }

  @Test
  public void tx0Preview_feeTx0() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = encodeFeePayload(0, (short) 0, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");
    Tx0Param tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assertions.assertEquals(1000175, tx0Param.getPremixValue());

    int TX0_SIZE = 567;

    // feeTx0
    int feeTx0 = 1;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 5;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 50;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());
  }

  @Test
  public void tx0Preview_feePremix() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = encodeFeePayload(0, (short) 0, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;

    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");
    Tx0Param tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, null);
    Assertions.assertEquals(1000175, tx0Param.getPremixValue());

    int TX0_SIZE = 572;

    // feePremix
    int feePremix = 1;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1000175, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 5;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1000875, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 20;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1003500, tx0Preview.getPremixValue());

    // feePremix max
    feePremix = 99999;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1009500, tx0Preview.getPremixValue());
  }

  @Test
  public void tx0_5premix_withChange_scode_noFee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
    int nbOutputsExpected = 10;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = encodeFeePayload(0, (short) 2, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 489988499;
    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            12345,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);

    Tx0 tx0 = tx0(new UnspentOutput[] {spendFromUtxo}, tx0Config, tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assertions.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assertions.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assertions.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee + change

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = TxUtil.getInstance().getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "a55c2154aa023ed127692139a29ad01d50d4cc43b8d50f029a8867da36e6e0cf", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff0d0000000000000000426a409ae6649a7b1fc9a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f00000000001600142540e8d450b7114a8b0b429709508735b4b1bbfbd6420f00000000001600145b1cdb2e6ae13f98034b84957d9e0975ad7e6da5d6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f0000000000160014833e54dd2bdc90a6d92aedbecef1ca9cdb24a4c4d6420f00000000001600148535df3b314d3191037e38c698ddb6bac83ba95ad6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f0000000000160014adb93750e1ffcfcefc54c6be67bd3011878a5aa5d6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c8193a1341d000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87024830450221008433bfd64acfe66ac039d3488955720102c34fba6627884115bd674355a81fea0220200df4e895d7392cb0bad2cccd9465e1c5a26195cef0eb1b93c97d4f083ab6c501210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_nofee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = encodeFeePayload(1, (short) 2, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 11246;

    // SCODE 0% => deposit
    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            12345,
            tx0MinerFee,
            premixMinerFee,
            mixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 = tx0(new UnspentOutput[] {spendFromUtxo}, tx0Config, tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assertions.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assertions.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assertions.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = TxUtil.getInstance().getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "d1d42d8ffdc8f1cc93d2eb184acfb0c19c56ca501a4a2fa8753deaa1dfa8d751", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1ec9a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402204c4860f85d79ae2ae343209b04949025404f8b722107a845392075315b308f7a02203735f8efc3f98093bbf557a697505d3efb72102adcc2456166b2e6f0ea1c182d01210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_fee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 1;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    byte[] feePayload = encodeFeePayload(0, (short) 2, (short) 0);
    long feeValue = FEE_VALUE / 2;
    long feeChange = 0;
    int feeDiscountPercent = 50;
    long changeValue = 16246;

    // SCODE 50% => samouraiFee
    Tx0Data tx0Data =
        new Tx0Data(
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            feePayload,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            12345,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 = tx0(new UnspentOutput[] {spendFromUtxo}, tx0Config, tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assertions.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assertions.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assertions.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = TxUtil.getInstance().getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "8e9ca87bf78ebed2c67046cc8cd9cd034549e41de3c4b28098aba29e206af023", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f68591788130000000000001600149747d7abc760e033a19d477d2091582f76b4308b763f000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402202cc01cd5d92addd89a974ce5c24f54044332e6c91054074d86f0877017dbdc6f022021c0647c17ea5995b14e91dd0c31b6be847f7f458af61512694a9cf61858d40b01210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_noScode() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.DEPOSIT);
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
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            encodeFeePayload(0, (short) 0, (short) 0),
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            12345,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 = tx0(new UnspentOutput[] {spendFromUtxo}, tx0Config, tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assertions.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assertions.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assertions.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = TxUtil.getInstance().getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "958c28db07b16091d8f6e69209159fc3a8098485c8900e6d40880a14266a8ad3", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9ab17f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402206d35d9aff2b1a637ec31fbef5aeceb7b384101a4fb9eb975e5eab3d7461f8d9f02205ad4fef2b2a552bde4d2fda4274344a2946bfeab4daef23eb61fcc3ef6204ab701210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChangePostmix_noScode() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            1021397,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            tx0PreviewService,
            mockPoolSupplier().getPools(),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24,
            WhirlpoolAccount.POSTMIX);
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
            pool01btc.getPoolId(),
            feePaymentCode,
            feeValue,
            feeChange,
            feeDiscountPercent,
            "test",
            encodeFeePayload(0, (short) 0, (short) 0),
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym");

    Tx0Preview tx0Preview =
        new Tx0Preview(
            pool01btc,
            tx0Data,
            12345,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected);
    Tx0 tx0 = tx0(new UnspentOutput[] {spendFromUtxo}, tx0Config, tx0Preview);

    assertEquals(tx0Preview, tx0);
    Assertions.assertEquals(tx0MinerFee, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(premixMinerFee, tx0Preview.getPremixMinerFee());
    Assertions.assertEquals(mixMinerFee, tx0Preview.getMixMinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());

    Transaction tx = tx0.getTx();
    Assertions.assertEquals(
        nbOutputsExpected + 3, tx.getOutputs().size()); // opReturn + fee (no change)

    String tx0Hash = tx.getHashAsString();
    String tx0Hex = TxUtil.getInstance().getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "1a1ff49a285a4b2131e7155e25341d575e7e6e9278c5f00bbc90dec362412334", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9ab17f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b000000000000160014d49377882fdc939d951aa51a3c0ad6dd4a152e26d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402204e37d89e31eb2242049605dabc803579c717f41eea9e53e7a460e8ac7a3806800220460816a471b9dd9cae5b937368da68166d7b2d28a946a01bc1d6317018e3063801210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }
}
