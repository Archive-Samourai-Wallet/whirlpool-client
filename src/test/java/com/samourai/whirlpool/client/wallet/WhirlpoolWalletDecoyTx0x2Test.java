package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.tx0.AbstractTx0ServiceV1Test;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletDecoyTx0x2Test extends AbstractTx0ServiceV1Test {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0Test.class);

  public WhirlpoolWalletDecoyTx0x2Test() throws Exception {
    super();
  }

  @BeforeEach
  @Override
  public void setup() throws Exception {
    super.setup();
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2()} */
  @Test
  public void tx0x2_decoy() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 28, Arrays.asList(973304L, 975854L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);

    Assertions.assertEquals(30000000, decoyTx0x2.getTotalValue());
    Assertions.assertEquals(1000255, decoyTx0x2.getPremixValue());
    Assertions.assertEquals(255, decoyTx0x2.getPremixMinerFee());
    Assertions.assertEquals(1202, decoyTx0x2.getTx0MinerFee());
    Assertions.assertEquals(7140, decoyTx0x2.getMixMinerFee());
    Assertions.assertEquals(1, decoyTx0x2.getMixMinerFeePrice());
    Assertions.assertEquals(42500, decoyTx0x2.getFeeValue());
    Assertions.assertEquals(0, decoyTx0x2.getFeeChange());
    Assertions.assertEquals(0, decoyTx0x2.getFeeDiscountPercent());
    Assertions.assertEquals(1949158, decoyTx0x2.getChangeValue());
    Assertions.assertEquals(1202, decoyTx0x2.getTx().getFee().getValue());
    Assertions.assertEquals(28050842, decoyTx0x2.getSpendValue());
  }

  @Test
  public void tx0x2_decoy_SCODE_50PERCENT() throws Exception {
    mockTx0Datas_SCODE_50PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 28, Arrays.asList(983929L, 986479L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);

    Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
    long changeValue = decoyTx0x2.getChangeValue();
    long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
    long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
    long changeOutputsSum = changeValueA + changeValueB;
    Assertions.assertEquals(changeValue, changeOutputsSum);

    Assertions.assertEquals(30000000, decoyTx0x2.getTotalValue());
    Assertions.assertEquals(1000255, decoyTx0x2.getPremixValue());
    Assertions.assertEquals(255, decoyTx0x2.getPremixMinerFee());
    Assertions.assertEquals(1202, decoyTx0x2.getTx0MinerFee());
    Assertions.assertEquals(7140, decoyTx0x2.getMixMinerFee());
    Assertions.assertEquals(1, decoyTx0x2.getMixMinerFeePrice());
    Assertions.assertEquals(21250, decoyTx0x2.getFeeValue());
    Assertions.assertEquals(0, decoyTx0x2.getFeeChange());
    Assertions.assertEquals(50, decoyTx0x2.getFeeDiscountPercent());
    Assertions.assertEquals(1970408, decoyTx0x2.getChangeValue());
    Assertions.assertEquals(1202, decoyTx0x2.getTx().getFee().getValue());
    Assertions.assertEquals(28029592, decoyTx0x2.getSpendValue());
  }

  @Test
  public void tx0x2_decoy_SCODE_100PERCENT() throws Exception {
    mockTx0Datas_SCODE_100PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 28, Arrays.asList(973304L, 975854L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);

    Assertions.assertEquals(2, decoyTx0x2.getChangeOutputs().size());
    long changeValue = decoyTx0x2.getChangeValue();
    long changeValueA = decoyTx0x2.getChangeOutputs().get(0).getValue().value;
    long changeValueB = decoyTx0x2.getChangeOutputs().get(1).getValue().value;
    long changeOutputsSum = changeValueA + changeValueB;
    Assertions.assertEquals(changeValue, changeOutputsSum);

    Assertions.assertEquals(30000000, decoyTx0x2.getTotalValue());
    Assertions.assertEquals(1000255, decoyTx0x2.getPremixValue());
    Assertions.assertEquals(255, decoyTx0x2.getPremixMinerFee());
    Assertions.assertEquals(1202, decoyTx0x2.getTx0MinerFee());
    Assertions.assertEquals(7140, decoyTx0x2.getMixMinerFee());
    Assertions.assertEquals(1, decoyTx0x2.getMixMinerFeePrice());
    Assertions.assertEquals(0, decoyTx0x2.getFeeValue());
    Assertions.assertEquals(42500, decoyTx0x2.getFeeChange());
    Assertions.assertEquals(100, decoyTx0x2.getFeeDiscountPercent());
    Assertions.assertEquals(1949158, decoyTx0x2.getChangeValue());
    Assertions.assertEquals(1202, decoyTx0x2.getTx().getFee().getValue());
    Assertions.assertEquals(28050842, decoyTx0x2.getSpendValue());
  }

  @Test
  public void tx0x2_decoy1() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 2329991);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 3000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 4, Arrays.asList(978011L, 308002L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  @Test
  public void tx0x2_decoy_maxOutputsLimit() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 23299910);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 30000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    whirlpoolWalletConfig.setTx0MaxOutputs(2); // set max premixs

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 2, Arrays.asList(28978297L, 22278207L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  @Test
  public void tx0x2_decoy_samehash() throws Exception {
    // mock initial data
    BipUtxo utxo1 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 1000000);
    BipUtxo utxo2 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 2, 2000000);
    BipUtxo utxo3 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 1, 3000000);
    BipUtxo utxo4 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 4000000);
    List<BipUtxo> spendFroms = mockUtxos(utxo1, utxo2, utxo3, utxo4);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 8, Arrays.asList(976605L, 978135L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  @Test
  public void tx0x2_decoy_fail() throws Exception {
    log.info("Decoy tx0x2 failure due to not having required amount. Should create normal tx0.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 2329991);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 300000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set Decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(
        decoyTx0x2,
        "0.01btc",
        false,
        2,
        Arrays.asList(586617L)); // normal tx0, single change output
  }

  @Test
  public void tx0x2_decoy_fail_singleUTXO() throws Exception {
    log.info("Decoy tx0x2 failure due to only having 1 utxo. Should create normal tx0.");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 2329991);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, Arrays.asList(spendFromUtxo), Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set Decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(
        decoyTx0x2,
        "0.01btc",
        false,
        2,
        Arrays.asList(286686L)); // normal tx0, single change output
  }

  @Test
  public void tx0x2_decoy_fail_sameOutpoints() throws Exception {
    log.info(
        "Decoy tx0x2 failure due to utxos having same outpoints (used together in previous tx). Should create normal tx0.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(
        decoyTx0x2,
        "0.01btc",
        false,
        29,
        Arrays.asList(948904L)); // normal tx0, single change output
  }

  @Test
  public void tx0x2_decoy_cascade() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 2329991);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 3000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    tx0Config.setCascade(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(2, tx0Result.getList().size());
    List<Tx0> decoyTx0x2s = tx0Result.getList();

    // verify
    log.info("decoyTx0x2s = " + decoyTx0x2s);
    assertTx0Previews(
        decoyTx0x2s,
        Arrays.asList("0.01btc", "0.001btc"),
        Arrays.asList(true, true),
        Arrays.asList(4, 12));

    // 0.01 pool
    Tx0 tx0x2_pool01 = decoyTx0x2s.get(0);
    log.info("tx0_pool01 = " + tx0x2_pool01);
    assertTx0(tx0x2_pool01, "0.01btc", true, 4, Arrays.asList(978011L, 308002L));
    assertUtxosEquals(tx0x2_pool01.getSpendFroms(), spendFroms);

    // 0.001 pool
    Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
    log.info("tx0_pool001 = " + tx0x2_pool001);
    assertTx0(tx0x2_pool001, "0.001btc", true, 12, Arrays.asList(38623L, 38624L));
    assertUtxosEquals(tx0x2_pool001.getSpendFroms(), tx0x2_pool01.getCascadingChangeUtxos());
  }

  /** Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_pool001()} */
  @Test
  public void tx0x2_decoy_pool001_split() throws Exception {
    log.info("Testing Decoy Tx0x2 for pool 0.001; Change outputs split evenly.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 500000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 1000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.001btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.001btc", true, 13, Arrays.asList(95474L, 95475L)); // split
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  @Test
  public void tx0x2_decoy_pool001_no_split() throws Exception {
    log.info(
        "Testing Decoy Tx0x2 for pool 0.001; Change outputs not split evenly due to max outputs reached for an 'entity'.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 500000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 1500000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.001btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.001btc", true, 16, Arrays.asList(294025L, 96065L)); // no change split
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool01()} Change values
   * might differ slightly for lower pools due fake samourai fake "fee" back to self
   */
  @Test
  public void tx0x2_decoy_cascade_pool01() throws Exception {
    log.info("Testing Decoy Tx0x2s for pools 0.01 & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    tx0Config.setCascade(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    List<Tx0> decoyTx0x2s = tx0Result.getList();

    // verify
    log.info("decoyTx0x2s = " + decoyTx0x2s);
    assertTx0Previews(
        decoyTx0x2s,
        Arrays.asList("0.01btc", "0.001btc"),
        Arrays.asList(true, true),
        Arrays.asList(28, 18));

    // 0.01 pool
    Tx0 tx0x2_pool01 = decoyTx0x2s.get(0);
    log.info("tx0_pool01 = " + tx0x2_pool01);
    assertTx0(tx0x2_pool01, "0.01btc", true, 28, Arrays.asList(973304L, 975854L));
    assertUtxosEquals(tx0x2_pool01.getSpendFroms(), spendFroms);

    // 0.001 pool
    Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
    log.info("tx0_pool001 = " + tx0x2_pool001);
    assertTx0(tx0x2_pool001, "0.001btc", true, 18, Arrays.asList(69338L, 69338L)); // split
    assertUtxosEquals(tx0x2_pool001.getSpendFroms(), tx0x2_pool01.getCascadingChangeUtxos());
  }

  /**
   * Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool05()} Change values
   * might differ slightly for lower pools due fake samourai fake "fee" back to self
   */
  @Test
  public void tx0x2_decoy_cascade_pool05() throws Exception {
    log.info("Testing Decoy Tx0x2s for pools 0.05, 0.01, & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 10000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    tx0Config.setCascade(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    List<Tx0> decoyTx0x2s = tx0Result.getList();

    // verify
    log.info("decoyTx0x2s = " + decoyTx0x2s);
    assertTx0Previews(
        decoyTx0x2s,
        Arrays.asList("0.05btc", "0.01btc", "0.001btc"),
        Arrays.asList(true, true, true),
        Arrays.asList(4, 8, 16));

    // 0.05 pool
    Tx0 tx0x2_pool05 = decoyTx0x2s.get(0);
    log.info("tx0_pool05 = " + tx0x2_pool05);
    assertTx0(tx0x2_pool05, "0.05btc", true, 4, Arrays.asList(4924631L, 4925141L));
    assertUtxosEquals(tx0x2_pool05.getSpendFroms(), spendFroms);

    // 0.01 pool
    Tx0 tx0x2_pool01 = decoyTx0x2s.get(1);
    log.info("tx0_pool01 = " + tx0x2_pool01);
    assertTx0(tx0x2_pool01, "0.01btc", true, 8, Arrays.asList(902070L, 902580L));
    assertUtxosEquals(tx0x2_pool01.getSpendFroms(), tx0x2_pool05.getCascadingChangeUtxos());

    // 0.001 pool
    Tx0 tx0x2_pool001 = decoyTx0x2s.get(2);
    log.info("tx0_pool001 = " + tx0x2_pool001);
    assertTx0(tx0x2_pool001, "0.001btc", true, 16, Arrays.asList(97370L, 97370L)); // split
    assertUtxosEquals(tx0x2_pool001.getSpendFroms(), tx0x2_pool01.getCascadingChangeUtxos());
  }

  /**
   * Test case for Tx0Service.computeSpendFromAmountsStonewall() Sorts spendFroms in descending
   * order (helps certain cases) ex: 3 utxos [0.0009, 0.003, 0.0009] - would fail if unsorted; sets
   * would be {0.0039} {0.0009} - passes if sorted; sets would be {0.003}{0.0018}
   */
  @Test
  public void tx0x2_decoy_3utxos() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 900000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 3000000);
    BipUtxo spendFromUtxo2 =
        newUtxo("3268819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 3, 900000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1, spendFromUtxo2);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 decoyTx0x2 = tx0Result.getList().iterator().next();

    // verify
    log.info("decoyTx0x2 = " + decoyTx0x2);
    assertTx0(decoyTx0x2, "0.01btc", true, 3, Arrays.asList(977992L, 778247L));
    assertUtxosEquals(decoyTx0x2.getSpendFroms(), spendFroms);
  }

  /**
   * Both change outputs must be large enough for lower pool level. If 1 change output is not large
   * enough, skips pool level and continues to lower pool.
   *
   * <p>Compare with tx0x2 tests: {@link
   * WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool05_senderSkip01()} {@link
   * WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool05_counterpartyNo01()} Change values might differ
   * slightly for lower pools due fake samourai "fee" back to self
   */
  @Test
  public void tx0x2_decoy_cascade_pool05_skip01() throws Exception {
    log.info(
        "Testing Decoy Tx0x2s for pools 0.05, & 0.001. Only 1 change output large enough for pool 0.01 so skipped.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 6000000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 20000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    tx0Config.setTx0x2DecoyForced(true); // force Tx0x2 decoy => skip 01
    tx0Config.setCascade(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    List<Tx0> decoyTx0x2s = tx0Result.getList();

    // verify
    log.info("decoyTx0x2s = " + decoyTx0x2s);
    assertTx0Previews(
        decoyTx0x2s,
        Arrays.asList("0.05btc", "0.001btc"),
        Arrays.asList(true, true),
        Arrays.asList(4, 21));

    // 0.05 pool
    Tx0 tx0x2_pool05 = decoyTx0x2s.get(0);
    log.info("tx0_pool05 = " + tx0x2_pool05);
    assertTx0(tx0x2_pool05, "0.05btc", true, 4, Arrays.asList(4924631L, 925141L));
    assertUtxosEquals(tx0x2_pool05.getSpendFroms(), spendFroms);

    // 0.001 pool
    Tx0 tx0x2_pool001 = decoyTx0x2s.get(1);
    log.info("tx0_pool001 = " + tx0x2_pool001);
    assertTx0(tx0x2_pool001, "0.001btc", true, 21, Arrays.asList(3718579L, 19854L));
    assertUtxosEquals(tx0x2_pool001.getSpendFroms(), tx0x2_pool05.getCascadingChangeUtxos());
  }

  /**
   * Doesn't reach 0.001 pool but split changes on 0.01.
   *
   * <p>Compare with tx0x2 test {@link WhirlpoolWalletTx0x2Test#tx0x2_cascade_pool05_no001()} Change
   * values differ slightly
   */
  @Test
  public void tx0x2_decoy_cascade_pool05_no001() throws Exception {
    log.info("Testing Decoy Tx0x2s for pools 0.05 & 0.01. Doesn't reach pool 0.001.");

    // mock initial data
    BipUtxo spendFromUtxo0 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 9170000);
    BipUtxo spendFromUtxo1 =
        newUtxo("7408819d56ec916ea3754abe927ef99590cfb0c5a675366a7bcd7ce6ac9ed69a", 2, 19130000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo0, spendFromUtxo1);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true); // set decoy Tx0x2 flag
    tx0Config.setCascade(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);
    List<Tx0> decoyTx0x2s = tx0Result.getList();

    // verify
    log.info("decoyTx0x2s = " + decoyTx0x2s);
    assertTx0Previews(
        decoyTx0x2s,
        Arrays.asList("0.05btc", "0.01btc"),
        Arrays.asList(true, true),
        Arrays.asList(4, 8));

    // 0.05 pool
    Tx0 tx0x2_pool05 = decoyTx0x2s.get(0);
    log.info("tx0_pool05 = " + tx0x2_pool05);
    assertTx0(tx0x2_pool05, "0.05btc", true, 4, Arrays.asList(4054631L, 4095141L));
    assertUtxosEquals(tx0x2_pool05.getSpendFroms(), spendFroms);

    // 0.01 pool
    Tx0 tx0x2_pool01 = decoyTx0x2s.get(1);
    log.info("tx0_pool01 = " + tx0x2_pool01);
    assertTx0(tx0x2_pool01, "0.01btc", true, 8, Arrays.asList(52325L, 52325L)); // split change
    assertUtxosEquals(tx0x2_pool01.getSpendFroms(), tx0x2_pool05.getCascadingChangeUtxos());

    // 0.001 pool not reached
  }
}
