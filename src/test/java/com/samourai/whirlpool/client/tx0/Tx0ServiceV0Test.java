package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoDetailImpl;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Arrays;
import org.bitcoinj.core.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0ServiceV0Test extends AbstractTx0ServiceV0Test {
  private Logger log = LoggerFactory.getLogger(Tx0ServiceV0Test.class);

  public Tx0ServiceV0Test() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void tx0Preview_scode_noFee() throws Exception {
    UtxoDetailImpl spendFromUtxo =
        new UtxoDetailImpl(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            null);

    int nbOutputsExpected = 70;

    long premixValue = 1000170;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    int feeSatPerByte = 1;
    byte[] feePayload = encodeFeePayload(0, (short) 0, (short) 0);
    long feeValue = 0;
    long feeChange = FEE_VALUE;
    int feeDiscountPercent = 100;
    long changeValue = 429975697;

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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(
            Tx0FeeTarget.MIN,
            Tx0FeeTarget.MIN,
            Arrays.asList(new UtxoDetailImpl[] {spendFromUtxo}));
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(2403, tx0Preview.getTx0MinerFee());
    Assertions.assertEquals(feeValue, tx0Preview.getFeeValue());
    Assertions.assertEquals(feeChange, tx0Preview.getFeeChange());
    Assertions.assertEquals(feeDiscountPercent, tx0Preview.getFeeDiscountPercent());
    Assertions.assertEquals(premixValue, tx0Preview.getPremixValue());
    Assertions.assertEquals(changeValue, tx0Preview.getChangeValue());
    Assertions.assertEquals(nbOutputsExpected, tx0Preview.getNbPremix());
  }

  @Test
  public void tx0Preview_overspend() throws Exception {
    UtxoDetailImpl spendFromUtxo =
        new UtxoDetailImpl(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            null);

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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(
            Tx0FeeTarget.MIN,
            Tx0FeeTarget.MIN,
            Arrays.asList(new UtxoDetailImpl[] {spendFromUtxo}));
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(1000170, tx0Preview.getPremixValue());

    // overspend too low => min
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 1L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Param.getPremixValue());
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Preview.getPremixValue());

    // overspend too high => max
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 999999999L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Param.getPremixValue());
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Preview.getPremixValue());
  }

  @Test
  public void tx0Preview_feeTx0() throws Exception {
    UtxoDetailImpl spendFromUtxo =
        new UtxoDetailImpl(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            null);

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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());

    int TX0_SIZE = 2403;

    // feeTx0
    int feeTx0 = 1;
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(
            Tx0FeeTarget.MIN,
            Tx0FeeTarget.MIN,
            Arrays.asList(new UtxoDetailImpl[] {spendFromUtxo}));
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 5;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 50;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());
  }

  @Test
  public void tx0Preview_feePremix() throws Exception {
    UtxoDetailImpl spendFromUtxo =
        new UtxoDetailImpl(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            "tb1qjara0278vrsr8gvaga7jpy2c9amtgvytr44xym",
            null);

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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());

    int TX0_SIZE = 2403;

    // feePremix
    int feePremix = 1;
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(
            Tx0FeeTarget.MIN,
            Tx0FeeTarget.MIN,
            Arrays.asList(new UtxoDetailImpl[] {spendFromUtxo}));
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1000170, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 5;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1000850, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 20;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1003400, tx0Preview.getPremixValue());

    // feePremix max
    feePremix = 99999;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview = tx0PreviewService.tx0PreviewSingle(tx0PreviewConfig, tx0Data, tx0Param).get();
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1009500, tx0Preview.getPremixValue());
  }

  @Test
  public void tx0_5premix_withChange_scode_noFee() throws Exception {
    long spendBalance = 500000246;
    BipUtxo spendFromUtxo =
        newUtxo(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, spendBalance);
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool01btc,
            Arrays.asList(new BipUtxo[] {spendFromUtxo}),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24);
    tx0Config.setTx0x2Decoy(false);
    int nbOutputsExpected = 10;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 247;
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
            spendBalance,
            526,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected,
            null);

    Tx0 tx0 = tx0(tx0Config, tx0Preview);

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
    String tx0Hex = txUtil.getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "02591d210a09103b1b5609d0d39c58d0168cede884390d7552ed2de85b79b3d2", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff0d0000000000000000426a405fb6a585292376a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f1027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f00000000001600142540e8d450b7114a8b0b429709508735b4b1bbfbd6420f00000000001600145b1cdb2e6ae13f98034b84957d9e0975ad7e6da5d6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f0000000000160014833e54dd2bdc90a6d92aedbecef1ca9cdb24a4c4d6420f00000000001600148535df3b314d3191037e38c698ddb6bac83ba95ad6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f0000000000160014adb93750e1ffcfcefc54c6be67bd3011878a5aa5d6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c8193a1341d000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87024730440220053fcf68d7d6291216c0245f183db2558dd1b00b83926859b53517d99ab7e75202205181548f6a7be3c144f8d240dd14241f8688892973588870d9acdde1487a757e0121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_nofee() throws Exception {
    long spendBalance = 1021643;
    BipUtxo spendFromUtxo =
        newUtxo(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool01btc,
            Arrays.asList(new BipUtxo[] {spendFromUtxo}),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24);
    tx0Config.setTx0x2Decoy(false);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 247;
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
            spendBalance,
            247,
            tx0MinerFee,
            premixMinerFee,
            mixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected,
            null);
    Tx0 tx0 = tx0(tx0Config, tx0Preview);

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
    String tx0Hex = txUtil.getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "70bd58e4ed64af067240278d00c265da68a6f4e8f6fb37196d19972d5aac43a7", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292276a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f10270000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87ee2b000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d02473044022072eeafd2e6ea296d74c0d80557e4256101c3fccf985ad2ff7f7ecd8830bfbf240220067315a60a0271df296c94772e63994871b598f5cd35cf631bb661ebf38569510121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_fee() throws Exception {
    long spendBalance = 1021643;
    BipUtxo spendFromUtxo =
        newUtxo(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool01btc,
            Arrays.asList(new BipUtxo[] {spendFromUtxo}),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24);
    tx0Config.setTx0x2Decoy(false);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 247;
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
            spendBalance,
            247,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected,
            null);
    Tx0 tx0 = tx0(tx0Config, tx0Preview);

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
    String tx0Hex = txUtil.getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "66eb521509216c983e3df088ea830e9e2f0ba0a890a61b25be1eb3e5e7633407", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f88130000000000001600149747d7abc760e033a19d477d2091582f76b4308b763f000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402200f3dc68fb4a87af59bcffd9b67a441e8922ee1fc7df776f96ee7064bcb1177ed02203d2c40bd9e65b65fa10805a32b71b5af147b09804b25352349ba17e28121f3090121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_noScode() throws Exception {
    long spendBalance = 1021643;
    BipUtxo spendFromUtxo =
        newUtxo(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool01btc,
            Arrays.asList(new BipUtxo[] {spendFromUtxo}),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24);
    tx0Config.setTx0x2Decoy(false);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 247;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    long feeValue = FEE_VALUE;
    long feeChange = 0;
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
            spendBalance,
            247,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected,
            null);
    Tx0 tx0 = tx0(tx0Config, tx0Preview);

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
    String tx0Hex = txUtil.getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "8e3244072db5f4ab2fb9d3b48f4ab5978bcea182c8bc95f29963aafa698e299d", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a5a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f10270000000000001600149747d7abc760e033a19d477d2091582f76b4308bee2b000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d024830450221008e73baef55909f28714c8725a76157453a66fb19b0f918096053615bc8087e06022055d1775546b380995123d64b2b076525841e8594caedc3532509c8a0431544090121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChangePostmix_noScode() throws Exception {
    long spendBalance = 1021643;
    BipUtxo spendFromUtxo =
        newUtxo(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool01btc,
            Arrays.asList(new BipUtxo[] {spendFromUtxo}),
            Tx0FeeTarget.BLOCKS_24,
            Tx0FeeTarget.BLOCKS_24);
    tx0Config.setChangeWallet(whirlpoolWallet.getWalletPostmix());
    tx0Config.setTx0x2Decoy(false);
    int nbOutputsExpected = 1;
    long premixValue = 1000150;
    String feePaymentCode =
        "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
    long tx0MinerFee = 247;
    long premixMinerFee = 150;
    long mixMinerFee = premixMinerFee * nbOutputsExpected;
    long feeValue = FEE_VALUE;
    long feeChange = 0;
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
            spendBalance,
            247,
            tx0MinerFee,
            mixMinerFee,
            premixMinerFee,
            1,
            1,
            premixValue,
            changeValue,
            nbOutputsExpected,
            null);
    Tx0 tx0 = tx0(tx0Config, tx0Preview);

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
    String tx0Hex = txUtil.getTxHex(tx);
    log.info(tx0.getTx().toString());
    Assertions.assertEquals(
        "3cc22bed8904c50a2f1907b5b0244ca390f73c9d2290fb0ee5f52ce8f6b62d51", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a5a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f10270000000000001600149747d7abc760e033a19d477d2091582f76b4308bee2b000000000000160014d49377882fdc939d951aa51a3c0ad6dd4a152e26d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402203d012ec090a7212717f75927028279543089a96aecfaf4ae560409cf32fee8c302203d74ea3d7dbfa7d4e96995edd09deadb2d174ca5dcc63441fd7e3f3c16a5f3ff0121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }
}
