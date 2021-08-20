package com.samourai.whirlpool.client.mix.listener;

public interface MixClientListener {
  void success(MixSuccess mixSuccess);

  void fail(MixFail mixFail);

  void progress(MixProgress mixProgress);
}
