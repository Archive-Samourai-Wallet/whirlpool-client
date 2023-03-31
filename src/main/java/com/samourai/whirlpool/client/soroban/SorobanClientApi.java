package com.samourai.whirlpool.client.soroban;

import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.soroban.PoolInfoSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterInputSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientApi {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();

  public SorobanClientApi() {}

  public Single<Collection<PoolInfoSorobanMessage>> fetchPools(RpcClient rpcClient)
      throws Exception {
    String directory = WhirlpoolProtocol.getSorobanDirPools();
    return rpcClient
        .directoryValues(directory)
        .map(
            payloads -> {
              Collection<PoolInfoSorobanMessage> poolInfoSorobanMessages = new LinkedList<>();
              for (String payload : payloads) {
                try {
                  PoolInfoSorobanMessage poolInfoSorobanMessage =
                      jsonUtil.getObjectMapper().readValue(payload, PoolInfoSorobanMessage.class);
                  poolInfoSorobanMessages.add(poolInfoSorobanMessage);
                } catch (Exception e) {
                  log.error("cannot parse PoolInfoSoroban: " + payload, e);
                }
              }
              return poolInfoSorobanMessages;
            });
  }

  public Completable registerInput(
      RpcClientEncrypted rpcClientEncrypted,
      PaymentCode paymentCodeCoordinator,
      RegisterInputRequest request)
      throws Exception {
    String info = "[REGISTER_INPUT] ";
    String directory = WhirlpoolProtocol.getSorobanDirRegisterInput(request.poolId);

    RegisterInputSorobanMessage message =
        new RegisterInputSorobanMessage(
            request.poolId,
            request.utxoHash,
            request.utxoIndex,
            request.signature,
            request.liquidity,
            request.blockHeight);
    if (log.isDebugEnabled()) {
      log.debug(
          info
              + "=> registerInput: "
              + request.utxoHash
              + ":"
              + request.utxoIndex
              + ", poolId="
              + request.poolId
              + ", liquidity="
              + request.liquidity
              + ", blockHeight="
              + request.blockHeight);
    }
    return rpcClientEncrypted.sendEncryptedWithSender(directory, message, paymentCodeCoordinator);
  }
}
