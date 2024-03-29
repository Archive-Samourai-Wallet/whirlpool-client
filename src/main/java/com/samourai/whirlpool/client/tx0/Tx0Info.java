package com.samourai.whirlpool.client.tx0;

import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0x2.Tx0x2Context;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;
import com.samourai.whirlpool.client.wallet.data.WhirlpoolInfo;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Info {
  private final Logger log = LoggerFactory.getLogger(Tx0Info.class);
  private WhirlpoolInfo whirlpoolInfo;
  private Tx0InfoConfig tx0InfoConfig;
  private Collection<Tx0Data> tx0Datas;

  public Tx0Info(
      WhirlpoolInfo whirlpoolInfo, Tx0InfoConfig tx0InfoConfig, Collection<Tx0Data> tx0Datas) {
    this.whirlpoolInfo = whirlpoolInfo;
    this.tx0InfoConfig = tx0InfoConfig;
    this.tx0Datas = tx0Datas;
  }

  public Tx0Previews tx0Previews(
      Tx0PreviewConfig tx0PreviewConfig, Collection<UnspentOutput> utxos) {
    // build Tx0Previews
    Tx0PreviewService tx0PreviewService = whirlpoolInfo.getTx0PreviewService();
    Map<String, Tx0Preview> tx0PreviewsByPoolId = new LinkedHashMap<String, Tx0Preview>();
    for (Tx0Data tx0Data : tx0Datas) {
      final String poolId = tx0Data.getPoolId();
      Pool pool = tx0InfoConfig.findPool(poolId);
      Tx0Param tx0Param = tx0PreviewService.getTx0Param(pool, tx0PreviewConfig);
      try {
        // real preview for outputs (with SCODE and outputs calculation)
        Tx0Preview tx0Preview = tx0PreviewService.tx0Preview(tx0Param, tx0Data, utxos);
        tx0PreviewsByPoolId.put(poolId, tx0Preview);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Pool not eligible for tx0: " + poolId, e.getMessage());
        }
      }
    }
    return new Tx0Previews(tx0PreviewsByPoolId);
  }

  public Tx0Previews tx0Previews(
      Collection<WhirlpoolUtxo> whirlpoolUtxos, Tx0PreviewConfig tx0PreviewConfig) {
    return tx0Previews(tx0PreviewConfig, WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos));
  }

  public List<Tx0> tx0Cascade(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Collection<Pool> pools,
      Tx0Config tx0Config)
      throws Exception {
    // adapt tx0Cascade() for WhirlpoolUtxo
    Callable<List<Tx0>> runTx0 =
        () ->
            tx0Cascade(
                walletSupplier,
                utxoSupplier,
                WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos),
                tx0Config,
                pools);
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0);
  }

  public List<Tx0> tx0Cascade(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<UnspentOutput> spendFroms,
      Tx0Config tx0Config,
      Collection<Pool> pools)
      throws Exception {

    // create TX0s
    Tx0Service tx0Service = whirlpoolInfo.getTx0Service();
    Tx0Previews tx0Previews = tx0Previews(tx0Config, spendFroms);
    List<Tx0> tx0List =
        tx0Service.tx0Cascade(
            spendFroms, walletSupplier, pools, tx0Config, utxoSupplier, tx0Previews);

    // broadcast each TX0
    int num = 1;
    for (Tx0 tx0 : tx0List) {
      if (log.isDebugEnabled()) {
        log.debug("Pushing Tx0 " + (num) + "/" + tx0List.size() + ": " + tx0);
      }
      // broadcast
      tx0Service.pushTx0WithRetryOnAddressReuse(tx0, walletSupplier, utxoSupplier, this);
      num++;
    }
    // refresh new utxos in background
    ClientUtils.refreshUtxosDelayAsync(utxoSupplier, whirlpoolInfo.getTx0Service().getParams())
        .subscribe();
    return tx0List;
  }

  public Tx0 tx0(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0Config tx0Config)
      throws Exception {
    // adapt tx0() for WhirlpoolUtxo
    Callable<Tx0> runTx0 =
        () ->
            tx0(
                walletSupplier,
                utxoSupplier,
                WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos),
                tx0Config,
                pool);
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0);
  }

  public Tx0 tx0(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<UnspentOutput> spendFroms,
      Tx0Config tx0Config,
      Pool pool)
      throws Exception {
    int initialPremixIndex =
        ClientUtils.getWalletPremix(walletSupplier).getIndexHandlerReceive().get();
    int initialChangeIndex =
        ClientUtils.getWalletDeposit(walletSupplier).getIndexHandlerChange().get();
    try {
      // create tx0
      Tx0Previews tx0Previews = tx0Previews(tx0Config, spendFroms);
      Tx0 tx0 =
          whirlpoolInfo
              .getTx0Service()
              .tx0(spendFroms, walletSupplier, pool, tx0Config, utxoSupplier, tx0Previews);

      // broadcast (or retry on address-reuse)
      whirlpoolInfo
          .getTx0Service()
          .pushTx0WithRetryOnAddressReuse(tx0, walletSupplier, utxoSupplier, this);
      // refresh new utxos in background
      ClientUtils.refreshUtxosDelayAsync(utxoSupplier, whirlpoolInfo.getTx0Service().getParams())
          .subscribe();
      return tx0;
    } catch (Exception e) {
      // revert index
      ClientUtils.getWalletPremix(walletSupplier)
          .getIndexHandlerReceive()
          .set(initialPremixIndex, true);
      ClientUtils.getWalletDeposit(walletSupplier)
          .getIndexHandlerChange()
          .set(initialChangeIndex, true);
      throw e;
    }
  }

  public Cahoots tx0x2(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<WhirlpoolUtxo> whirlpoolUtxos,
      Pool pool,
      Tx0Config tx0Config,
      PaymentCode paymentCodeCounterparty,
      SorobanWalletInitiator sorobanWalletInitiator)
      throws Exception {
    // adapt tx0() for WhirlpoolUtxo
    Callable<Cahoots> runTx0x2 =
        () ->
            tx0x2(
                walletSupplier,
                utxoSupplier,
                WhirlpoolUtxo.toUnspentOutputs(whirlpoolUtxos),
                tx0Config,
                pool,
                paymentCodeCounterparty,
                sorobanWalletInitiator);
    return handleUtxoStatusForTx0(whirlpoolUtxos, runTx0x2);
  }

  public Cahoots tx0x2(
      WalletSupplier walletSupplier,
      UtxoSupplier utxoSupplier,
      Collection<UnspentOutput> spendFroms,
      Tx0Config tx0Config,
      Pool pool,
      PaymentCode paymentCodeCounterparty,
      SorobanWalletInitiator sorobanWalletInitiator)
      throws Exception {
    // build initial TX0
    Tx0Service tx0Service = whirlpoolInfo.getTx0Service();
    Tx0Previews tx0Previews = tx0Previews(tx0Config, spendFroms);
    Tx0 tx0Initial =
        tx0Service.tx0(spendFroms, walletSupplier, pool, tx0Config, utxoSupplier, tx0Previews);

    // start Cahoots
    long minerFee =
        whirlpoolInfo.getMinerFeeSupplier().getFee(MinerFeeTarget.BLOCKS_4); // never used
    int account = 0; // never used
    Tx0x2Context tx0x2Context =
        Tx0x2Context.newInitiator(
            sorobanWalletInitiator.getCahootsWallet(),
            account,
            minerFee,
            whirlpoolInfo.getTx0Service(),
            tx0Initial);
    return sorobanWalletInitiator.meetAndInitiate(tx0x2Context, paymentCodeCounterparty);
  }

  public Tx0Config getTx0Config(Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget) {
    Tx0Config tx0Config =
        new Tx0Config(
            tx0FeeTarget,
            mixFeeTarget,
            SamouraiAccount.DEPOSIT,
            tx0InfoConfig.getTx0AttemptsAddressReuse(),
            tx0InfoConfig.getTx0AttemptsSoroban());
    return tx0Config;
  }

  private <T> T handleUtxoStatusForTx0(Collection<WhirlpoolUtxo> whirlpoolUtxos, Callable<T> runTx0)
      throws Exception {

    // verify utxos
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      // check status
      WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
      if (!WhirlpoolUtxoStatus.isTx0Possible(utxoStatus)) {
        throw new NotifiableException("Cannot Tx0: utxoStatus=" + utxoStatus);
      }
    }

    // set utxos status
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      whirlpoolUtxo.getUtxoState().setStatus(WhirlpoolUtxoStatus.TX0, true, true);
    }
    try {
      // run
      T tx0 = runTx0.call();

      // success
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        utxoState.setStatus(WhirlpoolUtxoStatus.TX0_SUCCESS, true, true);
      }

      return tx0;
    } catch (Exception e) {
      // error
      for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
        WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
        String error = NotifiableException.computeNotifiableException(e).getMessage();
        utxoState.setStatusError(WhirlpoolUtxoStatus.TX0_FAILED, error);
      }
      throw e;
    }
  }

  public Tx0InfoConfig getTx0DataConfig() {
    return tx0InfoConfig;
  }

  public WhirlpoolInfo getWhirlpoolInfo() {
    return whirlpoolInfo;
  }

  @Override
  public String toString() {
    return "tx0Datas={" + tx0Datas.toString() + "}";
  }
}
