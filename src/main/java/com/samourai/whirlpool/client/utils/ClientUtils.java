package com.samourai.whirlpool.client.utils;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.*;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Coordinator;
import com.samourai.whirlpool.protocol.SorobanProtocolWhirlpool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.beans.Utxo;
import com.samourai.whirlpool.protocol.soroban.api.WhirlpoolPartnerApiClient;
import com.samourai.xmanager.protocol.rest.RestErrorResponse;
import io.reactivex.Completable;
import java.io.File;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.*;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientUtils {
  private static final Logger log = LoggerFactory.getLogger(ClientUtils.class);

  private static final int SLEEP_REFRESH_UTXOS_TESTNET = 15000;
  private static final int SLEEP_REFRESH_UTXOS_MAINNET = 5000;
  public static final String USER_AGENT = "whirlpool-client";

  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .configure(
              DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // allow protocol upgrades
  private static final FeeUtil feeUtil = FeeUtil.getInstance();
  private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
  private static final int BORDEREAU_LENGTH = 30;

  public static void setupEnv() {
    // prevent user-agent tracking
    System.setProperty("http.agent", USER_AGENT);
  }

  public static WhirlpoolPartnerApiClient getWhirlpoolPartnerApiClient(
      RpcSession rpcSession,
      CoordinatorSupplier coordinatorSupplier,
      String poolId,
      SorobanProtocolWhirlpool sorobanProtocolWhirlpool)
      throws Exception {
    Coordinator c = coordinatorSupplier.findCoordinatorByPoolId(poolId);
    if (c == null) {
      throw new NotifiableException(
          "No coordinator available for pool "
              + poolId
              + ", please retry later or check for upgrade");
    }
    Bip47Partner coordinator = rpcSession.getRpcWallet().getBip47Partner(c.getPaymentCode(), true);
    return new WhirlpoolPartnerApiClient(rpcSession, coordinator, sorobanProtocolWhirlpool);
  }

  public static Integer findTxOutputIndex(
      String outputAddressBech32, Transaction tx, NetworkParameters params) {
    try {
      byte[] expectedScriptBytes =
          Bech32UtilGeneric.getInstance().computeScriptPubKey(outputAddressBech32, params);
      for (TransactionOutput output : tx.getOutputs()) {
        if (Arrays.equals(output.getScriptBytes(), expectedScriptBytes)) {
          return output.getIndex();
        }
      }
    } catch (Exception e) {
      log.error("findTxOutput failed", e);
    }
    return null;
  }

  public static String[] witnessSerialize64(TransactionWitness witness) {
    String[] serialized = new String[witness.getPushCount()];
    for (int i = 0; i < witness.getPushCount(); i++) {
      serialized[i] = WhirlpoolProtocol.encodeBytes(witness.getPush(i));
    }
    return serialized;
  }

  public static RSAKeyParameters publicKeyUnserialize(byte[] publicKeySerialized) throws Exception {
    RSAPublicKey rsaPublicKey =
        (RSAPublicKey)
            KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeySerialized));
    return new RSAKeyParameters(false, rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
  }

  public static String toJsonString(Object o) {
    try {
      return objectMapper.writeValueAsString(o);
    } catch (Exception e) {
      log.error("", e);
    }
    return null;
  }

  public static <T> T fromJson(String json, Class<T> type) throws Exception {
    return objectMapper.readValue(json, type);
  }

  public static Logger prefixLogger(Logger log, String logPrefix) {
    Level level = ((ch.qos.logback.classic.Logger) log).getEffectiveLevel();
    Logger newLog = LoggerFactory.getLogger(log.getName() + "[" + logPrefix + "]");
    ((ch.qos.logback.classic.Logger) newLog).setLevel(level);
    return newLog;
  }

  private static String parseRestErrorMessage(String responseBody) {
    try {
      RestErrorResponse restErrorResponse =
          ClientUtils.fromJson(responseBody, RestErrorResponse.class);
      return restErrorResponse.message;
    } catch (Exception e) {
      log.error(
          "parseRestErrorMessage failed: responseBody="
              + (responseBody != null ? responseBody : "null"));
      return null;
    }
  }

  public static String getHttpResponseBody(Throwable e) {
    if (e instanceof HttpException) {
      return ((HttpException) e).getResponseBody();
    }
    return null;
  }

  public static String parseRestErrorMessage(Throwable e) {
    String responseBody = getHttpResponseBody(e);
    if (responseBody == null) {
      return null;
    }
    return parseRestErrorMessage(responseBody);
  }

  public static Exception wrapRestError(Exception e) {
    String restErrorResponseMessage = ClientUtils.parseRestErrorMessage(e);
    if (restErrorResponseMessage != null) {
      return new NotifiableException(restErrorResponseMessage);
    }
    return e;
  }

  public static Throwable wrapRestError(Throwable e) {
    String restErrorResponseMessage = ClientUtils.parseRestErrorMessage(e);
    if (restErrorResponseMessage != null) {
      return new NotifiableException(restErrorResponseMessage);
    }
    return e;
  }

  public static int computeNextReceiveAddressIndex(
      IIndexHandler postmixIndexHandler, IndexRange indexRange) {
    // full range
    if (indexRange == IndexRange.FULL) {
      return postmixIndexHandler.getAndIncrementUnconfirmed();
    }

    int modulo = indexRange == IndexRange.ODD ? 1 : 0;
    int index;
    do {
      index = postmixIndexHandler.getAndIncrementUnconfirmed();
    } while (index % 2 != modulo);
    return index;
  }

  public static double satToBtc(long sat) {
    return sat / 100000000.0;
  }

  public static String utxoToKey(UnspentOutput unspentOutput) {
    return unspentOutput.tx_hash + ':' + unspentOutput.tx_output_n;
  }

  public static String utxoToKey(Utxo utxo) {
    return utxo.getHash() + ':' + utxo.getIndex();
  }

  public static String utxoToKey(String utxoHash, int utxoIndex) {
    return utxoHash + ':' + utxoIndex;
  }

  public static Completable sleepUtxosDelayAsync(final NetworkParameters params) {
    return AsyncUtil.getInstance()
        .runIOAsyncCompletable(
            () -> {
              // wait for delay
              boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(params);
              int sleepDelay =
                  isTestnet ? SLEEP_REFRESH_UTXOS_TESTNET : SLEEP_REFRESH_UTXOS_MAINNET;
              try {
                Thread.sleep(sleepDelay);
              } catch (InterruptedException e) {
              }
            });
  }

  public static String sha256Hash(String str) {
    return sha256Hash(str.getBytes());
  }

  public static String sha256Hash(byte[] bytes) {
    return Sha256Hash.wrap(Sha256Hash.hash(bytes)).toString();
  }

  public static String maskString(String value) {
    return Util.maskString(value);
  }

  public static void setLogLevel(Level logLevel) {
    LogbackUtils.setLogLevel("com.samourai", logLevel.toString());

    LogbackUtils.setLogLevel("com.samourai.whirlpool.client", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.whirlpool.client.mix.dialog", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.stomp.client", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.wallet.util.FeeUtil", logLevel.toString());

    LogbackUtils.setLogLevel("com.samourai.whirlpool.client.utils", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.whirlpool.client.wallet", logLevel.toString());

    LogbackUtils.setLogLevel("com.samourai.wallet.cahoots", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.wallet.cahoots.stowaway", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.wallet.cahoots.stonewallx2", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.soroban.client", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.soroban.client.rpc", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.soroban.client.dialog", logLevel.toString());
    LogbackUtils.setLogLevel("com.samourai.soroban.client.meeting", logLevel.toString());

    LogbackUtils.setLogLevel(
        "com.samourai.whirlpool.client.wallet.orchestrator", logLevel.toString());

    // skip noisy logs
    LogbackUtils.setLogLevel("org.bitcoinj", org.slf4j.event.Level.ERROR.toString());
    LogbackUtils.setLogLevel(
        "org.bitcoin", org.slf4j.event.Level.WARN.toString()); // "no wallycore"
    LogbackUtils.setLogLevel("org.eclipse.jetty", org.slf4j.event.Level.INFO.toString());
  }

  public static long computeTx0MinerFee(
      int nbPremix,
      long tx0FeePerB,
      Collection<? extends UnspentOutput> spendFroms,
      NetworkParameters params) {
    int tx0Size = computeTx0Size(nbPremix, spendFroms, params);
    long tx0MinerFee = FeeUtil.getInstance().calculateFee(tx0Size, tx0FeePerB);
    return tx0MinerFee;
  }

  public static int computeTx0Size(
      int nbPremix,
      Collection<? extends UnspentOutput> spendFromsOrNull,
      NetworkParameters params) {
    int nbOutputsNonOpReturn = nbPremix + 2; // outputs + change + fee

    int nbP2PKH = 0;
    int nbP2SH = 0;
    int nbP2WPKH = 0;
    if (spendFromsOrNull != null) { // spendFroms can be NULL (for fee simulation)
      for (UnspentOutput uo : spendFromsOrNull) {

        if (bech32Util.isP2WPKHScript(uo.script)) {
          nbP2WPKH++;
        } else {
          String address = uo.computeScript().getToAddress(params).toString();
          if (Address.fromBase58(params, address).isP2SHAddress()) {
            nbP2SH++;
          } else {
            nbP2PKH++;
          }
        }
      }
    } else {
      // estimate with 1 P2WPKH
      nbP2WPKH++;
    }
    int tx0Size = feeUtil.estimatedSizeSegwit(nbP2PKH, nbP2SH, nbP2WPKH, nbOutputsNonOpReturn, 1);
    return tx0Size;
  }

  public static int computeTx0Size(int nbPremix, int nbSpendFrom, NetworkParameters params) {
    int nbOutputsNonOpReturn = nbPremix + 2; // outputs + change + fee

    int nbP2PKH = 0;
    int nbP2SH = 0;
    int nbP2WPKH = 0;

    nbP2WPKH += nbSpendFrom; // estimate inputs with P2WPKH
    int tx0Size = feeUtil.estimatedSizeSegwit(nbP2PKH, nbP2SH, nbP2WPKH, nbOutputsNonOpReturn, 1);
    return tx0Size;
  }

  public static long computeTx0SpendValue(
      long premixValue, int nbPremix, long feeValueOrFeeChange, long tx0MinerFee) {
    long spendValue = (premixValue * nbPremix) + feeValueOrFeeChange + tx0MinerFee;
    return spendValue;
  }

  public static void createFile(File f) throws NotifiableException {
    try {
      SystemUtil.createFile(f);
    } catch (Exception e) {
      log.error("", e);
      throw new NotifiableException(e.getMessage());
    }
  }

  public static File createFile(String fileName) throws NotifiableException {
    File f = new File(fileName); // use current directory
    ClientUtils.createFile(f);
    return f;
  }

  public static <T> Collection<T> filterByAssignableType(Collection items, Class<T> type) {
    List<T> list = new LinkedList<T>();
    for (Object item : items) {
      if (item.getClass().isAssignableFrom(type)) {
        list.add((T) item);
      }
    }
    return list;
  }

  public static Collection<Integer> getOutputIndexs(Collection<TransactionOutput> outputs) {
    return outputs.stream().map(output -> output.getIndex()).collect(Collectors.<Integer>toList());
  }

  public static <B> Collection<B> intersect(Collection<B> a1, Collection<B> a2) {
    Set<B> s1 = new HashSet<B>(a1);
    Set<B> s2 = new HashSet<B>(a2);
    s1.retainAll(s2);
    return s1;
  }

  public static String dateToString(long timestamp) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
  }

  public static long bytesToMB(long bytes) {
    return Math.round(bytes / (1024L * 1024L));
  }

  public static String getDirUserHome() {
    String dir = System.getProperty("user.home");
    if (StringUtils.isEmpty(dir)) {
      dir = System.getProperty("user.dir"); // fallback
    }
    return dir;
  }

  public static <E> List<E> subListLastItems(LinkedList<E> list, int nb) {
    int fromIndex = Math.max(0, list.size() - nb);
    int toIndex = Math.min(fromIndex + nb, list.size());
    return list.subList(fromIndex, toIndex);
  }

  public static byte[] generateBordereau() {
    return RandomUtil.getInstance().nextBytes(BORDEREAU_LENGTH);
  }
}
