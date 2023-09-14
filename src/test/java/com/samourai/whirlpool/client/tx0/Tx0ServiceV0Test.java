package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
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
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            500000000,
            address);
    mockUtxos(spendFromUtxo);

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
        new Tx0PreviewConfig(Arrays.asList(pool01btc), Tx0FeeTarget.MIN, Tx0FeeTarget.MIN);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(Arrays.asList(pool01btc), Tx0FeeTarget.MIN, Tx0FeeTarget.MIN);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(1000170, tx0Preview.getPremixValue());

    // overspend too low => min
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 1L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Param.getPremixValue());
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(pool01btc.getMustMixBalanceMin(), tx0Preview.getPremixValue());

    // overspend too high => max
    tx0Param = new Tx0Param(feeSatPerByte, feeSatPerByte, pool01btc, 999999999L);
    Assertions.assertEquals(pool01btc.getMustMixBalanceCap(), tx0Param.getPremixValue());
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());

    int TX0_SIZE = 2403;

    // feeTx0
    int feeTx0 = 1;
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(Arrays.asList(pool01btc), Tx0FeeTarget.MIN, Tx0FeeTarget.MIN);
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 5;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(TX0_SIZE * feeTx0, tx0Preview.getTx0MinerFee());

    // feeTx0
    feeTx0 = 50;
    tx0Param = new Tx0Param(feeTx0, feeSatPerByte, pool01btc, null);
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
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
    Assertions.assertEquals(1000170, tx0Param.getPremixValue());

    int TX0_SIZE = 2403;

    // feePremix
    int feePremix = 1;
    Tx0PreviewConfig tx0PreviewConfig =
        new Tx0PreviewConfig(Arrays.asList(pool01btc), Tx0FeeTarget.MIN, Tx0FeeTarget.MIN);
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    Tx0Preview tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1000170, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 5;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1000850, tx0Preview.getPremixValue());

    // feePremix
    feePremix = 20;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1003400, tx0Preview.getPremixValue());

    // feePremix max
    feePremix = 99999;
    tx0Param = new Tx0Param(feeSatPerByte, feePremix, pool01btc, null);
    tx0Preview =
        tx0PreviewService.tx0Preview(
            tx0PreviewConfig, tx0Param, tx0Data, Arrays.asList(spendFromUtxo));
    check(tx0Preview);
    Assertions.assertEquals(TX0_SIZE, tx0Preview.getTx0Size());
    Assertions.assertEquals(1009500, tx0Preview.getPremixValue());
  }

  @Test
  public void tx0_5premix_withChange_scode_noFee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    long spendBalance = 500000000;
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance,
            address);
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            poolSupplier.getPools(),
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
            nbOutputsExpected,
            Arrays.asList(changeValue));

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
        "02591d210a09103b1b5609d0d39c58d0168cede884390d7552ed2de85b79b3d2", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff0d0000000000000000426a405fb6a585292376a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f1027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f00000000001600142540e8d450b7114a8b0b429709508735b4b1bbfbd6420f00000000001600145b1cdb2e6ae13f98034b84957d9e0975ad7e6da5d6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f0000000000160014833e54dd2bdc90a6d92aedbecef1ca9cdb24a4c4d6420f00000000001600148535df3b314d3191037e38c698ddb6bac83ba95ad6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f0000000000160014adb93750e1ffcfcefc54c6be67bd3011878a5aa5d6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c8193a1341d000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac8702483045022100912788c91de581fea149d910c16b81c23ba634dc5b2a09f2efcf43e54fc3d1b1022049126f1380f44ddec73af3120b36d00e0c33c1ef86a7aac67f03e6e6af9ca3850121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_nofee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    long spendBalance = 1021397;
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            poolSupplier.getPools(),
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
            nbOutputsExpected,
            Arrays.asList(changeValue));
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
        "e08ee502f2a1da94653ea2de24cb281e3276cfa86680fb51e8e43397beed5c2a", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292276a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f1027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d02473044022055f59f0850d9d9011000c2709591b93922a183f17133f54255492493484e09ce02205295d87341d50d9cf5c393389a4bca8863e5e88d38f818188aa64a89d38fe9060121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_scode_fee() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    long spendBalance = 1021397;
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            poolSupplier.getPools(),
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
            nbOutputsExpected,
            Arrays.asList(changeValue));
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
        "66eb521509216c983e3df088ea830e9e2f0ba0a890a61b25be1eb3e5e7633407", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a7a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f88130000000000001600149747d7abc760e033a19d477d2091582f76b4308b763f000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d024730440220646a2206780645e9e8f2b522aa30ff7340d6f514a2c93448e1625f39fb3dd31b02203fff8840b630785884917b36cf7beac0e482518ca154d8b5b5d6a047f713d63d0121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChange_noScode() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    long spendBalance = 1021397;
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            poolSupplier.getPools(),
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
            nbOutputsExpected,
            Arrays.asList(changeValue));
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
        "a483c4b6437f7e7c8d4945f2eef184b358f6ebd5b53b49503bd9ad8015d0459f", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a5a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f1027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b0000000000001600141bd05eb7c9cb516fddd8187cecb2e0cb4e21ac87d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d024730440220757637b158dca7ed576b8322d1c1c76645cf8ef5f53027448cead022abf54bba02204b859964195b32f7663c73c614eee50a6096cc10b3d06523f44a40d0abee1b940121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }

  @Test
  public void tx0_1premix_withChangePostmix_noScode() throws Exception {
    HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
    long spendBalance = 1021397;
    UnspentOutput spendFromUtxo =
        newUnspentOutput(
            "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae",
            1,
            spendBalance,
            address); // balance with 11000 change
    mockUtxos(spendFromUtxo);

    Tx0Config tx0Config =
        new Tx0Config(
            poolSupplier.getPools(),
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
            nbOutputsExpected,
            Arrays.asList(changeValue));
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
        "512f42a1332b51ed164bb3428a9b1e584a2da2a484b50da4ce8e56069cca54ec", tx0Hash);
    Assertions.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a405fb6a585292376a5a386ec113f301b78e911a34e3bc4993ca098720eebae961afd4a0739fbd1f995190921fffe6c1c5ac395ab0ba979acb7f29d97ab86fd776f1027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b000000000000160014d49377882fdc939d951aa51a3c0ad6dd4a152e26d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d024730440220575ccc8e874a52e3f4291c20015f6ca12868bc305bbcf3cf0214c3c38b7f8ea7022059c83775118b8b137ca0b2036e724d168f385fe7f7729a87a075445ab2139cca0121032e56be09a66e8ef8bddcd5c79d3958a77ef10c964fd4808907debf285093466100000000",
        tx0Hex);
  }
}
