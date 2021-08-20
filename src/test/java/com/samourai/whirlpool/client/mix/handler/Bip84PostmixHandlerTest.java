package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.BipWalletAndAddressType;
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

  private BipWalletAndAddressType bipWallet;

  public Bip84PostmixHandlerTest() throws Exception {
    super();
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    bipWallet =
        new BipWalletAndAddressType(
            bip84w,
            WhirlpoolAccount.POSTMIX,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
  }

  @Test
  public void computeNextReceiveAddressIndex() throws Exception {
    Bip84PostmixHandler phCli = new Bip84PostmixHandler(params, bipWallet, false);
    Bip84PostmixHandler phMobile = new Bip84PostmixHandler(params, bipWallet, true);

    Assert.assertEquals(0, phCli.computeDestination().getIndex());
    Assert.assertEquals(2, phCli.computeDestination().getIndex());
    Assert.assertEquals(4, phCli.computeDestination().getIndex());

    Assert.assertEquals(5, phMobile.computeDestination().getIndex());
    Assert.assertEquals(7, phMobile.computeDestination().getIndex());
    Assert.assertEquals(9, phMobile.computeDestination().getIndex());

    Assert.assertEquals(10, phCli.computeDestination().getIndex());
    Assert.assertEquals(11, phMobile.computeDestination().getIndex());
    Assert.assertEquals(12, phCli.computeDestination().getIndex());
    Assert.assertEquals(13, phMobile.computeDestination().getIndex());
    Assert.assertEquals(15, phMobile.computeDestination().getIndex());
    Assert.assertEquals(16, phCli.computeDestination().getIndex());
    Assert.assertEquals(18, phCli.computeDestination().getIndex());
  }
}
