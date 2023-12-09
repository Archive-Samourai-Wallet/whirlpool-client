package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.Chain;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostmixIndexService {
  private Logger log = LoggerFactory.getLogger(PostmixIndexService.class);
  private static final int POSTMIX_INDEX_RANGE_ITERATIONS = 50;
  protected static final int POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP = 4;

  private WhirlpoolWalletConfig config;

  public PostmixIndexService(WhirlpoolWalletConfig config) {
    this.config = config;
  }

  public synchronized void checkPostmixIndex(BipWallet walletPostmix, ISeenBackend seenBackend)
      throws PostmixIndexAlreadyUsedException {
    IIndexHandler postmixIndexHandler = walletPostmix.getIndexHandlerReceive();

    // check next output
    int postmixIndex =
        ClientUtils.computeNextReceiveAddressIndex(
            postmixIndexHandler, config.getIndexRangePostmix());

    try {
      checkPostmixIndex(walletPostmix, postmixIndex, seenBackend);
    } catch (PostmixIndexAlreadyUsedException e) {
      // forward error
      throw e;
    } catch (Exception e) {
      // ignore other errors such as http timeout
      log.warn("ignoring checkPostmixIndexAsync failure", e);
    } finally {
      postmixIndexHandler.cancelUnconfirmed(postmixIndex);
    }
  }

  protected void checkPostmixIndex(
      BipWallet walletPostmix, int postmixIndex, ISeenBackend seenBackend) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("checking postmixIndex: " + postmixIndex);
    }
    BipAddress bipAddress = walletPostmix.getAddressAt(Chain.RECEIVE.getIndex(), postmixIndex);
    String outputAddress = bipAddress.getAddressString();

    boolean seen = false;
    try {
      seen = seenBackend.seen(outputAddress);
    } catch (Exception e) {
      // ignore http failures
      if (log.isDebugEnabled()) {
        log.error("seenBackend failure", e);
      }
    }
    if (seen) {
      throw new PostmixIndexAlreadyUsedException(postmixIndex);
    }
  }

  public synchronized void fixPostmixIndex(BipWallet walletPostmix, ISeenBackend seenBackend)
      throws PostmixIndexAlreadyUsedException {
    IIndexHandler postmixIndexHandler = walletPostmix.getIndexHandlerReceive();

    int leftIndex = 0;
    int rightIndex = 0;
    for (int i = 0; i < POSTMIX_INDEX_RANGE_ITERATIONS; i++) {
      try {
        // quickly find index range
        Pair<Integer, Integer> indexRange = findPostmixIndexRange(walletPostmix, seenBackend);
        leftIndex = indexRange.getLeft();
        rightIndex = indexRange.getRight();
        if (log.isDebugEnabled()) {
          log.debug("valid postmixIndex range #" + i + ": [" + leftIndex + ";" + rightIndex + "]");
        }
      } catch (PostmixIndexAlreadyUsedException e) {
        throw e;
      } catch (Exception e) {
        // ignore other errors such as http timeout
        log.warn("ignoring findPostmixIndexRange failure", e);
        return;
      }

      if ((rightIndex - leftIndex) < POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP) {
        // finished
        if (log.isDebugEnabled()) {
          log.debug("fixing postmixIndex: " + rightIndex);
        }
        postmixIndexHandler.confirmUnconfirmed(rightIndex);
        return;
      }
      // continue with closer and closer index range...
    }
    log.error(
        "PostmixIndex error - please resync your wallet or contact support. PostmixIndex=["
            + leftIndex
            + ";"
            + rightIndex
            + "]");
    throw new PostmixIndexAlreadyUsedException(leftIndex);
  }

  // throws
  private Pair<Integer, Integer> findPostmixIndexRange(
      BipWallet walletPostmix, ISeenBackend seenBackend) throws Exception {
    IIndexHandler postmixIndexHandler = walletPostmix.getIndexHandlerReceive();

    int postmixIndex = 0;
    int incrementGap = 1;
    for (int i = 0; i < POSTMIX_INDEX_RANGE_ITERATIONS; i++) {
      int leftIndex = 0;
      try {
        // increment by incrementGap
        for (int x = 0; x < incrementGap; x++) {
          postmixIndex =
              ClientUtils.computeNextReceiveAddressIndex(
                  postmixIndexHandler, config.getIndexRangePostmix());

          // set leftIndex
          if (x == 0) {
            leftIndex = postmixIndex;
          }
        }

        // check next output
        checkPostmixIndex(walletPostmix, postmixIndex, seenBackend);

        // success!
        if (log.isDebugEnabled()) {
          log.debug("valid postmixIndex: " + postmixIndex);
        }

        // set postmixIndex to leftIndex (by cancelling unconfirmed indexs > leftIndex)
        if (log.isDebugEnabled()) {
          log.debug("rollbacking postmixIndex: " + postmixIndex + " => " + leftIndex);
        }
        for (int unconfirmedIndex = postmixIndex;
            unconfirmedIndex > leftIndex;
            unconfirmedIndex--) {
          postmixIndexHandler.cancelUnconfirmed(unconfirmedIndex);
        }

        // => return inclusive range
        return Pair.of(leftIndex, postmixIndex);

      } catch (PostmixIndexAlreadyUsedException e) {
        log.warn("postmixIndex already used: " + postmixIndex);

        // quick look-forward
        incrementGap *= 2;

        // avoid flooding
        try {
          Thread.sleep(500);
        } catch (InterruptedException ee) {
        }
      }
    }
    throw new PostmixIndexAlreadyUsedException(postmixIndex);
  }
}
