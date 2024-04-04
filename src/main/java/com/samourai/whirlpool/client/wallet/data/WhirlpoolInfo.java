package com.samourai.whirlpool.client.wallet.data;

import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.whirlpool.client.tx0.Tx0Info;
import com.samourai.whirlpool.client.tx0.Tx0InfoConfig;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.coordinator.ExpirableCoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.whirlpool.RpcSessionClient;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Tx0Data;
import com.samourai.whirlpool.protocol.SorobanAppWhirlpool;
import com.samourai.whirlpool.protocol.soroban.WhirlpoolApiClient;
import com.samourai.whirlpool.protocol.soroban.payload.tx0.Tx0DataRequest;
import com.samourai.whirlpool.protocol.soroban.payload.tx0.Tx0DataResponse;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolInfo {
  private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
  private final WhirlpoolWalletConfig config;

  protected final Tx0PreviewService tx0PreviewService;
  private Tx0Service tx0Service;
  private SorobanAppWhirlpool sorobanAppWhirlpool;
  protected final CoordinatorSupplier coordinatorSupplier;

  public WhirlpoolInfo(MinerFeeSupplier minerFeeSupplier, WhirlpoolWalletConfig config) {
    this.config = config;
    this.tx0PreviewService = computeTx0PreviewService(minerFeeSupplier, config);
    this.tx0Service = computeTx0Service(config, tx0PreviewService);
    this.sorobanAppWhirlpool = new SorobanAppWhirlpool(config.getSorobanConfig());
    this.coordinatorSupplier =
        computeCoordinatorSupplier(config, tx0PreviewService, sorobanAppWhirlpool);
  }

  protected Tx0PreviewService computeTx0PreviewService(
      MinerFeeSupplier minerFeeSupplier, WhirlpoolWalletConfig whirlpoolWalletConfig) {
    return new Tx0PreviewService(minerFeeSupplier, whirlpoolWalletConfig);
  }

  protected Tx0Service computeTx0Service(
      WhirlpoolWalletConfig whirlpoolWalletConfig, Tx0PreviewService tx0PreviewService) {
    return new Tx0Service(
        whirlpoolWalletConfig.getSamouraiNetwork().getParams(),
        tx0PreviewService,
        whirlpoolWalletConfig.getFeeOpReturnImpl());
  }

  protected ExpirableCoordinatorSupplier computeCoordinatorSupplier(
      WhirlpoolWalletConfig config,
      Tx0PreviewService tx0PreviewService,
      SorobanAppWhirlpool sorobanAppWhirlpool) {
    RpcClientService rpcClientService = config.getSorobanConfig().getRpcClientService();
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
    RpcClientService rpcClientService = config.getSorobanConfig().getRpcClientService();
    RpcSessionClient rpcSession =
        new RpcSessionClient(rpcClientService.generateRpcWallet().createRpcSession());
    Collection<Coordinator> coordinators = coordinatorSupplier.getCoordinators(); // TODO
    if (!coordinators.isEmpty()) {
      rpcSession.setCoordinator(coordinators.iterator().next());
    }
    return new WhirlpoolApiClient(rpcSession, sorobanAppWhirlpool);
  }

  public Tx0Info fetchTx0Info(String scode) throws Exception {
    Tx0DataRequest tx0DataRequest = new Tx0DataRequest(scode, config.getPartner());
    Coordinator coordinator =
        coordinatorSupplier.getCoordinatorRandom(); // TODO adapt for multi-coordinators
    String poolId = coordinator.getPoolIds().iterator().next(); // TODO

    // fetch Tx0Data with new Soroban identity
    WhirlpoolApiClient whirlpoolClientApi = createWhirlpoolApiClient();
    Tx0DataResponse tx0DataResponse =
        ClientUtils.loopHttpAttempts(
            config.getTx0AttemptsSoroban(),
            () -> whirlpoolClientApi.tx0FetchData(tx0DataRequest, coordinator.getSender(), poolId));

    // instanciate Tx0Info
    Collection<Pool> pools = coordinatorSupplier.getPools();
    Tx0InfoConfig tx0InfoConfig =
        new Tx0InfoConfig(
            getTx0PreviewService(),
            pools,
            config.getTx0AttemptsAddressReuse(),
            config.getTx0AttemptsSoroban());
    return new Tx0Info(
        this,
        tx0InfoConfig,
        Arrays.stream(tx0DataResponse.tx0Datas)
            .map(
                item -> {
                  try {
                    return new Tx0Data(item);
                  } catch (Exception e) {
                    throw new RuntimeException("invalid Tx0Data", e);
                  }
                })
            .collect(Collectors.toList()));
  }

  public Tx0PreviewService getTx0PreviewService() {
    return tx0PreviewService;
  }

  public Tx0Service getTx0Service() {
    return tx0Service;
  }

  public CoordinatorSupplier getCoordinatorSupplier() {
    return coordinatorSupplier;
  }

  public SorobanAppWhirlpool getSorobanAppWhirlpool() {
    return sorobanAppWhirlpool;
  }

  public MinerFeeSupplier getMinerFeeSupplier() {
    return tx0PreviewService.getMinerFeeSupplier();
  }
}
