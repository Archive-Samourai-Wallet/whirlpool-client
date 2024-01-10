package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.util.RandomUtil;
import com.samourai.whirlpool.client.test.AbstractTest;
import java.util.Arrays;
import java.util.LinkedList;
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
}
