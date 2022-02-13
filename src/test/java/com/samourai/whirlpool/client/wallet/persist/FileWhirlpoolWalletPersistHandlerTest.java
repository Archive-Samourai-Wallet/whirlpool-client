package com.samourai.whirlpool.client.wallet.persist;

import com.samourai.whirlpool.client.test.AbstractTest;

public class FileWhirlpoolWalletPersistHandlerTest extends AbstractTest {
  public FileWhirlpoolWalletPersistHandlerTest() throws Exception {
    super();
  }
  /*
  private FileWhirlpoolWalletPersistHandler persistHandler;
  private WhirlpoolWallet whirlpoolWallet;
  private final WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
  private File fileState;
  private File fileUtxos;

  @BeforeEach
  public void setup() throws Exception {
    String FILENAME_STATE = "/tmp/state";
    fileState = new File(FILENAME_STATE);
    if (fileState.exists()) {
      fileState.delete();
    }

    String FILENAME_UTXOS = "/tmp/utxos";
    fileUtxos = new File(FILENAME_UTXOS);
    if (fileUtxos.exists()) {
      fileUtxos.delete();
    }

    this.persistHandler = new FileWhirlpoolWalletPersistHandler(FILENAME_STATE, FILENAME_UTXOS);
    persistHandler.setInitialized(true);

    this.whirlpoolWallet = computeWallet();
  }

  private void reload() {
    ((FileWhirlpoolWalletPersistHandler) whirlpoolWallet.getConfig().getPersistHandler())
        .getUtxoConfigHandler()
        .loadUtxoConfigs(whirlpoolWallet);
  }

  private WhirlpoolUtxo computeUtxo(UnspentOutput utxo) {
    WhirlpoolAccount whirlpoolAccount = WhirlpoolAccount.DEPOSIT;
    WhirlpoolUtxoConfig utxoConfig = whirlpoolWallet.computeUtxoConfig(utxo, whirlpoolAccount);
    return new WhirlpoolUtxo(utxo, whirlpoolAccount, utxoConfig, WhirlpoolUtxoStatus.READY);
  }

  @Test
  public void testCleanup() throws Exception {
    // save

    UnspentOutput utxoFoo = new UnspentOutput();
    utxoFoo.tx_output_n = 1;
    utxoFoo.tx_hash = "foo";
    utxoFoo.value = 1234;
    utxoFoo.confirmations = 9999;
    utxoFoo.addr = "foo";
    utxoFoo.xpub = new UnspentOutput.Xpub();
    utxoFoo.xpub.path = "foo";
    WhirlpoolUtxo foo = computeUtxo(utxoFoo);

    UnspentOutput utxoBar = new UnspentOutput();
    utxoBar.tx_output_n = 2;
    utxoBar.tx_hash = "bar";
    utxoBar.value = 5678;
    utxoBar.confirmations = 8888;
    utxoBar.addr = "bar";
    utxoBar.xpub = new UnspentOutput.Xpub();
    utxoBar.xpub.path = "bar";
    WhirlpoolUtxo bar = computeUtxo(utxoBar);

    // verify
    Assert.assertNull(persistHandler.getUtxoConfig("foo"));
    Assert.assertNull(persistHandler.getUtxoConfig("foo", 2));
    Assert.assertNull(persistHandler.getUtxoConfig("bar", 1));

    persistHandler.save();

    // re-read
    reload();

    // verify
    Assert.assertNull(persistHandler.getUtxoConfig("foo"));
    Assert.assertNull(persistHandler.getUtxoConfig("foo", 2));
    Assert.assertNull(persistHandler.getUtxoConfig("bar", 1));

    // first clean => unchanged
    List<WhirlpoolUtxo> knownUtxos = Lists.of(foo);
    persistHandler.cleanUtxoConfig(knownUtxos);

    // verify
    Assert.assertNull(persistHandler.getUtxoConfig("foo"));
    Assert.assertNull(persistHandler.getUtxoConfig("foo", 2));
    Assert.assertNull(persistHandler.getUtxoConfig("bar", 1));

    // second clean => "bar" removed
    persistHandler.cleanUtxoConfig(knownUtxos);

    // verify
    Assert.assertNull(persistHandler.getUtxoConfig("foo"));
    Assert.assertNull(persistHandler.getUtxoConfig("foo", 2));
    Assert.assertNull(persistHandler.getUtxoConfig("bar", 2));
    Assert.assertNull(persistHandler.getUtxoConfig("bar", 1));

    // re-read
    reload();
  }

  private WhirlpoolWallet computeWallet() throws Exception {
    String backendUrl = BackendServer.TESTNET.getBackendUrl(false);
    BackendApi backendApi =
        new BackendApi(null, backendUrl, Optional.<OAuthManager>empty()) {
          @Override
          public MultiAddrResponse.Address fetchAddress(String zpub) throws Exception {
            // MOCK
            return new MultiAddrResponse.Address();
          }
        };
    byte[] seed =
        hdWalletFactory.computeSeedFromWords("all all all all all all all all all all all all");
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, "foo", params);

    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(
            null,
            null,
            WhirlpoolServer.LOCAL_TESTNET.getServerUrl(false),
            WhirlpoolServer.LOCAL_TESTNET.getParams(),
            false,
            backendApi);
    WhirlpoolDataService dataService =
        new WhirlpoolDataService(config, whirlpoolWalletService, "/tmp/utxos");
    return whirlpoolWalletService.openWallet(dataService, bip84w, "/tmp/state");
  }*/
  // TODO
}
