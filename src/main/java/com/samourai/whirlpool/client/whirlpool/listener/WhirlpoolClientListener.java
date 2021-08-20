package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.listener.*;
import io.reactivex.subjects.Subject;

public interface WhirlpoolClientListener {
  void success(MixSuccess mixSuccess);

  void fail(MixFail mixFail);

  void progress(MixProgress mixProgress);

  Subject<MixProgress> getObservable();
}
