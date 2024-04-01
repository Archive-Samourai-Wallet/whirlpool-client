package com.samourai.whirlpool.client.wallet.orchestrator;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.*;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixOrchestratorImpl extends MixOrchestrator {
  private final Logger log = LoggerFactory.getLogger(MixOrchestratorImpl.class);

  private final WhirlpoolWallet whirlpoolWallet;
  private final WhirlpoolClientConfig config;
  private final PoolSupplier poolSupplier;

  public MixOrchestratorImpl(
      MixingStateEditable mixingState,
      int loopDelay,
      WhirlpoolWalletConfig config,
      PoolSupplier poolSupplier,
      WhirlpoolWallet whirlpoolWallet) {
    super(
        loopDelay,
        config.getClientDelay(),
        new MixOrchestratorData(
            mixingState,
            poolSupplier,
            whirlpoolWallet.getUtxoSupplier(),
            whirlpoolWallet.getChainSupplier()),
        config.getMaxClients(),
        config.getMaxClientsPerPool(),
        config.getExtraLiquidityClientsPerPool(),
        config.isAutoMix());
    this.whirlpoolWallet = whirlpoolWallet;
    this.config = config;
    this.poolSupplier = poolSupplier;
  }

  @Override
  protected WhirlpoolClientListener computeMixListener(final MixParams mixParams) {
    final WhirlpoolClientListener orchestratorListener = super.computeMixListener(mixParams);
    final WhirlpoolUtxo whirlpoolUtxo = mixParams.getWhirlpoolUtxo();

    return new WhirlpoolClientListener() {
      @Override
      public void success(String mixId, Utxo receiveUtxo, MixDestination receiveDestination) {
        // update utxo
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatusMixing(
            WhirlpoolUtxoStatus.MIX_SUCCESS, true, mixParams, MixStep.SUCCESS, mixId);

        // remove from mixings
        orchestratorListener.success(mixId, receiveUtxo, receiveDestination);

        // notify
        whirlpoolWallet.onMixSuccess(mixParams, receiveUtxo, receiveDestination);
      }

      @Override
      public void fail(String mixId, MixFailReason reason, String notifiableError) {
        // update utxo
        String error = reason.getMessage();
        if (notifiableError != null) {
          error += " ; " + notifiableError;
        }
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        if (reason.isError()) {
          // mix failure
          utxoState.setStatusMixingError(WhirlpoolUtxoStatus.MIX_FAILED, mixParams, mixId, error);
        } else if (reason.isRequeue()) {
          // requeue
          utxoState.setStatus(WhirlpoolUtxoStatus.READY, false, false);
        } else {
          // stop
          utxoState.setStatus(WhirlpoolUtxoStatus.STOP, false, false);
        }

        // remove from mixings
        orchestratorListener.fail(mixId, reason, notifiableError);

        // notify & re-add to mixQueue...
        whirlpoolWallet.onMixFail(mixParams, reason, notifiableError);
      }

      @Override
      public void progress(String mixId, MixStep mixStep) {
        // update utxo
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatusMixing(utxoState.getStatus(), true, mixParams, mixStep, mixId);

        // manage orchestrator
        orchestratorListener.progress(mixId, mixStep);

        // notify
        whirlpoolWallet.onMixProgress(mixParams);
      }
    };
  }

  @Override
  protected WhirlpoolClient runWhirlpoolClient(WhirlpoolUtxo whirlpoolUtxo)
      throws NotifiableException {
    String poolId = whirlpoolUtxo.getUtxoState().getPoolId();
    String utxoName = whirlpoolUtxo.getUtxo().getUtxoName();
    if (log.isDebugEnabled()) {
      log.info(" • Registering " + utxoName + " (" + poolId + "): " + whirlpoolUtxo);
    } else {
      log.info(" • Registering " + utxoName + " (" + poolId + ")");
    }

    // find pool
    Pool pool = poolSupplier.findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException("Pool not found: " + poolId);
    }

    // prepare mixing
    MixParams mixParams = computeMixParams(whirlpoolUtxo, pool);
    WhirlpoolClientListener mixListener = computeMixListener(mixParams);
    mixListener.progress(null, MixStep.REGISTER_INPUT);

    // start mixing (whirlpoolClient will start a new thread)
    WhirlpoolClient whirlpoolClient = new WhirlpoolClientImpl(config);
    whirlpoolClient.whirlpool(mixParams, mixListener);
    return whirlpoolClient;
  }

  private MixParams computeMixParams(WhirlpoolUtxo whirlpoolUtxo, Pool pool) {
    IPremixHandler premixHandler = computePremixHandler(whirlpoolUtxo);
    IPostmixHandler postmixHandler = whirlpoolWallet.computePostmixHandler(whirlpoolUtxo);
    ChainSupplier chainSupplier = whirlpoolWallet.getChainSupplier();
    CoordinatorSupplier coordinatorSupplier = whirlpoolWallet.getCoordinatorSupplier();

    // generate temporary Soroban identity
    WhirlpoolApiClient whirlpoolApiClient = whirlpoolWallet.createWhirlpoolApiClient();
    return new MixParams(
        pool,
        whirlpoolUtxo,
        premixHandler,
        postmixHandler,
        chainSupplier,
        coordinatorSupplier,
        whirlpoolApiClient);
  }

  @Override
  protected void stopWhirlpoolClient(final Mixing mixing, final MixFailReason failReason) {
    super.stopWhirlpoolClient(mixing, failReason);

    // stop in new thread for faster response
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                mixing.getWhirlpoolClient().stop(failReason);

                if (failReason.isRequeue()) {
                  try {
                    mixQueue(mixing.getUtxo(), false);
                  } catch (Exception e) {
                    log.error("", e);
                  }
                }
              }
            },
            "stop-whirlpoolClient")
        .start();
  }

  private IPremixHandler computePremixHandler(WhirlpoolUtxo whirlpoolUtxo) {
    ECKey premixKey = whirlpoolUtxo.getECKey();

    UnspentOutput premixOrPostmixUtxo = whirlpoolUtxo.getUtxo();
    UtxoWithBalance utxoWithBalance =
        new UtxoWithBalance(
            premixOrPostmixUtxo.tx_hash,
            premixOrPostmixUtxo.tx_output_n,
            premixOrPostmixUtxo.value);

    // use PREMIX(0,0) as userPreHash (not transmitted to server but rehashed with another salt)
    String premix00Bech32 = whirlpoolWallet.getWalletPremix().getAddressAt(0, 0).getAddressString();
    String userPreHash = ClientUtils.sha256Hash(premix00Bech32);

    return new PremixHandler(utxoWithBalance, premixKey, userPreHash);
  }
}
