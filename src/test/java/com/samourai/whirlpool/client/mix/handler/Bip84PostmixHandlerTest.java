package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.BipWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandlerTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(Bip84PostmixHandlerTest.class);

  private BipWallet bipWallet;

  public Bip84PostmixHandlerTest() throws Exception {
    super();
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    bipWallet =
        new BipWallet(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
  }

  @Test
  public void computeNextReceiveAddressIndex() {
    Bip84PostmixHandler phCli = new Bip84PostmixHandler(bipWallet, false);
    Bip84PostmixHandler phMobile = new Bip84PostmixHandler(bipWallet, true);

    Assert.assertEquals(0, phCli.computeNextReceiveAddressIndex());
    Assert.assertEquals(2, phCli.computeNextReceiveAddressIndex());
    Assert.assertEquals(4, phCli.computeNextReceiveAddressIndex());

    Assert.assertEquals(5, phMobile.computeNextReceiveAddressIndex());
    Assert.assertEquals(7, phMobile.computeNextReceiveAddressIndex());
    Assert.assertEquals(9, phMobile.computeNextReceiveAddressIndex());

    Assert.assertEquals(10, phCli.computeNextReceiveAddressIndex());
    Assert.assertEquals(11, phMobile.computeNextReceiveAddressIndex());
    Assert.assertEquals(12, phCli.computeNextReceiveAddressIndex());
    Assert.assertEquals(13, phMobile.computeNextReceiveAddressIndex());
    Assert.assertEquals(15, phMobile.computeNextReceiveAddressIndex());
    Assert.assertEquals(16, phCli.computeNextReceiveAddressIndex());
    Assert.assertEquals(18, phCli.computeNextReceiveAddressIndex());
  }
}
