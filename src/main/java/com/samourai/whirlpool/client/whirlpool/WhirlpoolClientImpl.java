package com.samourai.whirlpool.client.whirlpool;

import com.samourai.http.client.HttpException;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.MixClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.listener.MixClientListener;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.PoolInfo;
import com.samourai.whirlpool.protocol.rest.PoolsResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolClientImpl implements WhirlpoolClient {
  private Logger log = LoggerFactory.getLogger(WhirlpoolClientImpl.class);

  private WhirlpoolClientConfig config;

  private int mixs;
  private int doneMixs;
  private boolean done;
  private String logPrefix;

  private List<MixClient> mixClients;
  private Thread mixThread;
  private WhirlpoolClientListener listener;

  /**
   * Get a new Whirlpool client.
   *
   * @param config client configuration (server...)
   * @return
   */
  public static WhirlpoolClient newClient(WhirlpoolClientConfig config) {
    return new WhirlpoolClientImpl(config);
  }

  private WhirlpoolClientImpl(WhirlpoolClientConfig config) {
    this.config = config;
    this.logPrefix = null;
    if (log.isDebugEnabled()) {
      log.debug("protocolVersion=" + WhirlpoolProtocol.PROTOCOL_VERSION);
    }
  }

  @Override
  public Pools fetchPools() throws HttpException, NotifiableException {
    String url = WhirlpoolProtocol.getUrlFetchPools(config.getServer(), config.isSsl());
    try {
      PoolsResponse poolsResponse = config.getHttpClient().parseJson(url, PoolsResponse.class);
      return computePools(poolsResponse);
    } catch (HttpException e) {
      String restErrorResponseMessage = ClientUtils.parseRestErrorMessage(e);
      if (restErrorResponseMessage != null) {
        throw new NotifiableException(restErrorResponseMessage);
      }
      throw e;
    }
  }

  private Pools computePools(PoolsResponse poolsResponse) {
    List<Pool> listPools = new ArrayList<Pool>();
    for (PoolInfo poolInfo : poolsResponse.pools) {
      Pool pool = new Pool();
      pool.setPoolId(poolInfo.poolId);
      pool.setDenomination(poolInfo.denomination);
      pool.setMinerFeeMin(poolInfo.minerFeeMin);
      pool.setMinerFeeMax(poolInfo.minerFeeMax);
      pool.setMinAnonymitySet(poolInfo.minAnonymitySet);
      pool.setNbRegistered(poolInfo.nbRegistered);

      pool.setMixAnonymitySet(poolInfo.mixAnonymitySet);
      pool.setMixStatus(poolInfo.mixStatus);
      pool.setElapsedTime(poolInfo.elapsedTime);
      pool.setMixNbConfirmed(poolInfo.mixNbConfirmed);
      listPools.add(pool);
    }
    Pools pools = new Pools(listPools, poolsResponse.feePaymentCode);
    return pools;
  }

  @Override
  public void whirlpool(final MixParams mixParams, int mixs, WhirlpoolClientListener listener) {
    this.mixs = mixs;
    this.listener = listener;
    this.doneMixs = 0;
    this.mixClients = new ArrayList<MixClient>();

    this.mixThread =
        new Thread(
            new Runnable() {
              @Override
              public synchronized void run() {
                runClient(mixParams);
                while (!done) {
                  try {
                    wait();
                  } catch (Exception e) {
                  }
                }
              }
            });
    this.mixThread.start();
  }

  private MixClient runClient(MixParams mixParams) {
    MixClientListener mixListener = computeMixListener();

    MixClient mixClient = new MixClient(config);
    if (logPrefix != null) {
      int mix = this.mixClients.size();
      mixClient.setLogPrefix(logPrefix + ":" + (mix + 1));
    }
    mixClient.whirlpool(mixParams, mixListener);
    this.mixClients.add(mixClient);
    return mixClient;
  }

  private MixClient getLastWhirlpoolClient() {
    return mixClients.get(mixClients.size() - 1);
  }

  private MixClientListener computeMixListener() {
    return new MixClientListener() {
      @Override
      public void success(MixSuccess mixSuccess, MixParams nextMixParams) {
        listener.mixSuccess(doneMixs + 1, mixs, mixSuccess);

        doneMixs++;
        if (doneMixs == mixs) {
          // all mixs done
          listener.success(mixs, mixSuccess);
          endMixThread();
        } else {
          // go to next mix
          runClient(nextMixParams);
        }
      }

      @Override
      public void fail() {
        listener.fail(doneMixs + 1, mixs);
        endMixThread();
      }

      @Override
      public void progress(MixStep step, String stepInfo, int stepNumber, int nbSteps) {
        listener.progress(doneMixs + 1, mixs, step, stepInfo, stepNumber, nbSteps);
      }
    };
  }

  private void endMixThread() {
    synchronized (mixThread) {
      done = true;
      mixThread.notify();
    }
  }

  @Override
  public void exit() {
    MixClient mixClient = getLastWhirlpoolClient();
    if (mixClient != null) {
      mixClient.exit();
    }
  }

  public void setLogPrefix(String logPrefix) {
    this.logPrefix = logPrefix;
  }

  public MixClient getMixClient(int mix) {
    return mixClients.get(mix - 1);
  }
}
