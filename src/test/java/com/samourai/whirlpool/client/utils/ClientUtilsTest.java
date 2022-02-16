package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.test.AbstractTest;
import io.reactivex.functions.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientUtilsTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(ClientUtilsTest.class);

  private int counter;

  public ClientUtilsTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setUp() {
    this.counter = 0;
  }

  @Test
  public void random() throws Exception {
    for (int i = 0; i < 10; i++) {
      doRandom();
    }
  }

  private int doRandom() {
    int rand = RandomUtil.getInstance().random(-1, 1);
    if (log.isDebugEnabled()) {
      log.debug("rand=" + rand);
    }
    Assertions.assertTrue(rand >= -1 && rand <= 1);
    return rand;
  }

  @Test
  public void runAsync() {
    this.counter = 0;
    Action action;
    action =
        () -> {
          counter++;
          log.info("running testAsync => counter=" + counter);
        };

    // simple run
    ClientUtils.runAsync(action, "testAsync").blockingAwait();
    Assertions.assertEquals(1, counter);

    // with doOnComplete
    ClientUtils.runAsync(action, "testAsync")
        .doOnComplete(
            () -> {
              Assertions.assertEquals(2, counter);
              counter++;
              log.info("doOnComplete => counter=" + counter);
            })
        .doOnComplete(
            () -> {
              Assertions.assertEquals(3, counter);
              counter++;
              log.info("doOnComplete2 => counter=" + counter);
            })
        .blockingAwait();
    Assertions.assertEquals(4, counter);
  }
}
