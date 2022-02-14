package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.wallet.api.paynym.PaynymApi;
import com.samourai.wallet.api.paynym.PaynymServer;
import com.samourai.wallet.api.paynym.beans.PaynymContact;
import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpirablePaynymSupplierTest extends AbstractTest {
  private static final String PCODE =
      "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6";
  private static final String PCODE2 =
      "PM8TJfP8GCovEuu715SgTzzhRFY6Lki9E9T9JJR4JRyqEBXcFmMmfSrz58cY5MhaDEfd1BuWUBXPwjk1vRm4aTHcBM2vQyVvQhcdTGRQGNCnGeqbWW4B";
  private static final NetworkParameters params = TestNet3Params.get();

  private ExpirablePaynymSupplier paynymWalletImpl;

  public ExpirablePaynymSupplierTest() throws Exception {
    super();

    Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
    PaynymApi paynymApi = new PaynymApi(httpClient, PaynymServer.get().getUrl(), bip47Util);

    HD_Wallet bip44w =
        HD_WalletFactoryGeneric.getInstance().restoreWallet(SEED_WORDS, SEED_PASSPHRASE, params);
    BIP47Wallet bip47w = new BIP47Wallet(bip44w);
    WalletStateSupplier walletStateSupplier = computeWalletStateSupplier();
    paynymWalletImpl = new ExpirablePaynymSupplier(999999, bip47w, paynymApi, walletStateSupplier);
    Assertions.assertEquals(PCODE, paynymWalletImpl.getPaymentCode());
  }

  private WalletStateSupplier computeWalletStateSupplier() {
    return new WalletStateSupplier() {
      @Override
      public boolean isInitialized() {
        return false;
      }

      @Override
      public void setInitialized(boolean value) {}

      @Override
      public boolean isNymClaimed() {
        return false;
      }

      @Override
      public void setNymClaimed(boolean value) {}

      @Override
      public IIndexHandler getIndexHandlerExternal() {
        return null;
      }

      @Override
      public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
        return null;
      }

      @Override
      public void load() throws Exception {}

      @Override
      public boolean persist(boolean force) throws Exception {
        return false;
      }
    };
  }

  @Test
  public void claim() throws Exception {
    paynymWalletImpl.claim().blockingAwait();
  }

  @Test
  public void followUnfollow() throws Exception {

    // follow
    paynymWalletImpl.follow(PCODE2).blockingAwait();

    // verify
    PaynymState paynymState = paynymWalletImpl.getPaynymState();
    PaynymContact paynymContact = paynymState.getFollowing().iterator().next();
    Assertions.assertEquals(PCODE2, paynymContact.getCode());
    Assertions.assertEquals("nymHc99UYDRYd6EdPYxbLCSLC", paynymContact.getNymId());
    Assertions.assertEquals("+boldboat533", paynymContact.getNymName());

    // unfollow
    paynymWalletImpl.unfollow(PCODE2).blockingAwait();

    // verify
    paynymState = paynymWalletImpl.getPaynymState();
    Assertions.assertFalse(paynymState.getFollowing().contains(PCODE2));
  }

  @Test
  public void getNym() throws Exception {
    PaynymState paynymState = paynymWalletImpl.getPaynymState();
    Assertions.assertTrue(paynymState.isClaimed());
    Assertions.assertEquals("/" + PCODE + "/avatar", paynymState.getNymAvatar());
    Assertions.assertEquals("+stillmud69f", paynymState.getNymName());
    Assertions.assertEquals("nymmFABjPvpR2uxmAUKfD53mj", paynymState.getNymID());
    Assertions.assertEquals(true, paynymState.isSegwit());

    Assertions.assertTrue(paynymState.getFollowing().isEmpty());
    Assertions.assertTrue(paynymState.getFollowers().isEmpty());
  }
}
