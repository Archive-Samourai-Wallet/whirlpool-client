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
    HD_Wallet bip44w = hdWalletFactory.getBIP44(seed, SEED_PASSPHRASE, params);
    bipWallet =
        new BipWallet(
            bip44w,
            WhirlpoolAccount.DEPOSIT,
            new MemoryIndexHandler(),
            new MemoryIndexHandler(),
            AddressType.SEGWIT_NATIVE);
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1qp4jqz890g3u30meeks68aeqyf7tdaeycyc6hd0", toBech32(bipWallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1q7uef0jnnj2dnzguz438aeejpqhjk7z45ngd4ww", toBech32(bipWallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1q765gfuv0f4l83fqk0sl9vaeu8tjcuqtyrrduyv", toBech32(bipWallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1q7uef0jnnj2dnzguz438aeejpqhjk7z45ngd4ww", toBech32(bipWallet.getAddressAt(0, 15)));
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
        "vpub5YEQpEDPAZWVTkmWASSHyaUMsae7uV9FnRrhZ3cqV6RFbBQx7wjVsUfLqSE3hgNY8WQixurkbWNkfV2sRE7LPfNKQh2t3s5une4QZthwdCu",
        bipWallet.getPub(AddressType.SEGWIT_NATIVE));
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
