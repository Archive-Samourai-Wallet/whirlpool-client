package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.IBackendClient;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.api.backend.seenBackend.SeenResponse;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PostmixIndexServiceTest extends AbstractTest {
  private PostmixIndexService postmixIndexService;
  private IPostmixHandler postmixHandler;

  public PostmixIndexServiceTest() throws Exception {}

  @BeforeEach
  public void setup() throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip44w =
        HD_WalletFactoryGeneric.getInstance().getBIP44(seed, SEED_PASSPHRASE, params);
    BipWallet walletPostmix =
        new BipWallet(bip44w, new MemoryIndexHandlerSupplier(), BIP_WALLET.POSTMIX_BIP84);
    postmixHandler = new Bip84PostmixHandler(params, walletPostmix, IndexRange.EVEN);

    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(null);
    postmixIndexService = new PostmixIndexService(config);
  }

  @Test
  public void checkPostmixIndexMock() throws Exception {
    doCheckPostmixIndexMock(0);
    doCheckPostmixIndexMock(0);
    doCheckPostmixIndexMock(15);
    // doCheckPostmixIndex(2598);
    // doCheckPostmixIndex(11251);
  }

  @Test
  public void checkPostmixIndex_alreadyUsed() throws Exception {
    ISeenBackend seenBackend =
        BackendApi.newBackendApiSamourai(httpClient, BackendServer.TESTNET.getBackendUrlClear());
    PostmixIndexAlreadyUsedException e =
        Assertions.assertThrows(
            PostmixIndexAlreadyUsedException.class,
            () -> postmixIndexService.checkPostmixIndex(postmixHandler, seenBackend));
    Assertions.assertEquals(0, e.getPostmixIndex());
  }

  @Test
  public void checkPostmixIndex_failure() throws Exception {
    ISeenBackend seenBackend = BackendApi.newBackendApiDojo(httpClient, "http://foo", "foo");

    // ignore other errors such as http timeout
    postmixIndexService.checkPostmixIndex(postmixHandler, seenBackend); // no exception thrown
  }

  private void doCheckPostmixIndexMock(int validPostmixIndex) throws Exception {
    // mock serverApi
    ISeenBackend seenBackend = mockSeenBackend(validPostmixIndex, postmixHandler);
    try {
      // check
      postmixIndexService.checkPostmixIndex(postmixHandler, seenBackend);
    } catch (PostmixIndexAlreadyUsedException e) {
      // postmix index is desynchronized
      postmixIndexService.fixPostmixIndex(postmixHandler, seenBackend);
    }

    // verify
    int postmixIndex = postmixHandler.getIndexHandler().get();
    int minAcceptable = validPostmixIndex - PostmixIndexService.POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP;
    int maxAcceptable = validPostmixIndex + PostmixIndexService.POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP;
    Assertions.assertTrue(postmixIndex >= minAcceptable && postmixIndex <= maxAcceptable);
  }

  private ISeenBackend mockSeenBackend(int validPostmixIndex, IPostmixHandler postmixHandler)
      throws Exception {
    final List<String> alreadyUsedAddresses = new LinkedList<String>();
    for (int i = 0; i < validPostmixIndex; i++) {
      String address = postmixHandler.computeDestination(i).getAddress();
      alreadyUsedAddresses.add(address);
    }

    ISeenBackend seenBackend =
        new ISeenBackend() {
          @Override
          public SeenResponse seen(Collection<String> addresses) throws Exception {
            return new SeenResponse(null) {
              @Override
              public boolean isSeen(String address) {
                return alreadyUsedAddresses.contains(address);
              }
            };
          }

          @Override
          public boolean seen(String address) throws Exception {
            return alreadyUsedAddresses.contains(address);
          }

          @Override
          public IBackendClient getHttpClient() {
            return null;
          }
        };
    return seenBackend;
  }
}
