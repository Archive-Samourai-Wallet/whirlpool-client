package com.samourai.whirlpool.client.wallet.orchestrator;

import com.samourai.soroban.client.rpc.RpcSession;
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
      public void success(Utxo receiveUtxo) {
        // update utxo
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatusMixing(
            WhirlpoolUtxoStatus.MIX_SUCCESS, true, mixParams, MixStep.SUCCESS);

        // remove from mixings
        orchestratorListener.success(receiveUtxo);

        // notify
        whirlpoolWallet.onMixSuccess(mixParams, receiveUtxo);
      }

      @Override
      public void fail(MixFailReason reason, String notifiableError) {
        // update utxo
        String error = reason.getMessage();
        if (notifiableError != null) {
          error += " ; " + notifiableError;
        }
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        if (reason == MixFailReason.STOP) {
          // silent stop
          utxoState.setStatus(WhirlpoolUtxoStatus.STOP, false, false);
        } else if (reason == MixFailReason.CANCEL) {
          // silent cancel
          utxoState.setStatus(WhirlpoolUtxoStatus.READY, false, false);
        } else {
          // mix failure
          utxoState.setStatusMixingError(WhirlpoolUtxoStatus.MIX_FAILED, mixParams, error);
        }

        // remove from mixings
        orchestratorListener.fail(reason, notifiableError);

        // notify & re-add to mixQueue...
        whirlpoolWallet.onMixFail(mixParams, reason, notifiableError);
      }

      @Override
      public void progress(MixStep mixStep) {
        // update utxo
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatusMixing(utxoState.getStatus(), true, mixParams, mixStep);

        // manage orchestrator
        orchestratorListener.progress(mixStep);

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
    mixListener.progress(MixStep.REGISTERING_INPUT);

    // start mixing (whirlpoolClient will start a new thread)
    WhirlpoolClient whirlpoolClient = new WhirlpoolClientImpl(config);
    whirlpoolClient.whirlpool(mixParams, mixListener);
    return whirlpoolClient;
  }

  private MixParams computeMixParams(WhirlpoolUtxo whirlpoolUtxo, Pool pool) {
    IPremixHandler premixHandler = computePremixHandler(whirlpoolUtxo);
    IPostmixHandler postmixHandler = computePostmixHandler(whirlpoolUtxo);
    ChainSupplier chainSupplier = whirlpoolWallet.getChainSupplier();
    CoordinatorSupplier coordinatorSupplier = whirlpoolWallet.getCoordinatorSupplier();

    // generate temporary Soroban identity
    RpcSession rpcSession = config.getRpcClientService().generateRpcWallet().createRpcSession();
    return new MixParams(
        pool,
        whirlpoolUtxo,
        premixHandler,
        postmixHandler,
        chainSupplier,
        coordinatorSupplier,
        rpcSession);
  }

  @Override
  protected void stopWhirlpoolClient(
      final Mixing mixing, final boolean cancel, final boolean reQueue) {
    super.stopWhirlpoolClient(mixing, cancel, reQueue);

    // stop in new thread for faster response
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                mixing.getWhirlpoolClient().stop(cancel);

                if (reQueue) {
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
    ECKey premixKey = whirlpoolUtxo.getBipAddress().getHdAddress().getECKey();

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

  private IPostmixHandler computePostmixHandler(WhirlpoolUtxo whirlpoolUtxo) {
    ExternalDestination externalDestination = config.getExternalDestination();
    if (externalDestination != null) {
      int nextMixsDone = whirlpoolUtxo.getMixsDone() + 1;
      if (nextMixsDone >= externalDestination.getMixs()) {
        // random factor for privacy
        if (externalDestination.useRandomDelay()) {
          if (log.isDebugEnabled()) {
            log.debug(
                "Mixing to POSTMIX, external destination randomly delayed for better privacy ("
                    + whirlpoolUtxo
                    + ")");
          }
        } else {
          if (log.isDebugEnabled()) {
            log.debug("Mixing to EXTERNAL (" + whirlpoolUtxo + ")");
          }
          if (externalDestination.getPostmixHandler() != null) {
            return externalDestination.getPostmixHandler();
          }
          return new XPubPostmixHandler(
              whirlpoolWallet.getWalletStateSupplier().getIndexHandlerExternal(),
              config.getWhirlpoolNetwork().getParams(),
              externalDestination.getXpub(),
              externalDestination.getChain(),
              externalDestination.getStartIndex());
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug(
              "Mixing to POSTMIX, mix "
                  + nextMixsDone
                  + "/"
                  + externalDestination.getMixs()
                  + " before external destination ("
                  + whirlpoolUtxo
                  + ")");
        }
      }
    }
    return new Bip84PostmixHandler(
        config.getWhirlpoolNetwork().getParams(),
        whirlpoolWallet.getWalletPostmix(),
        config.getIndexRangePostmix());
  }
}
