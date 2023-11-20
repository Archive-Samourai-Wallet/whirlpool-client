package com.samourai.whirlpool.client.soroban;

import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Util;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import com.samourai.whirlpool.protocol.WhirlpoolProtocolSoroban;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterCoordinatorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.RegisterInputSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientApi {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private WhirlpoolNetwork whirlpoolNetwork;
  private WhirlpoolProtocolSoroban whirlpoolProtocolSoroban;

  public SorobanClientApi(
      WhirlpoolNetwork whirlpoolNetwork, WhirlpoolProtocolSoroban whirlpoolProtocolSoroban) {
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.whirlpoolProtocolSoroban = whirlpoolProtocolSoroban;
  }

  public Single<Collection<RegisterCoordinatorSorobanMessage>> fetchCoordinators(
      SorobanClient sorobanClient) throws Exception {
    String directory = whirlpoolProtocolSoroban.getDirCoordinators(whirlpoolNetwork);
    return sorobanClient
        .getRpcClient()
        .directoryValues(directory)
        .map(
            payloads ->
                Arrays.stream(payloads)
                    // parse
                    .map(
                        payload -> {
                          try {
                            return sorobanClient.readSignedWithSender(
                                payload, RegisterCoordinatorSorobanMessage.class);
                          } catch (Exception e) {
                            log.error("Invalid RegisterCoordinatorSorobanMessage: " + payload, e);
                            return null;
                          }
                        })
                    .filter(Objects::nonNull)
                    .filter(m -> validateCoordinator(m.getRight(), m.getLeft()))
                    .map(m -> m.getRight())
                    // only 1 message per coordinator
                    .filter(Util.distinctBy(m -> m.coordinator.coordinatorId))
                    .collect(Collectors.toList()));
  }

  protected boolean validateCoordinator(
      RegisterCoordinatorSorobanMessage payload, PaymentCode sender) {
    // check coordinator.paymentCode against sender
    if (!sender.toString().equals(payload.coordinator.paymentCode)) {
      log.error(
          "Invalid RegisterCoordinatorSorobanMessage: sender doesn't match coordinator.paymentCode");
      return false;
    }

    // check coordinator.paymentCodeSignature against samourai signature
    return messageSignUtil.verifySignedMessage(
        whirlpoolNetwork.getSigningAddress(),
        payload.coordinator.paymentCode,
        payload.coordinator.paymentCodeSignature,
        whirlpoolNetwork.getParams());
  }

  public Completable registerInput(
      SorobanClient sorobanClient, RegisterInputRequest request, PaymentCode paymentCodeCoordinator)
      throws Exception {
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
    String encryptedPayload =
        sorobanClient.encryptWithSender(message.toPayload(), paymentCodeCoordinator);
    return sorobanClient.getRpcClient().directoryAdd(directory, encryptedPayload, RpcMode.SHORT);
  }

  public Single<InviteMixSorobanMessage> waitInviteMix(
      SorobanClient sorobanClient,
      RpcWallet rpcWalletClient,
      BIP47UtilGeneric bip47Util,
      PaymentCode paymentCodeCoordinator,
      long timeoutMs)
      throws Exception {
    String directory =
        whirlpoolProtocolSoroban.getDirRegisterInputResponse(
            rpcWalletClient, whirlpoolNetwork, bip47Util);
    return sorobanClient
        .getRpcClient()
        .directoryValueWaitAndRemove(directory, timeoutMs)
        .map(
            payload ->
                sorobanClient.readEncrypted(
                    payload, paymentCodeCoordinator, InviteMixSorobanMessage.class));
  }

  public WhirlpoolProtocolSoroban getWhirlpoolProtocolSoroban() {
    return whirlpoolProtocolSoroban;
  }
}
