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

  protected final Tx0PreviewService tx0PreviewService;
  private Tx0Service tx0Service;
  private SorobanAppWhirlpool sorobanAppWhirlpool;
  protected final CoordinatorSupplier coordinatorSupplier;
  private RpcClientService rpcClientService;

  private String partner;
  private int tx0AttemptsAddressReuse;
  private int tx0AttemptsSoroban;

  public WhirlpoolInfo(
      Tx0PreviewService tx0PreviewService,
      Tx0Service tx0Service,
      SorobanAppWhirlpool sorobanAppWhirlpool,
      CoordinatorSupplier coordinatorSupplier,
      RpcClientService rpcClientService,
      String partner,
      int tx0AttemptsAddressReuse,
      int tx0AttemptsSoroban) {
    this.tx0PreviewService = tx0PreviewService;
    this.tx0Service = tx0Service;
    this.rpcClientService = rpcClientService;
    this.sorobanAppWhirlpool = sorobanAppWhirlpool;
    this.coordinatorSupplier = coordinatorSupplier;
    this.partner = partner;
    this.tx0AttemptsAddressReuse = tx0AttemptsAddressReuse;
    this.tx0AttemptsSoroban = tx0AttemptsSoroban;
  }

  public WhirlpoolInfo(
      MinerFeeSupplier minerFeeSupplier, WhirlpoolWalletConfig whirlpoolWalletConfig) {
    this.tx0PreviewService = computeTx0PreviewService(minerFeeSupplier, whirlpoolWalletConfig);
    this.tx0Service =
        new Tx0Service(
            whirlpoolWalletConfig.getSamouraiNetwork().getParams(),
            tx0PreviewService,
            whirlpoolWalletConfig.getFeeOpReturnImpl());
    this.rpcClientService =
        whirlpoolWalletConfig.getSorobanWalletService().getSorobanService().getRpcClientService();
    this.sorobanAppWhirlpool = new SorobanAppWhirlpool(whirlpoolWalletConfig.getSamouraiNetwork());
    this.coordinatorSupplier =
        computeCoordinatorSupplier(
            whirlpoolWalletConfig, tx0PreviewService, rpcClientService, sorobanAppWhirlpool);
    this.partner = whirlpoolWalletConfig.getPartner();
    this.tx0AttemptsAddressReuse = whirlpoolWalletConfig.getTx0AttemptsAddressReuse();
    this.tx0AttemptsSoroban = whirlpoolWalletConfig.getTx0AttemptsSoroban();
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

  public Tx0Info fetchTx0Info(String scode) throws Exception {
    Tx0DataRequest tx0DataRequest = new Tx0DataRequest(scode, partner);
    Coordinator coordinator =
        coordinatorSupplier.getCoordinatorRandom(); // TODO adapt for multi-coordinators
    String poolId = coordinator.getPoolIds().iterator().next(); // TODO

    // fetch Tx0Data with new Soroban identity
    WhirlpoolApiClient whirlpoolClientApi = createWhirlpoolApiClient();
    Tx0DataResponse tx0DataResponse =
        ClientUtils.loopHttpAttempts(
            tx0AttemptsSoroban,
            () -> whirlpoolClientApi.tx0FetchData(tx0DataRequest, coordinator.getSender(), poolId));

    // instanciate Tx0Info
    Collection<Pool> pools = coordinatorSupplier.getPools();
    Tx0InfoConfig tx0InfoConfig =
        new Tx0InfoConfig(
            getTx0PreviewService(), pools, tx0AttemptsAddressReuse, tx0AttemptsSoroban);
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
