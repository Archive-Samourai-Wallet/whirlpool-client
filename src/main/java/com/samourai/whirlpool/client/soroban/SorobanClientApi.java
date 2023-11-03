package com.samourai.whirlpool.client.soroban;

import com.samourai.soroban.client.AbstractSorobanPayload;
import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.wallet.util.Util;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import com.samourai.whirlpool.protocol.WhirlpoolProtocolSoroban;
import com.samourai.whirlpool.protocol.soroban.ErrorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterInputSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientApi {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();
  private WhirlpoolNetwork whirlpoolNetwork;
  private WhirlpoolProtocolSoroban whirlpoolProtocolSoroban;

  public SorobanClientApi(
      WhirlpoolNetwork whirlpoolNetwork, WhirlpoolProtocolSoroban whirlpoolProtocolSoroban) {
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.whirlpoolProtocolSoroban = whirlpoolProtocolSoroban;
  }

  public Single<Collection<RegisterCoordinatorSorobanMessage>> fetchCoordinators(
      RpcClient rpcClient) throws Exception {
    String directory = whirlpoolProtocolSoroban.getDirCoordinators(whirlpoolNetwork);
    return rpcClient
        .directoryValues(directory)
        .map(
            payloads ->
                Arrays.stream(payloads)
                    // parse
                    .map(
                        payload -> {
                          try {
                            return jsonUtil
                                .getObjectMapper()
                                .readValue(payload, RegisterCoordinatorSorobanMessage.class);
                          } catch (Exception e) {
                            log.error(
                                "cannot parse RegisterCoordinatorSorobanMessage: " + payload, e);
                            return null;
                          }
                        })
                    // only 1 message per coordinator
                    .filter(Util.distinctBy(m -> m.coordinator.coordinatorId))
                    .collect(Collectors.toList()));
  }

  public Single<String> registerInput(
      RpcClientEncrypted rpcClientEncrypted, RegisterInputRequest request) throws Exception {
    String directory =
        whirlpoolProtocolSoroban.getDirRegisterInput(whirlpoolNetwork, request.poolId);

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
          "=> registerInput: "
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
    return rpcClientEncrypted.sendEncryptedWithSender(
        directory, message, whirlpoolNetwork.getSigningPaymentCode(), RpcMode.SHORT);
  }

  public Single<AbstractSorobanPayload> waitInviteMix(
      RpcClientEncrypted rpcClientEncrypted,
      RpcWallet rpcWalletClient,
      BIP47UtilGeneric bip47Util,
      long timeoutMs)
      throws Exception {
    PaymentCode paymentCodeCoordinator = whirlpoolNetwork.getSigningPaymentCode();
    String directory =
        whirlpoolProtocolSoroban.getDirRegisterInputResponse(
            rpcWalletClient, whirlpoolNetwork, bip47Util);
    return rpcClientEncrypted
        .receiveEncrypted(directory, timeoutMs, paymentCodeCoordinator, 10000)
        .map(
            payload -> {
              if (ErrorSorobanMessage.isAssignableFrom(payload)) {
                // error response
                return jsonUtil.getObjectMapper().readValue(payload, ErrorSorobanMessage.class);
              } else {
                return jsonUtil.getObjectMapper().readValue(payload, InviteMixSorobanMessage.class);
              }
            });
  }

  public WhirlpoolProtocolSoroban getWhirlpoolProtocolSoroban() {
    return whirlpoolProtocolSoroban;
  }
}
