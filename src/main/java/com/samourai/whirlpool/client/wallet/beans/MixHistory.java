package com.samourai.whirlpool.client.wallet.beans;

import com.google.common.collect.Lists;
import com.samourai.whirlpool.client.mix.MixParams;
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
  private int nbMixed;
  private int nbFailed;
  private LinkedList<MixResult> mixResults;
  private long mixedVolume;
  private Subject<MixHistory> observable;

  public MixHistory() {
    this.startupTime = System.currentTimeMillis();
    this.nbMixed = 0;
    this.nbFailed = 0;
    this.mixResults = new LinkedList<>();
    this.mixedVolume = 0;
    this.observable = BehaviorSubject.create();
  }

  public void onMixSuccess(MixParams mixParams, Utxo receiveUtxo) {
    this.nbMixed++;
    this.mixedVolume += mixParams.getDenomination();
    MixResult mixResult =
        new MixResult(
            mixParams.getPoolId(),
            mixParams.getDenomination(),
            mixParams.getWhirlpoolUtxo().getUtxo().value == mixParams.getDenomination(),
            receiveUtxo,
            mixParams.getPostmixHandler().getDestination());
    addMixResult(mixResult);
  }

  public void onMixFail(MixParams mixParams, MixFailReason failReason, String notifiableError) {
    this.nbFailed++;
    MixResult mixResult =
        new MixResult(
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

  public int getNbMixed() {
    return nbMixed;
  }

  public int getNbFailed() {
    return nbFailed;
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

  public long getMixedVolume() {
    return mixedVolume;
  }

  public Observable<MixHistory> getObservable() {
    return observable;
  }
}
