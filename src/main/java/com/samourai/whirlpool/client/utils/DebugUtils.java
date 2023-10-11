package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.api.paynym.beans.PaynymContact;
import com.samourai.wallet.api.paynym.beans.PaynymState;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugUtils {
  private static final Logger log = LoggerFactory.getLogger(DebugUtils.class);
  private static final UtxoUtil utxoUtil = UtxoUtil.getInstance();

  public static String getDebug(WhirlpoolWallet whirlpoolWallet) {
    StringBuilder sb = new StringBuilder("\n");
    sb.append("now = " + ClientUtils.dateToString(System.currentTimeMillis()) + "\n");
    if (whirlpoolWallet != null) {
      sb.append(getDebugWallet(whirlpoolWallet));
      sb.append(getDebugUtxos(whirlpoolWallet));
      sb.append(getDebugMixingThreads(whirlpoolWallet));
      sb.append(getDebugPools(whirlpoolWallet.getPoolSupplier()));
      sb.append(getDebugPaynym(whirlpoolWallet.getPaynymSupplier()));
    } else {
      sb.append("WhirlpoolWallet is closed.\n");
    }
    sb.append(getDebugSystem());
    return sb.toString();
  }

  public static String getDebugWallet(WhirlpoolWallet whirlpoolWallet) {
    StringBuilder sb = new StringBuilder().append("\n");

    // receive address
    BipAddress depositAddress = whirlpoolWallet.getWalletDeposit().getNextAddressReceive(false);

    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ RECEIVE ADDRESS" + "\n");
    sb.append(" • Address: " + depositAddress.getAddressString() + "\n");
    sb.append(" • Path: " + depositAddress.getPathAddress() + "\n\n");

    // balance
    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ WALLET:" + "\n");
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      Collection<WhirlpoolUtxo> utxos = whirlpoolWallet.getUtxoSupplier().findUtxos(account);
      sb.append(
          " • "
              + account
              + ": "
              + utxos.size()
              + " utxos, "
              + ClientUtils.satToBtc(BipUtxo.sumValue(utxos))
              + " BTC\n");

      for (BipWallet wallet : whirlpoolWallet.getWalletSupplier().getWallets(account)) {
        NetworkParameters params = wallet.getParams();
        String nextAddressReceive = wallet.getNextAddressReceive(false).getAddressString();
        String nextAddressChange = wallet.getNextAddressChange(false).getAddressString();
        sb.append(
            "     > "
                + wallet.getId()
                + ": path="
                + wallet.getDerivation().getPathAccount(params)
                + ", bipFormats="
                + wallet.getBipFormats()
                + ", bipFormatDefault="
                + wallet.getBipFormatDefault().getId()
                + ", xpub="
                + ClientUtils.maskString(wallet.getXPub())
                + ", bipPub="
                + ClientUtils.maskString(wallet.getBipPub())
                + ", nextAddressReceive="
                + nextAddressReceive
                + ", nextAddressChange="
                + nextAddressChange);

        for (Chain chain : Chain.values()) {
          int index = wallet.getIndexHandler(chain).get();
          sb.append(", " + chain.name().toLowerCase() + "Index=" + index);
        }
        sb.append("\n");
      }
    }
    sb.append(
        " • TOTAL = "
            + ClientUtils.satToBtc(whirlpoolWallet.getUtxoSupplier().getBalanceTotal())
            + " BTC"
            + "\n\n");

    // chain status
    WalletResponse.InfoBlock latestBlock = whirlpoolWallet.getChainSupplier().getLatestBlock();
    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ LATEST BLOCK:" + "\n");
    sb.append(" • Height: " + latestBlock.height + "\n");
    sb.append(" • Hash: " + latestBlock.hash + "\n");
    sb.append(" • Time: " + ClientUtils.dateToString(latestBlock.time * 1000) + "\n\n");

    // chain
    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ MINER FEE:" + "\n");
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      long value = whirlpoolWallet.getMinerFeeSupplier().getFee(minerFeeTarget);
      sb.append("fee[" + minerFeeTarget + "] = " + value + "\n");
    }
    return sb.toString();
  }

  public static String getDebugUtxos(WhirlpoolWallet whirlpoolWallet) {
    StringBuilder sb = new StringBuilder().append("\n");
    for (WhirlpoolAccount account : WhirlpoolAccount.values()) {
      sb.append(getDebugUtxos(whirlpoolWallet, account) + "\n");
    }
    return sb.toString();
  }

  private static String getDebugUtxos(WhirlpoolWallet whirlpoolWallet, WhirlpoolAccount account) {
    StringBuilder sb = new StringBuilder().append("\n");

    Collection<WhirlpoolUtxo> utxos = whirlpoolWallet.getUtxoSupplier().findUtxos(account);
    int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlock().height;
    try {
      sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
      double balance = ClientUtils.satToBtc(whirlpoolWallet.getUtxoSupplier().getBalanceTotal());
      String lastUpdate =
          ClientUtils.dateToString(whirlpoolWallet.getUtxoSupplier().getLastUpdate());
      sb.append(
          "⣿ "
              + account.name()
              + " UTXOS ("
              + utxos.size()
              + ") ("
              + balance
              + "btc at "
              + lastUpdate
              + "):");
      sb.append(getDebugUtxos(utxos, latestBlockHeight));
    } catch (Exception e) {
      log.error("", e);
    }
    return sb.toString();
  }

  public static String getDebugUtxos(Collection<WhirlpoolUtxo> utxos, int latestBlockHeight) {
    String lineFormat =
        "| %10s | %7s | %68s | %45s | %13s | %27s | %14s | %8s | %8s | %4s | %19s | %19s |\n";
    StringBuilder sb = new StringBuilder().append("\n");
    sb.append(
        String.format(
            lineFormat,
            "BALANCE",
            "CONFIRM",
            "UTXO",
            "ADDRESS",
            "TYPE",
            "PATH",
            "STATUS",
            "MIXABLE",
            "POOL",
            "MIXS",
            "ACTIVITY",
            "ERROR"));
    sb.append(String.format(lineFormat, "(btc)", "", "", "", "", "", "", "", "", "", "", ""));
    Iterator var3 = utxos.iterator();

    while (var3.hasNext()) {
      WhirlpoolUtxo whirlpoolUtxo = (WhirlpoolUtxo) var3.next();
      WhirlpoolUtxoState utxoState = whirlpoolUtxo.getUtxoState();
      String utxo = whirlpoolUtxo.getTxHash() + ":" + whirlpoolUtxo.getTxOutputIndex();
      String mixableStatusName =
          utxoState.getMixableStatus() != null ? utxoState.getMixableStatus().name() : "-";
      Long lastActivity = whirlpoolUtxo.getUtxoState().getLastActivity();
      String activity =
          (lastActivity != null ? ClientUtils.dateToString(lastActivity) + " " : "")
              + (whirlpoolUtxo.getUtxoState().getMessage() != null
                  ? whirlpoolUtxo.getUtxoState().getMessage()
                  : "");
      Long lastError = whirlpoolUtxo.getUtxoState().getLastError();
      String error =
          (lastError != null ? ClientUtils.dateToString(lastError) + " " : "")
              + (whirlpoolUtxo.getUtxoState().getError() != null
                  ? whirlpoolUtxo.getUtxoState().getError()
                  : "");
      sb.append(
          String.format(
              lineFormat,
              ClientUtils.satToBtc(whirlpoolUtxo.getValueLong()),
              whirlpoolUtxo.getConfirmInfo().getConfirmations(latestBlockHeight),
              utxo,
              whirlpoolUtxo.getAddress(),
              whirlpoolUtxo.getBipFormat().getId(),
              utxoUtil.computePath(whirlpoolUtxo),
              utxoState.getStatus().name(),
              mixableStatusName,
              whirlpoolUtxo.getUtxoState().getPoolId() != null
                  ? whirlpoolUtxo.getUtxoState().getPoolId()
                  : "-",
              whirlpoolUtxo.getMixsDone(),
              activity,
              error));
    }
    return sb.toString();
  }

  public static String getDebugSystem() {
    StringBuilder sb = new StringBuilder().append("\n");
    final ThreadGroup tg = Thread.currentThread().getThreadGroup();
    Collection<Thread> threadSet =
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getThreadGroup() == tg)
            .sorted(Comparator.comparing(t -> t.getName().toLowerCase()))
            .collect(Collectors.<Thread>toList());
    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ SYSTEM THREADS:" + "\n");
    int i = 0;
    for (Thread t : threadSet) {
      sb.append("#" + i + " " + t + ":" + "" + t.getState() + "\n");
      // show trace for BLOCKED
      if (Thread.State.BLOCKED.equals(t.getState())) {
        sb.append(StringUtils.join(t.getStackTrace(), "\n"));
      }
      i++;
    }

    // memory
    Runtime rt = Runtime.getRuntime();
    long total = rt.totalMemory();
    long free = rt.freeMemory();
    long used = total - free;
    sb.append(
        "⣿ MEM USE: "
            + ClientUtils.bytesToMB(used)
            + "M/"
            + ClientUtils.bytesToMB(total)
            + "M"
            + "\n");

    return sb.toString();
  }

  public static String getDebugMixingThreads(WhirlpoolWallet whirlpoolWallet) {
    StringBuilder sb = new StringBuilder().append("\n");
    MixingState mixingState = whirlpoolWallet.getMixingState();
    int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlock().height;
    try {
      sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
      sb.append("⣿ MIXING THREADS:" + "\n");

      String lineFormat =
          "| %25s | %8s | %10s | %10s | %8s | %68s | %14s | %8s | %6s | %20s | %19s |\n";
      sb.append(
          String.format(
              lineFormat,
              "STATUS",
              "SINCE",
              "ACCOUNT",
              "BALANCE",
              "CONFIRMS",
              "UTXO",
              "PATH",
              "POOL",
              "MIXS",
              "ACTIVITY",
              "ERROR"));

      long now = System.currentTimeMillis();
      for (WhirlpoolUtxo whirlpoolUtxo : mixingState.getUtxosMixing()) {
        MixProgress mixProgress = whirlpoolUtxo.getUtxoState().getMixProgress();
        String progress = mixProgress != null ? mixProgress.toString() : "";
        String since = mixProgress != null ? ((now - mixProgress.getSince()) / 1000) + "s" : "";
        String utxo = whirlpoolUtxo.getTxHash() + ":" + whirlpoolUtxo.getTxOutputIndex();
        Long lastActivity = whirlpoolUtxo.getUtxoState().getLastActivity();
        String activity =
            (lastActivity != null ? ClientUtils.dateToString(lastActivity) + " " : "")
                + (whirlpoolUtxo.getUtxoState().getMessage() != null
                    ? whirlpoolUtxo.getUtxoState().getMessage()
                    : "");
        Long lastError = whirlpoolUtxo.getUtxoState().getLastError();
        String error =
            (lastError != null ? ClientUtils.dateToString(lastError) + " " : "")
                + (whirlpoolUtxo.getUtxoState().getError() != null
                    ? whirlpoolUtxo.getUtxoState().getError()
                    : "");
        sb.append(
            String.format(
                lineFormat,
                progress,
                since,
                whirlpoolUtxo.getAccount().name(),
                ClientUtils.satToBtc(whirlpoolUtxo.getValueLong()),
                whirlpoolUtxo.getConfirmInfo().getConfirmations(latestBlockHeight),
                utxo,
                utxoUtil.computePath(whirlpoolUtxo),
                mixProgress.getPoolId() != null ? mixProgress.getPoolId() : "-",
                whirlpoolUtxo.getMixsDone(),
                activity,
                error));
      }
    } catch (Exception e) {
      log.error("", e);
    }
    sb.append(
        "Mixing: "
            + mixingState.getNbMixing()
            + " ("
            + mixingState.getNbMixingMustMix()
            + "+"
            + mixingState.getNbMixingLiquidity()
            + ")\n");
    sb.append(
        "Queued: "
            + mixingState.getNbQueued()
            + " ("
            + mixingState.getNbQueuedMustMix()
            + "+"
            + mixingState.getNbQueuedLiquidity()
            + ")\n");
    return sb.toString();
  }

  public static String getDebugPools(PoolSupplier poolSupplier) {
    StringBuilder sb = new StringBuilder().append("\n");
    try {
      sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
      sb.append("⣿ POOLS:" + "\n");

      String lineFormat = "| %15s | %15s | %20s | %15s | %15s | %20s | %15s |\n";
      sb.append(
          String.format(
              lineFormat,
              "NAME",
              "DENOMINATION",
              "MAX. PREMIXS PER TX0",
              "MIN. DEPOSIT",
              "MAX. PER TX0",
              "MAX. PER CASCADING",
              "FLAT ENTRY FEE"));

      for (Pool pool : poolSupplier.getPools()) {
        sb.append(
            String.format(
                lineFormat,
                pool.getPoolId(),
                ClientUtils.satToBtc(pool.getDenomination()),
                pool.getTx0MaxOutputs(),
                ClientUtils.satToBtc(pool.getTx0PreviewMinSpendValue()),
                ClientUtils.satToBtc(pool.getTx0PreviewMaxSpendValue()),
                ClientUtils.satToBtc(pool.getTx0PreviewMaxSpendValueCascading()),
                ClientUtils.satToBtc(pool.getFeeValue())));
      }
    } catch (Exception e) {
      log.error("", e);
    }
    return sb.toString();
  }

  public static String getDebugPaynym(PaynymSupplier paynymSupplier) {
    if (paynymSupplier == null) { // not implemented yet on Android
      return "";
    }
    StringBuilder sb = new StringBuilder().append("\n");

    sb.append("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿" + "\n");
    sb.append("⣿ PAYNYM" + "\n");

    PaynymState paynymState = paynymSupplier.getPaynymState();
    boolean claimed = paynymState.isClaimed();

    sb.append(" • PaymentCode: " + paynymState.getPaymentCode() + "\n");
    sb.append(" • Claimed: " + claimed + "\n");

    if (claimed) {
      sb.append(" • Name: " + paynymState.getNymName() + "\n");

      Collection<PaynymContact> followers = paynymState.getFollowers();
      sb.append(" • " + followers.size() + " followers: ");
      sb.append(
          followers.stream()
                  .map(paynymContact -> paynymContact.getNymName())
                  .collect(Collectors.joining(", "))
              + "\n");

      Collection<PaynymContact> following = paynymState.getFollowing();
      sb.append(" • " + following.size() + " following: ");
      sb.append(
          following.stream()
                  .map(paynymContact -> paynymContact.getNymName())
                  .collect(Collectors.joining(", "))
              + "\n");
      sb.append("\n");
    }
    return sb.toString();
  }
}
