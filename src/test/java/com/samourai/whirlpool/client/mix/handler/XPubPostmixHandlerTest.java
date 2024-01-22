package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.whirlpool.client.test.AbstractTest;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XPubPostmixHandlerTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(XPubPostmixHandlerTest.class);

  public XPubPostmixHandlerTest() throws Exception {
    super();
  }

  @Test
  public void test_vpub_postmix() throws Exception {
    String XPUB =
        "vpub5YEQpEDXWE3TawqjQNFt5o4sBM1RP1B1mtVZr8ysEA9hFLsZZ4RB8oxE4Sfkumc47jnVPUgRL9hJf3sWpTYBKtdkP3UK6J8p1n2ykmjHnrW";
    NetworkParameters params = TestNet3Params.get();
    int CHAIN = 1;
    XPubPostmixHandler postmixHandler =
        new XPubPostmixHandler(new MemoryIndexHandler(), params, XPUB, CHAIN);

    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        0,
        "tb1q6jfh0zp0mjfem9g655drczkkm49p2t3xlqyd69",
        DestinationType.XPUB,
        "m/84'/1'/2147483646'/1/0");
    assertMixDestination(
        postmixHandler.computeDestination(10),
        10,
        "tb1qy0jwwtmtqscfvktmfekrfw34xuwj73rgzm4kug",
        DestinationType.XPUB,
        "m/84'/1'/2147483646'/1/10");
    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        1,
        "tb1qv7n3qjsn449nrm4q5hvlrpj6p2d077mmukjd3e",
        DestinationType.XPUB,
        "m/84'/1'/2147483646'/1/1");
  }

  @Test
  public void test_vpub_deposit() throws Exception {
    String XPUB =
        "vpub5YEQpEDPAZWVTkmWASSHyaUMsae7uV9FnRrhZ3cqV6RFbBQx7wjVsUfLqSE3hgNY8WQixurkbWNkfV2sRE7LPfNKQh2t3s5une4QZthwdCu";
    NetworkParameters params = TestNet3Params.get();
    int CHAIN = 0;
    XPubPostmixHandler postmixHandler =
        new XPubPostmixHandler(new MemoryIndexHandler(), params, XPUB, CHAIN);

    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        0,
        "tb1qp4jqz890g3u30meeks68aeqyf7tdaeycyc6hd0",
        DestinationType.XPUB,
        "m/84'/1'/0'/0/0");
    assertMixDestination(
        postmixHandler.computeDestination(10),
        10,
        "tb1qhjt7esuhjtxkdna7av9hff9l8fs9x7ddpvt735",
        DestinationType.XPUB,
        "m/84'/1'/0'/0/10");
    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        1,
        "tb1q4crk5fzlr7qcz0nsun67luk982mn4wtlyydvlh",
        DestinationType.XPUB,
        "m/84'/1'/0'/0/1");
  }

  @Test
  public void test_xpub_deposit() throws Exception {
    String XPUB =
        "xpub6C8aSUjB7fwH6CSpS5AjRh1sPwfmrZKNNrfye5rkijhFpSfiKeSNT2CpVLuDzQiipdYAmmyi4eLXritVhYjfBfeEWJPXUrUEEHrcgnEH7wX";
    NetworkParameters params = MainNetParams.get();
    int CHAIN = 0;
    XPubPostmixHandler postmixHandler =
        new XPubPostmixHandler(new MemoryIndexHandler(), params, XPUB, CHAIN);

    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        0,
        "bc1qhy6dh6c67q8uwshffrs2rjs85x4wyhx9k45rha",
        DestinationType.XPUB,
        "m/44'/0'/0'/0/0");
    assertMixDestination(
        postmixHandler.computeDestination(10),
        10,
        "bc1qjwvsd49ahgn343r5cyazx27qk3dl243sncj0rx",
        DestinationType.XPUB,
        "m/44'/0'/0'/0/10");
    assertMixDestination(
        postmixHandler.computeDestinationNext(),
        1,
        "bc1q5scdtccre7m6x557antq9zkj5ed9m6nmuz36j4",
        DestinationType.XPUB,
        "m/44'/0'/0'/0/1");
  }

  private void assertMixDestination(
      MixDestination mixDestination,
      int index,
      String address,
      DestinationType destinationType,
      String path) {
    Assertions.assertEquals(index, mixDestination.getIndex());
    Assertions.assertEquals(address, mixDestination.getAddress());
    Assertions.assertEquals(destinationType, mixDestination.getType());
    Assertions.assertEquals(path, mixDestination.getPath());
  }
}
