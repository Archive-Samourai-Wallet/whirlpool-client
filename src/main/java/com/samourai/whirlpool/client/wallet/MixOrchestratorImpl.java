package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.*;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.orchestrator.MixOrchestrator;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixOrchestratorImpl extends MixOrchestrator {
  private final Logger log = LoggerFactory.getLogger(MixOrchestratorImpl.class);

  private WhirlpoolDataService dataService;
  private WhirlpoolWallet whirlpoolWallet;
  private WhirlpoolClientConfig config;

  public MixOrchestratorImpl(
      MixingStateEditable mixingState,
      int loopDelay,
      WhirlpoolDataService dataService,
      WhirlpoolWallet whirlpoolWallet) {
    super(
        loopDelay,
        dataService.getConfig().getClientDelay(),
        new MixOrchestratorData(
            mixingState, dataService.getPoolSupplier(), whirlpoolWallet.getUtxoSupplier()),
        dataService.getConfig().getMaxClients(),
        dataService.getConfig().getMaxClientsPerPool(),
        dataService.getConfig().isAutoMix(),
        dataService.getConfig().getMixsTarget());
    this.dataService = dataService;
    this.whirlpoolWallet = whirlpoolWallet;
    this.config = dataService.getConfig();
  }

  @Override
  protected void onMixSuccess(WhirlpoolUtxo whirlpoolUtxo, MixSuccess mixSuccess) {
    super.onMixSuccess(whirlpoolUtxo, mixSuccess);
    whirlpoolWallet.onMixSuccess(whirlpoolUtxo, mixSuccess);
  }

  @Override
  protected void onMixFail(
      WhirlpoolUtxo whirlpoolUtxo, MixFailReason reason, String notifiableError) {
    super.onMixFail(whirlpoolUtxo, reason, notifiableError);
    whirlpoolWallet.onMixFail(whirlpoolUtxo, reason, notifiableError);
  }

  @Override
  protected WhirlpoolClient runWhirlpoolClient(
      WhirlpoolUtxo whirlpoolUtxo, WhirlpoolClientListener listener) throws NotifiableException {
    if (log.isDebugEnabled()) {
      log.info(
          " • Connecting client to pool: " + whirlpoolUtxo.getPoolId() + ", utxo=" + whirlpoolUtxo);
    } else {
      log.info(" • Connecting client to pool: " + whirlpoolUtxo.getPoolId());
    }

    // find pool
    String poolId = whirlpoolUtxo.getPoolId();
    Pool pool = dataService.getPoolSupplier().findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException("Pool not found: " + poolId);
    }

    // start mixing (whirlpoolClient will start a new thread)
    MixParams mixParams = computeMixParams(whirlpoolUtxo, pool);

    WhirlpoolClient whirlpoolClient = new WhirlpoolClientImpl(config);
    whirlpoolClient.whirlpool(mixParams, listener);
    return whirlpoolClient;
  }

  private MixParams computeMixParams(WhirlpoolUtxo whirlpoolUtxo, Pool pool) {
    IPremixHandler premixHandler = computePremixHandler(whirlpoolUtxo);
    IPostmixHandler postmixHandler = computePostmixHandler();
    return new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
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
    HD_Address premixAddress =
        whirlpoolWallet
            .getWalletSupplier()
            .getWallet(whirlpoolUtxo.getAccount())
            .getAddressAt(whirlpoolUtxo.getUtxo());
    ECKey premixKey = premixAddress.getECKey();

    UnspentResponse.UnspentOutput premixOrPostmixUtxo = whirlpoolUtxo.getUtxo();
    UtxoWithBalance utxoWithBalance =
        new UtxoWithBalance(
            premixOrPostmixUtxo.tx_hash,
            premixOrPostmixUtxo.tx_output_n,
            premixOrPostmixUtxo.value);

    // use PREMIX(0,0) as userPreHash (not transmitted to server but rehashed with another salt)
    HD_Address premix00 =
        whirlpoolWallet.getWalletSupplier().getWallet(WhirlpoolAccount.PREMIX).getAddressAt(0, 0);
    NetworkParameters params = config.getNetworkParameters();
    String premix00Bech32 = Bech32UtilGeneric.getInstance().toBech32(premix00, params);
    String userPreHash = ClientUtils.sha256Hash(premix00Bech32);

    return new PremixHandler(utxoWithBalance, premixKey, userPreHash);
  }

  private IPostmixHandler computePostmixHandler() {
    return new Bip84PostmixHandler(whirlpoolWallet.getWalletPostmix(), config.isMobile());
  }
}
