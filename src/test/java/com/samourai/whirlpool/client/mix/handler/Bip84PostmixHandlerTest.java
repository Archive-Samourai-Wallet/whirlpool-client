package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.constants.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandlerTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(Bip84PostmixHandlerTest.class);

  private BipWallet bipWallet;

  public Bip84PostmixHandlerTest() throws Exception {
    super();

    load();
  }

  protected void load() throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, SEED_PASSPHRASE, params);

    WalletStateSupplier walletStateSupplier = computeWalletStateSupplier();
    bipWallet =
        BIP_WALLET.POSTMIX_BIP84.newBipWallet(bipFormatSupplier, bip84w, walletStateSupplier);
  }

  @Test
  public void computeNextReceiveAddressIndex() throws Exception {
    Bip84PostmixHandler phCli = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Bip84PostmixHandler phMobile = new Bip84PostmixHandler(params, bipWallet, IndexRange.ODD);

    Assertions.assertEquals(0, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(2, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(4, phCli.computeDestinationNext().getIndex());

    Assertions.assertEquals(5, phMobile.computeDestinationNext().getIndex());
    Assertions.assertEquals(7, phMobile.computeDestinationNext().getIndex());
    Assertions.assertEquals(9, phMobile.computeDestinationNext().getIndex());

    Assertions.assertEquals(10, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(11, phMobile.computeDestinationNext().getIndex());
    Assertions.assertEquals(12, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(13, phMobile.computeDestinationNext().getIndex());
    Assertions.assertEquals(15, phMobile.computeDestinationNext().getIndex());
    Assertions.assertEquals(16, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(18, phCli.computeDestinationNext().getIndex());
  }

  @Test
  public void onMixFail() throws Exception {
    Bip84PostmixHandler phCli = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);

    Assertions.assertEquals(0, phCli.computeDestinationNext().getIndex());
    phCli.onRegisterOutput();
    Assertions.assertEquals(2, phCli.computeDestinationNext().getIndex());
    phCli.onRegisterOutput();
    phCli.onMixFail();
    Assertions.assertEquals(4, phCli.computeDestinationNext().getIndex());
    phCli.onMixFail();
    Assertions.assertEquals(4, phCli.computeDestinationNext().getIndex());
    phCli.onMixFail();
    Assertions.assertEquals(4, phCli.computeDestinationNext().getIndex());
    phCli.onRegisterOutput();
    phCli.onMixFail();
    Assertions.assertEquals(6, phCli.computeDestinationNext().getIndex());
    phCli.onRegisterOutput();
    phCli.onMixFail();
    Assertions.assertEquals(8, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(10, phCli.computeDestinationNext().getIndex());
    phCli.onRegisterOutput();
    phCli.onMixFail();
    Assertions.assertEquals(12, phCli.computeDestinationNext().getIndex());
    Assertions.assertEquals(14, phCli.computeDestinationNext().getIndex());

    // reload
    load();

    Assertions.assertEquals(16, phCli.computeDestinationNext().getIndex());
  }

  @Test
  public void multi() throws Exception {
    Bip84PostmixHandler ph1 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Bip84PostmixHandler ph2 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);

    Assertions.assertEquals(0, ph1.computeDestinationNext().getIndex());
    ph1.onRegisterOutput();

    Assertions.assertEquals(2, ph1.computeDestinationNext().getIndex());
    ph1.onRegisterOutput();

    Assertions.assertEquals(4, ph2.computeDestinationNext().getIndex());
    ph2.onRegisterOutput();
  }

  @Test
  public void multi2() throws Exception {
    Bip84PostmixHandler ph1 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Bip84PostmixHandler ph2 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Bip84PostmixHandler ph3 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);

    Assertions.assertEquals(0, ph1.computeDestinationNext().getIndex());
    Assertions.assertEquals(2, ph2.computeDestinationNext().getIndex());
    ph1.onMixFail();

    Bip84PostmixHandler ph4 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);

    Assertions.assertEquals(4, ph3.computeDestinationNext().getIndex());
    ph3.onRegisterOutput();

    ph4.onMixFail();
    ph2.onMixFail();

    Bip84PostmixHandler ph5 = new Bip84PostmixHandler(params, bipWallet, IndexRange.EVEN);
    Assertions.assertEquals(6, ph5.computeDestinationNext().getIndex());
  }
}
