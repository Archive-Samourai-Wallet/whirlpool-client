package com.samourai.wallet.client;

import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import org.junit.Assert;
import org.junit.Test;

public class BipWalletTest extends AbstractTest {
  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE = "whirlpool";
  private BipWallet bipWallet;

  public BipWalletTest() throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, SEED_PASSPHRASE, params);
    bipWallet =
        new BipWallet(
            bip84w, WhirlpoolAccount.DEPOSIT, new MemoryIndexHandler(), new MemoryIndexHandler());
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1q5lc455emwwttdqwf9p32xf8fhgrhvfp5vxvul7", toBech32(bipWallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1q2vw863w92dwpej48maqyjazj4ch3x0krzrw9cs", toBech32(bipWallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1qtfrd7zug2qkhv3nc6294pls92qru6vvqse40dw", toBech32(bipWallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1q2vw863w92dwpej48maqyjazj4ch3x0krzrw9cs", toBech32(bipWallet.getAddressAt(0, 15)));
  }

  @Test
  public void getNextAddress() throws Exception {
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(0, 0)), toBech32(bipWallet.getNextAddress()));
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(0, 1)), toBech32(bipWallet.getNextAddress()));
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(0, 2)), toBech32(bipWallet.getNextAddress()));

    // change
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(1, 0)), toBech32(bipWallet.getNextChangeAddress()));
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(1, 1)), toBech32(bipWallet.getNextChangeAddress()));
    Assert.assertEquals(
        toBech32(bipWallet.getAddressAt(1, 2)), toBech32(bipWallet.getNextChangeAddress()));
  }

  @Test
  public void getZpub() throws Exception {
    Assert.assertEquals(
        "vpub5YEQpEDXWE3TcFX9JXj73TaBskrDTy5pdw3HNujngNKfAYtgx1ynNd6ri92A8Jdgccm9BX4S8yo45hsK4oiCar15pqA7MHM9XtkzNySdknj",
        bipWallet.getPub(AddressType.SEGWIT_NATIVE));
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
