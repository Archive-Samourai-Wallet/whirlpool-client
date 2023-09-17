package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.rest.CheckOutputRequest;
import com.samourai.whirlpool.protocol.rest.RestErrorResponse;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PostmixIndexServiceTest extends AbstractTest {
  private PostmixIndexService postmixIndexService;
  private BipWallet walletPostmix;

  public PostmixIndexServiceTest() throws Exception {}

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip44w =
        HD_WalletFactoryGeneric.getInstance().getBIP44(seed, SEED_PASSPHRASE, params);
    walletPostmix =
        new BipWallet(bip44w, new MemoryIndexHandlerSupplier(), BIP_WALLET.POSTMIX_BIP84);
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
    postmixIndexService = new PostmixIndexService(whirlpoolWalletConfig);

    PostmixIndexAlreadyUsedException e =
        Assertions.assertThrows(
            PostmixIndexAlreadyUsedException.class,
            () -> postmixIndexService.checkPostmixIndex(walletPostmix));
    Assertions.assertEquals(0, e.getPostmixIndex());
  }

  @Test
  public void checkPostmixIndex_failure() throws Exception {
    ServerApi serverApi = new ServerApi("http://foo", computeHttpClientService());
    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(false, serverApi);
    postmixIndexService = new PostmixIndexService(config);

    // ignore other errors such as http timeout
    postmixIndexService.checkPostmixIndex(walletPostmix); // no exception thrown
  }

  private void doCheckPostmixIndexMock(int validPostmixIndex) throws Exception {
    // mock serverApi
    ServerApi serverApi = mockServerApi(validPostmixIndex, walletPostmix);
    WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(false, serverApi);
    postmixIndexService = new PostmixIndexService(config);

    try {
      // check
      postmixIndexService.checkPostmixIndex(walletPostmix);
    } catch (PostmixIndexAlreadyUsedException e) {
      // postmix index is desynchronized
      postmixIndexService.fixPostmixIndex(walletPostmix);
    }

    // verify
    int postmixIndex = walletPostmix.getIndexHandlerReceive().get();
    int minAcceptable = validPostmixIndex - PostmixIndexService.POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP;
    int maxAcceptable = validPostmixIndex + PostmixIndexService.POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP;
    Assertions.assertTrue(postmixIndex >= minAcceptable && postmixIndex <= maxAcceptable);
  }

  private ServerApi mockServerApi(int validPostmixIndex, BipWallet walletPostmix) {
    final List<String> alreadyUsedAddresses = new LinkedList<String>();
    for (int i = 0; i < validPostmixIndex; i++) {
      String address = walletPostmix.getAddressAt(0, i).getAddressString();
      alreadyUsedAddresses.add(address);
    }

    ServerApi serverApi =
        new ServerApi(null, mockHttpClientService()) {
          @Override
          public Single<Optional<String>> checkOutput(final CheckOutputRequest checkOutputRequest) {
            return httpObservable(
                () -> {
                  if (alreadyUsedAddresses.contains(checkOutputRequest.receiveAddress)) {
                    // already used
                    String responseBody =
                        ClientUtils.toJsonString(
                            new RestErrorResponse(
                                611,
                                PostmixIndexService.CHECKOUTPUT_ERROR_OUTPUT_ALREADY_REGISTERED));
                    throw new HttpException((Exception) null, responseBody);
                  }
                  return "OK";
                });
          }
        };
    return serverApi;
  }

  private <T> Single<Optional<T>> httpObservable(final Callable<T> supplier) {
    return Single.fromCallable(() -> Optional.ofNullable(supplier.call()))
        .subscribeOn(Schedulers.io());
  }
}
