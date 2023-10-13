package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.tx0.AbstractTx0ServiceV1Test;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolWalletTx0Test extends AbstractTx0ServiceV1Test {
  private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTx0Test.class);

  public WhirlpoolWalletTx0Test() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void tx0_nodecoy() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 5432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", false, 1, Arrays.asList(283730L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(148750, tx0.getFeeValue());
    Assertions.assertEquals(0, tx0.getFeeChange());
    Assertions.assertEquals(264, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_SCODE_nodecoy_50PERCENT() throws Exception {
    mockTx0Datas_SCODE_50PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 5432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", false, 1, Arrays.asList(358105L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(74375, tx0.getFeeValue());
    Assertions.assertEquals(0, tx0.getFeeChange());
    Assertions.assertEquals(264, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_SCODE_nodecoy_100PERCENT() throws Exception {
    mockTx0Datas_SCODE_100PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 5432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", false, 1, Arrays.asList(283730L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(0, tx0.getFeeValue());
    Assertions.assertEquals(148750, tx0.getFeeChange());
    Assertions.assertEquals(264, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_decoy() throws Exception {
    // mock initial data
    BipUtxo spendFromUtxo1 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 6000000);
    BipUtxo spendFromUtxo2 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324af", 2, 8000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo1, spendFromUtxo2);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool,
            Arrays.asList(spendFromUtxo1, spendFromUtxo2),
            Tx0FeeTarget.BLOCKS_12,
            Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", true, 2, Arrays.asList(2925172L, 925172L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(148750, tx0.getFeeValue());
    Assertions.assertEquals(0, tx0.getFeeChange());
    Assertions.assertEquals(396, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_SCODE_decoy_50PERCENT() throws Exception {
    mockTx0Datas_SCODE_50PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo1 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 6000000);
    BipUtxo spendFromUtxo2 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324af", 2, 8000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo1, spendFromUtxo2);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool,
            Arrays.asList(spendFromUtxo1, spendFromUtxo2),
            Tx0FeeTarget.BLOCKS_12,
            Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", true, 2, Arrays.asList(2962360L, 962359L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(74375, tx0.getFeeValue());
    Assertions.assertEquals(0, tx0.getFeeChange());
    Assertions.assertEquals(396, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_SCODE_decoy_100PERCENT() throws Exception {
    mockTx0Datas_SCODE_100PERCENT();

    // mock initial data
    BipUtxo spendFromUtxo1 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 6000000);
    BipUtxo spendFromUtxo2 =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324af", 2, 8000000);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo1, spendFromUtxo2);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool,
            Arrays.asList(spendFromUtxo1, spendFromUtxo2),
            Tx0FeeTarget.BLOCKS_12,
            Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(true);

    // run
    Tx0Result tx0Result = whirlpoolWallet.tx0(tx0Config);

    // verify
    Assertions.assertEquals(1, tx0Result.getList().size());
    Tx0 tx0 = tx0Result.getList().iterator().next();
    assertTx0(tx0, "0.05btc", true, 2, Arrays.asList(2925172L, 925172L));
    assertUtxosEquals(tx0.getSpendFroms(), spendFroms);
    Assertions.assertEquals(0, tx0.getFeeValue());
    Assertions.assertEquals(148750, tx0.getFeeChange());
    Assertions.assertEquals(396, tx0.getTx().getFee().getValue());
  }

  @Test
  public void tx0_cascading_nodecoy() throws Exception {
    log.info("Testing 0.05432999 btc. Makes Tx0s for pools 0.05 & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 5432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    assertTx0Previews(
        tx0s,
        Arrays.asList("0.05btc", "0.001btc"),
        Arrays.asList(false, false),
        Arrays.asList(1, 2));
  }

  @Test
  public void tx0Cascade_test0_nodecoy() throws Exception {
    log.info("Testing 0.05432999 btc. Makes Tx0s for pools 0.05 & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 5432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    assertTx0Previews(
        tx0s,
        Arrays.asList("0.05btc", "0.001btc"),
        Arrays.asList(false, false),
        Arrays.asList(1, 2));
    Tx0 tx0_pool05 = tx0s.get(0);
    assertTx0(tx0_pool05, "0.05btc", false, 1, Arrays.asList(283730L));
    assertUtxosEquals(tx0_pool05.getSpendFroms(), spendFroms);

    Tx0 tx0_pool001 = tx0s.get(1);
    assertTx0(tx0_pool001, "0.001btc", false, 2, Arrays.asList(77925L));
    assertUtxosEquals(tx0_pool001.getSpendFroms(), tx0_pool05.getCascadingChangeUtxos());
  }

  @Test
  public void tx0Cascade_test1() throws Exception {
    log.info("Testing 0.06432999 btc. Makes Tx0s for pools 0.05, 0.01, & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 6432999);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.05btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    Assertions.assertEquals(3, tx0s.size());
    Tx0 tx0_pool05 = tx0s.get(0);
    Tx0 tx0_pool01 = tx0s.get(1);
    Tx0 tx0_pool001 = tx0s.get(2);

    log.info("tx0_pool05 = " + tx0_pool05);
    assertTx0(tx0_pool05, "0.05btc", false, 1, Arrays.asList(1283730L));
    assertUtxosEquals(tx0_pool05.getSpendFroms(), spendFroms);

    log.info("tx0_pool01 = " + tx0_pool01);
    assertTx0(tx0_pool01, "0.01btc", false, 1, Arrays.asList(240711L));
    assertUtxosEquals(tx0_pool01.getSpendFroms(), tx0_pool05.getCascadingChangeUtxos());

    log.info("tx0_pool001 = " + tx0_pool001);
    assertTx0(tx0_pool001, "0.001btc", false, 2, Arrays.asList(34906L));
    assertUtxosEquals(tx0_pool001.getSpendFroms(), tx0_pool01.getCascadingChangeUtxos());
  }

  @Test
  public void tx0Cascade_test2() throws Exception {
    log.info("Testing 0.74329991 btc. Makes Tx0s for pools 0.5, 0.05, 0.01, & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 74329991);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.5btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, Arrays.asList(spendFromUtxo), Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    Assertions.assertEquals(4, tx0s.size());
    Tx0 tx0_pool5 = tx0s.get(0);
    Tx0 tx0_pool05 = tx0s.get(1);
    Tx0 tx0_pool01 = tx0s.get(2);
    Tx0 tx0_pool001 = tx0s.get(3);

    log.info("tx0_pool5 = " + tx0_pool5);
    assertTx0(tx0_pool5, "0.5btc", false, 1, Arrays.asList(22841972L));
    assertUtxosEquals(tx0_pool5.getSpendFroms(), spendFroms);

    log.info("tx0_pool05 = " + tx0_pool05);
    assertTx0(tx0_pool05, "0.05btc", false, 4, Arrays.asList(2691845L));
    assertUtxosEquals(tx0_pool05.getSpendFroms(), tx0_pool5.getCascadingChangeUtxos());

    log.info("tx0_pool01 = " + tx0_pool01);
    assertTx0(tx0_pool01, "0.01btc", false, 2, Arrays.asList(648540L));
    assertUtxosEquals(tx0_pool01.getSpendFroms(), tx0_pool05.getCascadingChangeUtxos());

    log.info("tx0_pool001 = " + tx0_pool001);
    assertTx0(tx0_pool001, "0.001btc", false, 6, Arrays.asList(41591L));
    assertUtxosEquals(tx0_pool001.getSpendFroms(), tx0_pool01.getCascadingChangeUtxos());
  }

  @Test
  public void tx0Cascade_test3() throws Exception {
    log.info("Testing 0.52329991 btc. Makes Tx0s for pools 0.5 & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 52329991);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.5btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, Arrays.asList(spendFromUtxo), Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    Assertions.assertEquals(2, tx0s.size());
    Tx0 tx0_pool5 = tx0s.get(0);
    Tx0 tx0_pool001 = tx0s.get(1);

    log.info("tx0_pool5 = " + tx0_pool5);
    assertTx0(tx0_pool5, "0.5btc", false, 1, Arrays.asList(841972L));
    assertUtxosEquals(tx0_pool5.getSpendFroms(), spendFroms);

    log.info("tx0_pool001 = " + tx0_pool001);
    assertTx0(tx0_pool001, "0.001btc", false, 8, Arrays.asList(34451L));
    assertUtxosEquals(tx0_pool001.getSpendFroms(), tx0_pool5.getCascadingChangeUtxos());
  }

  @Test
  public void tx0Cascade_test4() throws Exception {
    log.info("Testing 0.02329991 btc. Makes Tx0s for pools 0.01 & 0.001");

    // mock initial data
    BipUtxo spendFromUtxo =
        newUtxo("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, 2329991);
    List<BipUtxo> spendFroms = mockUtxos(spendFromUtxo);

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, Arrays.asList(spendFromUtxo), Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();

    // verify
    Assertions.assertEquals(2, tx0s.size());
    Tx0 tx0_pool01 = tx0s.get(0);
    Tx0 tx0_pool001 = tx0s.get(1);

    log.info("tx0_pool01 = " + tx0_pool01);
    assertTx0(tx0_pool01, "0.01btc", false, 2, Arrays.asList(286686L));
    assertUtxosEquals(tx0_pool01.getSpendFroms(), spendFroms);

    log.info("tx0_pool001 = " + tx0_pool001);
    assertTx0(tx0_pool001, "0.001btc", false, 2, Arrays.asList(80881L));
    assertUtxosEquals(tx0_pool001.getSpendFroms(), tx0_pool01.getCascadingChangeUtxos());
  }

  @Disabled // uncomment to manually broadcast a new tx0Cascade
  @Test
  public void tx0Cascade_manual() throws Exception {
    log.info("Deposit address: " + whirlpoolWallet.getDepositAddress(false));

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.01btc");
    Collection<WhirlpoolUtxo> spendFroms =
        whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT);
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();
    Tx0 firstTx0 = tx0s.iterator().next();

    log.info("Tx0: " + firstTx0.getSpendFroms() + " " + firstTx0.getTx());
  }

  @Disabled // uncomment to manually broadcast a new fake tx0Cascade
  @Test
  public void tx0Cascade_fake_manual() throws Exception {
    log.info("Deposit address: " + whirlpoolWallet.getDepositAddress(false));

    // configure TX0
    Pool pool = poolSupplier.findPoolById("0.001btc");
    Collection<WhirlpoolUtxo> spendFroms =
        whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT);
    Tx0Config tx0Config =
        whirlpoolWallet.getTx0Config(
            pool, spendFroms, Tx0FeeTarget.BLOCKS_12, Tx0FeeTarget.BLOCKS_12);
    tx0Config.setTx0x2Decoy(false);
    tx0Config.setCascade(true);

    // run
    List<Tx0> tx0s = whirlpoolWallet.tx0(tx0Config).getList();
    Tx0 tx0 = tx0s.iterator().next();
    log.info("Tx0: " + tx0.getSpendFroms() + " " + tx0.getTx());
  }
}
