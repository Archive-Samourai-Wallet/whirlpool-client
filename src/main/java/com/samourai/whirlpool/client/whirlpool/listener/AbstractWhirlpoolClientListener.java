package com.samourai.whirlpool.client.whirlpool.listener;

import com.samourai.whirlpool.client.mix.listener.*;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public abstract class AbstractWhirlpoolClientListener implements WhirlpoolClientListener {
  private WhirlpoolClientListener notifyListener;
  private Subject<MixProgress> observable;

  public AbstractWhirlpoolClientListener(WhirlpoolClientListener notifyListener) {
    this.notifyListener = notifyListener;
    this.observable = BehaviorSubject.create();
  }

  public AbstractWhirlpoolClientListener() {
    this(null);
  }

  @Override
  public void success(MixSuccess mixSuccess) {
    if (notifyListener != null) {
      notifyListener.success(mixSuccess);
    }
  }

  @Override
  public void fail(MixFail mixFail) {
    if (notifyListener != null) {
      notifyListener.fail(mixFail);
    }
  }

  @Override
  public void progress(MixProgress mixProgress) {
    if (notifyListener != null) {
      notifyListener.progress(mixProgress);
    }
  }

  public Subject<MixProgress> getObservable() {
    return observable;
  }
}
