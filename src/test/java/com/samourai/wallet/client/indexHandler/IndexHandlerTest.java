package com.samourai.wallet.client.indexHandler;

import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class IndexHandlerTest extends AbstractTest {
  private MemoryIndexHandler indexHandler;

  public IndexHandlerTest() throws Exception {
    indexHandler = new MemoryIndexHandler();
  }

  @Test
  public void getAndSet() throws Exception {
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(0, indexHandler.get());

    Assert.assertEquals(0, indexHandler.getAndIncrement());
    Assert.assertEquals(1, indexHandler.getAndIncrement());
    Assert.assertEquals(2, indexHandler.getAndIncrement());

    indexHandler.set(5);
    Assert.assertEquals(5, indexHandler.getAndIncrement());
    Assert.assertEquals(6, indexHandler.getAndIncrement());
  }

  @Test
  public void getAndSetUnconfirmed() throws Exception {
    Assert.assertEquals(0, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(0, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());

    Assert.assertEquals(0, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(1, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(2, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(3, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(4, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(5, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    Assert.assertEquals(6, indexHandler.getUnconfirmed());

    indexHandler.cancelUnconfirmed(5);
    Assert.assertEquals(5, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    indexHandler.cancelUnconfirmed(4);
    Assert.assertEquals(4, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    indexHandler.cancelUnconfirmed(1);
    Assert.assertEquals(4, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    indexHandler.cancelUnconfirmed(2);
    Assert.assertEquals(4, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());
    indexHandler.cancelUnconfirmed(3);
    Assert.assertEquals(1, indexHandler.getUnconfirmed());
    Assert.assertEquals(0, indexHandler.get());

    Assert.assertEquals(0, indexHandler.getAndIncrement());
    Assert.assertEquals(1, indexHandler.getAndIncrement());
    Assert.assertEquals(2, indexHandler.getAndIncrement());

    indexHandler.set(5);
    Assert.assertEquals(5, indexHandler.getAndIncrement());
    Assert.assertEquals(6, indexHandler.getAndIncrement());
    Assert.assertEquals(7, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(8, indexHandler.getUnconfirmed());
    Assert.assertEquals(7, indexHandler.get());

    indexHandler.cancelUnconfirmed(7);
    Assert.assertEquals(7, indexHandler.getUnconfirmed());
    Assert.assertEquals(7, indexHandler.get());

    Assert.assertEquals(7, indexHandler.getAndIncrementUnconfirmed());

    Assert.assertEquals(8, indexHandler.getAndIncrementUnconfirmed());

    Assert.assertEquals(9, indexHandler.getAndIncrementUnconfirmed());

    Assert.assertEquals(10, indexHandler.getAndIncrementUnconfirmed());

    indexHandler.confirmUnconfirmed(10);
    Assert.assertEquals(11, indexHandler.getAndIncrementUnconfirmed());
    Assert.assertEquals(11, indexHandler.get());
  }
}
