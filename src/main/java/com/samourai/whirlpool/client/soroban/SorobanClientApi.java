package com.samourai.whirlpool.client.soroban;

import com.samourai.soroban.client.AbstractSorobanPayload;
import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.soroban.ErrorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterInputSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientApi {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();
  private WhirlpoolNetwork whirlpoolNetwork;

  public SorobanClientApi(WhirlpoolNetwork whirlpoolNetwork) {
    this.whirlpoolNetwork = whirlpoolNetwork;
  }

  public Single<Collection<RegisterCoordinatorSorobanMessage>> fetchCoordinators(
      RpcClient rpcClient) throws Exception {
    String directory = WhirlpoolProtocol.getSorobanDirCoordinators(whirlpoolNetwork);
    return rpcClient
        .directoryValues(directory)
        .map(
            payloads -> {
              Map<String, RegisterCoordinatorSorobanMessage> registerCoordinatorById =
                  new LinkedHashMap<>();
              for (String payload : payloads) {
                try {
                  RegisterCoordinatorSorobanMessage message =
                      jsonUtil
                          .getObjectMapper()
                          .readValue(payload, RegisterCoordinatorSorobanMessage.class);

                  // keep latest message distinct by coordinatorId
                  registerCoordinatorById.put(message.coordinator.coordinatorId, message);
                } catch (Exception e) {
                  log.error("cannot parse RegisterCoordinatorSorobanMessage: " + payload, e);
                }
              }
              return registerCoordinatorById.values();
            });
  }

  public Single<String> registerInput(
      RpcClientEncrypted rpcClientEncrypted, RegisterInputRequest request) throws Exception {
    String directory =
        WhirlpoolProtocol.getSorobanDirRegisterInput(whirlpoolNetwork, request.poolId);

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
      RegisterInputRequest request,
      RpcWallet rpcWalletClient,
      BIP47UtilGeneric bip47Util,
      long timeoutMs)
      throws Exception {
    NetworkParameters params = rpcWalletClient.getBip47Wallet().getParams();
    PaymentCode paymentCodeCoordinator = whirlpoolNetwork.getSigningPaymentCode();
    String directory =
        WhirlpoolProtocol.getSorobanDirRegisterInputResponse(
            rpcWalletClient,
            whirlpoolNetwork,
            request.utxoHash,
            request.utxoIndex,
            bip47Util,
            params);
    return rpcClientEncrypted
        .receiveEncrypted(directory, timeoutMs, paymentCodeCoordinator, 10000)
        .map(
            payload -> {
              try {
                return JSONUtils.getInstance()
                    .getObjectMapper()
                    .readValue(payload, InviteMixSorobanMessage.class);
              } catch (Exception e) {
                // may be an error response
                return JSONUtils.getInstance()
                    .getObjectMapper()
                    .readValue(payload, ErrorSorobanMessage.class);
              }
            });
  }
}
