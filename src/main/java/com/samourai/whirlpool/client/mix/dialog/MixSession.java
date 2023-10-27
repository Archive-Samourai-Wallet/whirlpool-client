package com.samourai.whirlpool.client.mix.dialog;

import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompTransportListener;
import com.samourai.stomp.client.StompTransport;
import com.samourai.wallet.util.MessageErrorListener;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.soroban.InviteMixSorobanMessage;
import com.samourai.whirlpool.protocol.websocket.MixMessage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixSession {
  private static final Logger log = LoggerFactory.getLogger(MixSession.class);

  private MixDialogListener listener;
  private WhirlpoolProtocol whirlpoolProtocol;
  private WhirlpoolClientConfig config;
  private StompTransport transport;
  private ServerApi serverApi;
  private boolean done;

  // connect data
  private Long connectBeginTime;

  // session data
  private MixDialog dialog;

  public MixSession(
      MixDialogListener listener,
      WhirlpoolProtocol whirlpoolProtocol,
      WhirlpoolClientConfig config,
      InviteMixSorobanMessage inviteMixSorobanMessage,
      ServerApi serverApi) {
    this.listener = listener;
    this.whirlpoolProtocol = whirlpoolProtocol;
    this.config = config;
    this.transport = null;
    this.serverApi = serverApi;
    this.dialog = new MixDialog(listener, this, inviteMixSorobanMessage);
  }

  public synchronized void connect() {
    if (done) {
      if (log.isDebugEnabled()) {
        log.debug("connect() aborted: done");
      }
      return;
    }
    if (connectBeginTime == null) {
      connectBeginTime = System.currentTimeMillis();
    }

    String wsUrl = serverApi.getWsUrlConnect();
    if (log.isDebugEnabled()) {
      log.debug("connecting to server: " + wsUrl);
    }

    // connect with a new transport
    Map<String, String> connectHeaders = new LinkedHashMap<>();
    IStompClient stompClient = config.getStompClientService().newStompClient();
    transport = new StompTransport(stompClient, computeTransportListener());
    transport.connect(wsUrl, connectHeaders);
  }

  private void subscribe() {
    // subscribe to private queue
    final String privateQueue =
        whirlpoolProtocol.WS_PREFIX_USER_PRIVATE + whirlpoolProtocol.WS_PREFIX_USER_REPLY;
    transport.subscribe(
        computeSubscribeStompHeaders(privateQueue),
        new MessageErrorListener<Object, String>() {
          @Override
          public void onMessage(Object payload) {
            if (done) {
              if (log.isTraceEnabled()) {
                log.trace("onMessage: done");
              }
              return;
            }

            // mix dialog
            MixMessage mixMessage = checkMixMessage(payload);
            if (mixMessage != null) {
              dialog.onPrivateReceived(mixMessage);
            } else {
              String notifiableError = "not a MixMessage: " + ClientUtils.toJsonString(payload);
              log.error("--> " + privateQueue + ": " + notifiableError);
              listener.exitOnProtocolError(notifiableError);
            }
          }

          @Override
          public void onError(String errorMessage) {
            String notifiableException = "subscribe error: " + errorMessage;
            log.error("--> " + privateQueue + ": " + notifiableException);
            listener.exitOnProtocolError(errorMessage); // subscribe error
          }
        },
        serverProtocolVersion -> {
          // server version mismatch
          listener.exitOnProtocolVersionMismatch(serverProtocolVersion);
        });

    // will automatically receive mixStatus in response of subscription
    if (log.isDebugEnabled()) {
      log.debug("subscribed to server");
    }
  }

  private MixMessage checkMixMessage(Object payload) {
    // should be MixMessage
    Class payloadClass = payload.getClass();
    if (!MixMessage.class.isAssignableFrom(payloadClass)) {
      String notifiableError =
          "unexpected message from server: " + ClientUtils.toJsonString(payloadClass);
      log.error("Protocol error: " + notifiableError);
      listener.exitOnProtocolError(notifiableError);
      return null;
    }
    return (MixMessage) payload;
  }

  public synchronized void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("Disconnecting...");
    }
    done = true;
    connectBeginTime = null;
    if (transport != null) {
      transport.disconnect();
    }
    if (log.isDebugEnabled()) {
      log.debug("Disconnected.");
    }
  }

  public void send(String destination, Object message) {
    if (transport != null) {
      transport.send(destination, message);
    } else {
      log.warn("send: ignoring (transport = null)");
    }
  }

  //

  private Map<String, String> computeSubscribeStompHeaders(String destination) {
    Map<String, String> stompHeaders = new HashMap<String, String>();
    if (destination != null) {
      stompHeaders.put(StompTransport.HEADER_DESTINATION, destination);
    }
    return stompHeaders;
  }

  private IStompTransportListener computeTransportListener() {
    return new IStompTransportListener() {

      @Override
      public synchronized void onTransportConnected() {
        if (log.isDebugEnabled()) {
          long elapsedTime = (System.currentTimeMillis() - connectBeginTime) / 1000;
          log.debug("Connected in " + elapsedTime + "s, subscribing...");
        }
        connectBeginTime = null;
        subscribe();

        // confirm input
        try {
          dialog.confirmInput();
        } catch (Exception e) {
          log.error("confirmInput failed", e);
          listener.exitOnProtocolError(e.getMessage());
        }
      }

      @Override
      public synchronized void onTransportDisconnected(Throwable exception) {
        // transport cannot be used
        transport = null;

        if (done) {
          if (log.isTraceEnabled()) {
            log.trace("onTransportDisconnected: done");
          }
          return;
        }

        // we just got disconnected
        log.error(" ! connexion lost, aborting");
        done = true;
        listener.exitOnDisconnected(exception.getMessage());
      }
    };
  }

  //

  public ServerApi getServerApi() {
    return serverApi;
  }

  protected StompTransport __getTransport() {
    return transport;
  }
}
