package com.samourai.whirlpool.client.mix;

import com.samourai.soroban.client.AbstractSorobanPayload;
import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.exception.RejectedInputException;
import com.samourai.whirlpool.client.soroban.SorobanClientApi;
import com.samourai.whirlpool.protocol.WhirlpoolProtocolSoroban;
import com.samourai.whirlpool.protocol.soroban.ErrorSorobanMessage;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.messages.RegisterInputRequest;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixClientSoroban {
  private static final Logger log = LoggerFactory.getLogger(MixClientSoroban.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private SorobanClientApi sorobanClientApi;
  private BIP47UtilGeneric bip47Util;
  private RpcSession rpcSession;
  private RpcWallet rpcWallet;

  public MixClientSoroban(
      SorobanClientApi sorobanClientApi,
      BIP47UtilGeneric bip47Util,
      RpcSession rpcSession,
      RpcWallet rpcWallet) {
    this.sorobanClientApi = sorobanClientApi;
    this.bip47Util = bip47Util;
    this.rpcSession = rpcSession;
    this.rpcWallet = rpcWallet;
  }

  public InviteMixSorobanMessage registerInputAndWaitInviteMix(RegisterInputRequest request)
      throws Exception {
    while (true) {
      // REGISTER_INPUT
      asyncUtil.blockingGet(
          rpcSession.withRpcClientEncrypted(
              rpcWallet.getEncrypter(),
              rce -> {
                if (log.isDebugEnabled()) {
                  log.debug("=> withRpcClientEncrypted.call registerInput ");
                }
                return sorobanClientApi.registerInput(rce, request);
              }));

      if (log.isDebugEnabled()) {
        log.debug("=> registerInput success, waitInviteMix...");
      }
      try {
        // wait for mix invite or input rejection
        AbstractSorobanPayload response =
            asyncUtil.blockingGet(
                rpcSession.withRpcClientEncrypted(
                    rpcWallet.getEncrypter(),
                    rce ->
                        sorobanClientApi.waitInviteMix(
                            rce,
                            rpcWallet,
                            bip47Util,
                            getWhirlpoolProtocolSoroban().getRegisterInputFrequencyMs())));
        if (response instanceof InviteMixSorobanMessage) {
          // it's a mix invite
          return (InviteMixSorobanMessage) response;
        }
        // otherwise it's an error response => input rejected
        ErrorSorobanMessage errorResponse = (ErrorSorobanMessage) response;
        throw new RejectedInputException(errorResponse.message);
      } catch (TimeoutException e) {
        // no mix invite received yet
      }
    }
  }

  public void exit() {
    rpcSession.close();
  }

  public WhirlpoolProtocolSoroban getWhirlpoolProtocolSoroban() {
    return sorobanClientApi.getWhirlpoolProtocolSoroban();
  }
}
