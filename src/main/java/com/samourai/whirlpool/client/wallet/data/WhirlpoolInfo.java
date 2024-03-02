package com.samourai.whirlpool.client.wallet.data;

import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.tx0.Tx0Previews;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.coordinator.ExpirableCoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.RpcSessionClient;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.SorobanAppWhirlpool;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import io.reactivex.Single;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolInfo {
  private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

  protected final Tx0PreviewService tx0PreviewService;
  private SorobanAppWhirlpool sorobanAppWhirlpool;
  protected final CoordinatorSupplier coordinatorSupplier;
  private RpcClientService rpcClientService;

  public WhirlpoolInfo(
      Tx0PreviewService tx0PreviewService,
      SorobanAppWhirlpool sorobanAppWhirlpool,
      CoordinatorSupplier coordinatorSupplier,
      RpcClientService rpcClientService) {
    this.tx0PreviewService = tx0PreviewService;
    this.rpcClientService = rpcClientService;
    this.sorobanAppWhirlpool = sorobanAppWhirlpool;
    this.coordinatorSupplier = coordinatorSupplier;
  }

  public WhirlpoolInfo(
      MinerFeeSupplier minerFeeSupplier, WhirlpoolWalletConfig whirlpoolWalletConfig) {
    this.tx0PreviewService = computeTx0PreviewService(minerFeeSupplier, whirlpoolWalletConfig);
    this.rpcClientService =
        whirlpoolWalletConfig.getSorobanWalletService().getSorobanService().getRpcClientService();
    this.sorobanAppWhirlpool = new SorobanAppWhirlpool(whirlpoolWalletConfig.getWhirlpoolNetwork());
    this.coordinatorSupplier =
        computeCoordinatorSupplier(
            whirlpoolWalletConfig, tx0PreviewService, rpcClientService, sorobanAppWhirlpool);
  }

  protected Tx0PreviewService computeTx0PreviewService(
      MinerFeeSupplier minerFeeSupplier, WhirlpoolWalletConfig whirlpoolWalletConfig) {
    return new Tx0PreviewService(minerFeeSupplier, whirlpoolWalletConfig);
  }

  protected ExpirableCoordinatorSupplier computeCoordinatorSupplier(
      WhirlpoolWalletConfig config,
      Tx0PreviewService tx0PreviewService,
      RpcClientService rpcClientService,
      SorobanAppWhirlpool sorobanAppWhirlpool) {
    RpcSessionClient rpcSession =
        new RpcSessionClient(rpcClientService.generateRpcWallet().createRpcSession());
    if (log.isDebugEnabled()) {
      String sender = rpcSession.getRpcWallet().getBip47Account().getPaymentCode().toString();
      log.debug("New soroban identity for CoordinatorSupplier: sender=" + sender);
    }
    // specific whirlpoolApiClient for coordinatorSupplier (without soroban nodes filter)
    WhirlpoolApiClient whirlpoolApiClient = new WhirlpoolApiClient(rpcSession, sorobanAppWhirlpool);
    return new ExpirableCoordinatorSupplier(
        config.getRefreshPoolsDelay(), whirlpoolApiClient, tx0PreviewService, config);
  }

  public WhirlpoolApiClient createWhirlpoolApiClient() {
    RpcSessionClient rpcSession =
        new RpcSessionClient(rpcClientService.generateRpcWallet().createRpcSession());
    Collection<Coordinator> coordinators = coordinatorSupplier.getCoordinators(); // TODO
    if (!coordinators.isEmpty()) {
      rpcSession.setCoordinator(coordinators.iterator().next());
    }
    return new WhirlpoolApiClient(rpcSession, sorobanAppWhirlpool);
  }

  // used by Sparrow
  public Single<Tx0Previews> tx0Previews(Tx0Config tx0Config, Collection<UnspentOutput> utxos) {
    WhirlpoolApiClient whirlpoolApiClient = createWhirlpoolApiClient();
    return tx0PreviewService.tx0Previews(tx0Config, utxos, whirlpoolApiClient, coordinatorSupplier);
  }

  public Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
  }

  public CoordinatorSupplier getCoordinatorSupplier() {
    return coordinatorSupplier;
  }

  public SorobanAppWhirlpool getSorobanAppWhirlpool() {
    return sorobanAppWhirlpool;
  }
}
