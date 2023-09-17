package com.samourai.whirlpool.client.wallet.data.walletState;

import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WalletStatePersistableSupplierTest extends AbstractTest {
  protected WalletStateSupplier walletStateSupplier;
  protected BipWallet walletPostmix;

  public WalletStatePersistableSupplierTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    load();
  }

  protected void load() throws Exception {
    walletStateSupplier = computeWalletStateSupplier();

    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip44w =
        HD_WalletFactoryGeneric.getInstance().getBIP44(seed, SEED_PASSPHRASE, params);
    walletPostmix = new BipWallet(bip44w, walletStateSupplier, BIP_WALLET.POSTMIX_BIP84);
  }

  @Test
  public void isNymClaimed() throws Exception {
    Assertions.assertFalse(walletStateSupplier.isNymClaimed());

    walletStateSupplier.setNymClaimed(true);
    Assertions.assertTrue(walletStateSupplier.isNymClaimed());

    walletStateSupplier.persist(true);
    Assertions.assertTrue(walletStateSupplier.isNymClaimed());
  }

  @Test
  public void indexHandler_set() throws Exception {
    IIndexHandler indexHandler =
        walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(0, indexHandler.get());

    indexHandler.set(1, false);
    Assertions.assertEquals(1, indexHandler.get());

    indexHandler.set(5, false);
    Assertions.assertEquals(5, indexHandler.get());

    indexHandler.set(2, false);
    Assertions.assertEquals(5, indexHandler.get());

    indexHandler.set(4, false);
    Assertions.assertEquals(5, indexHandler.get());

    indexHandler.set(4, true);
    Assertions.assertEquals(4, indexHandler.get());
  }

  @Test
  public void indexHandler_getAndIncrement() throws Exception {
    IIndexHandler indexHandler =
        walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(0, indexHandler.getAndIncrement());
    Assertions.assertEquals(1, indexHandler.get());

    indexHandler = walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(1, indexHandler.getAndIncrement());
    Assertions.assertEquals(2, indexHandler.get());

    walletStateSupplier.persist(true);
    Assertions.assertEquals(2, indexHandler.get());

    indexHandler = walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(2, indexHandler.getAndIncrement());
    Assertions.assertEquals(3, indexHandler.get());

    Assertions.assertEquals(3, indexHandler.getAndIncrement());
    Assertions.assertEquals(4, indexHandler.get());
  }

  @Test
  public void indexHandler_getAndIncrementUnconfirmed() throws Exception {
    IIndexHandler indexHandler =
        walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(0, indexHandler.getAndIncrementUnconfirmed());
    Assertions.assertEquals(0, indexHandler.get());

    indexHandler = walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(1, indexHandler.getAndIncrementUnconfirmed());
    Assertions.assertEquals(0, indexHandler.get());
    indexHandler.confirmUnconfirmed(0);

    indexHandler = walletStateSupplier.getIndexHandlerWallet(walletPostmix, Chain.RECEIVE);
    Assertions.assertEquals(1, indexHandler.get());
    Assertions.assertEquals(1, indexHandler.get());
    Assertions.assertEquals(2, indexHandler.getAndIncrementUnconfirmed());
    Assertions.assertEquals(1, indexHandler.get());

    walletStateSupplier.persist(true);
    Assertions.assertEquals(1, indexHandler.get());
    Assertions.assertEquals(1, indexHandler.get());
    Assertions.assertEquals(3, indexHandler.getAndIncrementUnconfirmed());
  }
}
