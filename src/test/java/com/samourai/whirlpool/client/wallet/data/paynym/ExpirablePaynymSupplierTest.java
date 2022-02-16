package com.samourai.whirlpool.client.wallet.data.paynym;

import com.samourai.wallet.api.paynym.PaynymApi;
import com.samourai.wallet.api.paynym.PaynymServer;
import com.samourai.wallet.api.paynym.beans.PaynymContact;
import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
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

  private WalletStateSupplier walletStateSupplier;
  private ExpirablePaynymSupplier paynymSupplier;

  public ExpirablePaynymSupplierTest() throws Exception {
    super();

    Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
    PaynymApi paynymApi = new PaynymApi(httpClient, PaynymServer.get().getUrl(), bip47Util);

    HD_Wallet bip44w =
        HD_WalletFactoryGeneric.getInstance().restoreWallet(SEED_WORDS, SEED_PASSPHRASE, params);
    BIP47Wallet bip47w = new BIP47Wallet(bip44w);
    walletStateSupplier = computeWalletStateSupplier();
    paynymSupplier = new ExpirablePaynymSupplier(999999, bip47w, paynymApi, walletStateSupplier);
    paynymSupplier.load();
    Assertions.assertEquals(PCODE, paynymSupplier.getPaymentCode());
  }

  @Test
  public void claim() throws Exception {
    paynymSupplier.claim().blockingAwait();
  }

  @Test
  public void followUnfollow() throws Exception {
    walletStateSupplier.setNymClaimed(true);

    // follow
    paynymSupplier.follow(PCODE2).blockingAwait();

    // verify
    PaynymState paynymState = paynymSupplier.getPaynymState();
    PaynymContact paynymContact = paynymState.getFollowing().iterator().next();
    Assertions.assertEquals(PCODE2, paynymContact.getCode());
    Assertions.assertEquals("nymHc99UYDRYd6EdPYxbLCSLC", paynymContact.getNymId());
    Assertions.assertEquals("+boldboat533", paynymContact.getNymName());

    // unfollow
    paynymSupplier.unfollow(PCODE2).blockingAwait();

    // verify
    paynymState = paynymSupplier.getPaynymState();
    Assertions.assertFalse(paynymState.getFollowing().contains(PCODE2));
  }

  @Test
  public void getPaynymState() throws Exception {
    doGetPaynymState(false);
    doGetPaynymState(true);
  }

  private void doGetPaynymState(boolean claimed) throws Exception {
    walletStateSupplier.setNymClaimed(claimed);
    paynymSupplier.refresh();
    PaynymState paynymState = paynymSupplier.getPaynymState();
    Assertions.assertEquals(claimed, paynymState.isClaimed());

    if (claimed) {
      Assertions.assertEquals("/" + PCODE + "/avatar", paynymState.getNymAvatar());
      Assertions.assertEquals("+stillmud69f", paynymState.getNymName());
      Assertions.assertEquals("nymmFABjPvpR2uxmAUKfD53mj", paynymState.getNymID());
      Assertions.assertEquals(true, paynymState.isSegwit());
    } else {
      Assertions.assertNull(paynymState.getNymAvatar());
      Assertions.assertNull(paynymState.getNymName());
      Assertions.assertNull(paynymState.getNymID());
      Assertions.assertNull(paynymState.isSegwit());
    }

    Assertions.assertTrue(paynymState.getFollowing().isEmpty());
    Assertions.assertTrue(paynymState.getFollowers().isEmpty());
  }
}
