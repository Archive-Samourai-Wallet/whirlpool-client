package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.client.indexHandler.IIndexHandler;

public interface IPostmixHandler {
  MixDestination computeDestinationNext() throws Exception;

  MixDestination computeDestination(int index) throws Exception;

  void onRegisterOutput();

  void onMixFail();

  IIndexHandler getIndexHandler();
}
