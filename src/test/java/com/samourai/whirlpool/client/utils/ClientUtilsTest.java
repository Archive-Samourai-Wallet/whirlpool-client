package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.httpClient.HttpNetworkException;
import com.samourai.wallet.httpClient.HttpResponseException;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.test.AbstractTest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
  public void subListLastItems() {
    LinkedList<Integer> list = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5));
    Assertions.assertEquals(Arrays.asList(5), ClientUtils.subListLastItems(list, 1));
    Assertions.assertEquals(Arrays.asList(4, 5), ClientUtils.subListLastItems(list, 2));
    Assertions.assertEquals(Arrays.asList(2, 3, 4, 5), ClientUtils.subListLastItems(list, 4));
    Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), ClientUtils.subListLastItems(list, 5));
    Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), ClientUtils.subListLastItems(list, 10));
  }

  @Test
  public void loopHttpAttempts_success() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);
    boolean result =
        ClientUtils.loopHttpAttempts(
            3,
            () -> {
              attempts.incrementAndGet();
              if (attempts.get() == 1) {
                throw new HttpNetworkException("test"); // should retry
              } else if (attempts.get() == 2) {
                throw new TimeoutException(); // should retry
              }
              return true;
            });
    Assertions.assertTrue(result);
    Assertions.assertEquals(3, attempts.get());
  }

  @Test
  public void loopHttpAttempts_failure() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);
    Assertions.assertThrows(
        HttpResponseException.class,
        () ->
            ClientUtils.loopHttpAttempts(
                3,
                () -> {
                  attempts.incrementAndGet();
                  if (attempts.get() == 1) {
                    throw new HttpResponseException("test", 200); // should stop
                  }
                  return true;
                }));
    Assertions.assertEquals(1, attempts.get());
  }
}
