package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.whirlpool.client.exception.PostmixIndexAlreadyUsedException;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostmixIndexService {
  private Logger log = LoggerFactory.getLogger(PostmixIndexService.class);
  private static final int POSTMIX_INDEX_RANGE_ITERATIONS = 10000;
  protected static final int POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP = 4;
  protected static final int POSTMIX_INDEX_LOOKAHEAD = 10;

  private WhirlpoolWalletConfig config;

  public PostmixIndexService(WhirlpoolWalletConfig config) {
    this.config = config;
  }

  public synchronized void checkPostmixIndex(
      IPostmixHandler postmixHandler, ISeenBackend seenBackend)
      throws PostmixIndexAlreadyUsedException {
    IIndexHandler postmixIndexHandler = postmixHandler.getIndexHandler();

    // check next output
    int postmixIndex =
        ClientUtils.computeNextReceiveAddressIndex( // increments unconfirmed counter
            postmixIndexHandler, config.getIndexRangePostmix());

    try {
      checkPostmixIndex(postmixHandler, postmixIndex, seenBackend);
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
      IPostmixHandler postmixHandler, int postmixIndex, ISeenBackend seenBackend) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("checking postmixIndex: " + postmixIndex);
    }
    String outputAddress = postmixHandler.computeDestination(postmixIndex).getAddress();

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

  protected void checkPostmixIndexLookahead(
      IPostmixHandler postmixHandler, int postmixIndex, ISeenBackend seenBackend) throws Exception {
    int value = postmixIndex;
    List<Integer> unconfirmedIndexs = new LinkedList<>();
    for (int i = 0; i < POSTMIX_INDEX_LOOKAHEAD; i++) {
      if (log.isDebugEnabled()) {
        if (i > 0) {
          log.debug(
              "checking postmixIndex: "
                  + postmixIndex
                  + ", lookahead: "
                  + i
                  + "/"
                  + POSTMIX_INDEX_LOOKAHEAD);
        }
      }
      // throws PostmixIndexAlreadyUsedException
      checkPostmixIndex(postmixHandler, value, seenBackend);

      // increments unconfirmed counter
      value =
          ClientUtils.computeNextReceiveAddressIndex(
              postmixHandler.getIndexHandler(), config.getIndexRangePostmix());
      unconfirmedIndexs.add(value);
    }

    // rollback unconfirmed index on success (keep it on failure to continue looking further)
    for (int unconfirmedIndex : unconfirmedIndexs) {
      postmixHandler.getIndexHandler().cancelUnconfirmed(unconfirmedIndex);
    }
  }

  public synchronized int resetPostmixIndex(
      IPostmixHandler postmixHandler, ISeenBackend seenBackend) throws Exception {
    IIndexHandler postmixIndexHandler = postmixHandler.getIndexHandler();
    postmixIndexHandler.set(0, true);
    return fixPostmixIndex(postmixHandler, seenBackend);
  }

  public synchronized int fixPostmixIndex(IPostmixHandler postmixHandler, ISeenBackend seenBackend)
      throws Exception {
    IIndexHandler postmixIndexHandler = postmixHandler.getIndexHandler();

    int leftIndex = 0;
    int rightIndex = 0;
    for (int i = 0; i < POSTMIX_INDEX_RANGE_ITERATIONS; i++) {
      try {
        // quickly find index range
        Pair<Integer, Integer> indexRange = findPostmixIndexRange(postmixHandler, seenBackend);
        leftIndex = indexRange.getLeft();
        rightIndex = indexRange.getRight();
        if (log.isDebugEnabled()) {
          log.debug(
              "found candidate postmixIndex range #"
                  + i
                  + ": ["
                  + leftIndex
                  + ";"
                  + rightIndex
                  + "]");
        }
      } catch (PostmixIndexAlreadyUsedException e) {
        throw e;
      } catch (Exception e) {
        // throw other errors such as http timeout
        throw e;
      }

      if ((rightIndex - leftIndex) < POSTMIX_INDEX_RANGE_ACCEPTABLE_GAP) {
        if (log.isDebugEnabled()) {
          log.debug("found candidate postmixIndex: " + rightIndex);
        }
        try {
          // double-check with lookahead (increments unconfirmed counter)
          checkPostmixIndexLookahead(postmixHandler, rightIndex, seenBackend);

          // lookahead is clear => finished
          if (log.isDebugEnabled()) {
            log.debug("fixing postmixIndex: " + rightIndex);
          }
          postmixIndexHandler.confirmUnconfirmed(rightIndex);
          return rightIndex;
        } catch (PostmixIndexAlreadyUsedException e) {
          if (log.isDebugEnabled()) {
            log.debug("postmixIndex already used: " + rightIndex + " (lookahead already used)");
          }
        }
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
      IPostmixHandler postmixHandler, ISeenBackend seenBackend) throws Exception {
    IIndexHandler postmixIndexHandler = postmixHandler.getIndexHandler();

    int postmixIndex = 0;
    int incrementGap = 1;
    for (int i = 0; i < POSTMIX_INDEX_RANGE_ITERATIONS; i++) {
      int leftIndex = 0;
      try {
        // increment by incrementGap
        for (int x = 0; x < incrementGap; x++) {
          postmixIndex =
              ClientUtils.computeNextReceiveAddressIndex( // increments unconfirmed counter
                  postmixIndexHandler, config.getIndexRangePostmix());

          // set leftIndex
          if (x == 0) {
            leftIndex = postmixIndex;
          }
        }

        // check next output
        checkPostmixIndex(postmixHandler, postmixIndex, seenBackend);

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
        if (log.isDebugEnabled()) {
          log.debug("postmixIndex already used: " + postmixIndex);
        }

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
