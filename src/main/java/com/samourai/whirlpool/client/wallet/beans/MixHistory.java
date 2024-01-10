package com.samourai.whirlpool.client.wallet.beans;

import com.google.common.collect.Lists;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.DestinationType;
import com.samourai.whirlpool.client.mix.handler.MixDestination;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.protocol.beans.Utxo;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.LinkedList;
import java.util.List;

public class MixHistory {
  private static final int LIMIT = 1000;

  private long startupTime;
  private int mixedCount;
  private int failedCount;
  private Long mixedLastTime;
  private Long failedLastTime;
  private LinkedList<MixResult> mixResults;
  private LinkedList<MixResult> mixResultsExternalXpub; // unlimited history
  private long mixedVolume;
  private int externalXpubCount;
  private Long externalXpubLastTime;
  private long externalXpubVolume;
  private Subject<MixHistory> observable;

  public MixHistory() {
    this.startupTime = System.currentTimeMillis();
    this.mixedCount = 0;
    this.failedCount = 0;
    this.mixedLastTime = null;
    this.failedLastTime = null;
    this.mixResults = new LinkedList<>();
    this.mixResultsExternalXpub = new LinkedList<>();
    this.mixedVolume = 0;
    this.externalXpubCount = 0;
    this.externalXpubLastTime = null;
    this.externalXpubVolume = 0;
    this.observable = BehaviorSubject.create();
  }

  public void onMixSuccess(
      MixParams mixParams, Utxo receiveUtxo, MixDestination receiveDestination) {
    long now = System.currentTimeMillis();
    this.mixedCount++;
    this.mixedLastTime = now;
    this.mixedVolume += mixParams.getDenomination();
    MixResult mixResult =
        new MixResult(
            now,
            mixParams.getPoolId(),
            mixParams.getDenomination(),
            mixParams.getWhirlpoolUtxo().getUtxo().value == mixParams.getDenomination(),
            receiveUtxo,
            receiveDestination);
    addMixResult(mixResult);
    if (DestinationType.XPUB.equals(receiveDestination.getType())) {
      externalXpubCount++;
      externalXpubLastTime = now;
      externalXpubVolume += mixParams.getDenomination();
      mixResultsExternalXpub.add(mixResult); // unlimited history
    }
  }

  public void onMixFail(MixParams mixParams, MixFailReason failReason, String notifiableError) {
    long now = System.currentTimeMillis();
    this.failedCount++;
    this.failedLastTime = now;
    MixResult mixResult =
        new MixResult(
            now,
            mixParams.getPoolId(),
            mixParams.getDenomination(),
            mixParams.getWhirlpoolUtxo().getUtxo().value == mixParams.getDenomination(),
            mixParams.getPremixHandler().getUtxo(),
            failReason,
            notifiableError);
    addMixResult(mixResult);
  }

  protected synchronized void addMixResult(MixResult mixResult) {
    this.mixResults.add(mixResult);
    if (mixResults.size() > LIMIT) {
      mixResults.removeFirst();
    }
    emit();
  }

  protected void emit() {
    // notify
    observable.onNext(this);
  }

  public long getStartupTime() {
    return startupTime;
  }

  public int getMixedCount() {
    return mixedCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public Long getMixedLastTime() {
    return mixedLastTime;
  }

  public Long getFailedLastTime() {
    return failedLastTime;
  }

  public List<MixResult> getMixResultsDesc(int limit) {
    return Lists.reverse(ClientUtils.subListLastItems(mixResults, limit));
  }

  public List<MixResult> getMixResultsDesc() {
    return Lists.reverse(mixResults);
  }

  public List<MixResult> getMixResults() {
    return mixResults;
  }

  public LinkedList<MixResult> getMixResultsExternalXpub() {
    return mixResultsExternalXpub;
  }

  public List<MixResult> getMixResultsExternalXpubDesc() {
    return Lists.reverse(mixResultsExternalXpub);
  }

  public List<MixResult> getMixResultsExternalXpubDesc(int limit) {
    return Lists.reverse(ClientUtils.subListLastItems(mixResultsExternalXpub, limit));
  }

  public long getMixedVolume() {
    return mixedVolume;
  }

  public int getExternalXpubCount() {
    return externalXpubCount;
  }

  public Long getExternalXpubLastTime() {
    return externalXpubLastTime;
  }

  public long getExternalXpubVolume() {
    return externalXpubVolume;
  }

  public Observable<MixHistory> getObservable() {
    return observable;
  }
}
