package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    bipWallet = new BipWallet(bip84w, new MemoryIndexHandlerSupplier(), BIP_WALLET.POSTMIX_BIP84);
  }

  @Test
  public void computeNextReceiveAddressIndex() throws Exception {
    Bip84PostmixHandler phCli = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Bip84PostmixHandler phMobile = new Bip84PostmixHandler(params, bipWallet, IndexRange.ODD);

    Assertions.assertEquals(0, phCli.computeDestination().getIndex());
    Assertions.assertEquals(2, phCli.computeDestination().getIndex());
    Assertions.assertEquals(4, phCli.computeDestination().getIndex());

    Assertions.assertEquals(5, phMobile.computeDestination().getIndex());
    Assertions.assertEquals(7, phMobile.computeDestination().getIndex());
    Assertions.assertEquals(9, phMobile.computeDestination().getIndex());

    Assertions.assertEquals(10, phCli.computeDestination().getIndex());
    Assertions.assertEquals(11, phMobile.computeDestination().getIndex());
    Assertions.assertEquals(12, phCli.computeDestination().getIndex());
    Assertions.assertEquals(13, phMobile.computeDestination().getIndex());
    Assertions.assertEquals(15, phMobile.computeDestination().getIndex());
    Assertions.assertEquals(16, phCli.computeDestination().getIndex());
    Assertions.assertEquals(18, phCli.computeDestination().getIndex());
  }
}
