package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.BipUtxoImpl;
import com.samourai.wallet.utxo.UtxoConfirmInfo;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfig;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import org.bitcoinj.core.NetworkParameters;

public class WhirlpoolUtxo extends BipUtxoImpl {
  private BipWallet bipWallet;
  private BipFormat bipFormat;
  private WhirlpoolUtxoState utxoState;
  private UtxoConfigSupplier utxoConfigSupplier;

  public WhirlpoolUtxo(
      BipUtxo utxo,
      BipWallet bipWallet,
      BipFormat bipFormat,
      String poolId,
      UtxoConfigSupplier utxoConfigSupplier) {
    super(utxo);
    this.bipWallet = bipWallet;
    this.bipFormat = bipFormat;
    this.utxoState = new WhirlpoolUtxoState(poolId);
    this.utxoConfigSupplier = utxoConfigSupplier;

    this.setMixableStatus();
  }

  public BipAddress getBipAddress() {
    return bipWallet.getAddressAt(getChainIndex(), getAddressIndex());
  }

  private void setMixableStatus() {
    MixableStatus mixableStatus = computeMixableStatus();
    utxoState.setMixableStatus(mixableStatus);
  }

  private MixableStatus computeMixableStatus() {
    // check pool
    if (utxoState.getPoolId() == null) {
      return MixableStatus.NO_POOL;
    }

    // check confirmations
    if (!getConfirmInfo().isConfirmed()) {
      return MixableStatus.UNCONFIRMED;
    }

    // ok
    return MixableStatus.MIXABLE;
  }

  // used by Sparrow
  public UtxoConfig getUtxoConfigOrDefault() {
    UtxoConfig utxoConfig = utxoConfigSupplier.getUtxo(getTxHash(), getTxOutputIndex());
    if (utxoConfig == null) {
      int mixsDone = WhirlpoolAccount.POSTMIX.equals(getAccount()) ? 1 : 0;
      utxoConfig = new UtxoConfigPersisted(mixsDone);
    }
    return utxoConfig;
  }

  public int getMixsDone() {
    return getUtxoConfigOrDefault().getMixsDone();
  }

  public void setMixsDone(int mixsDone) {
    utxoConfigSupplier.setMixsDone(getTxHash(), getTxOutputIndex(), mixsDone);
  }

  public boolean isBlocked() {
    return getUtxoConfigOrDefault().isBlocked();
  }

  public void setBlocked(boolean blocked) {
    utxoConfigSupplier.setBlocked(getTxHash(), getTxOutputIndex(), blocked);
  }

  public String getNote() {
    return getUtxoConfigOrDefault().getNote();
  }

  public void setNote(String note) {
    utxoConfigSupplier.setNote(getTxHash(), getTxOutputIndex(), note);
  }

  public BipWallet getBipWallet() {
    return bipWallet;
  }

  public BipFormat getBipFormat() {
    return bipFormat;
  }

  public WhirlpoolAccount getAccount() {
    return bipWallet.getAccount();
  }

  public WhirlpoolUtxoState getUtxoState() {
    return utxoState;
  }

  public boolean isAccountDeposit() {
    return WhirlpoolAccount.DEPOSIT.equals(getAccount());
  }

  public boolean isAccountPremix() {
    return WhirlpoolAccount.PREMIX.equals(getAccount());
  }

  public boolean isAccountPostmix() {
    return WhirlpoolAccount.POSTMIX.equals(getAccount());
  }

  public String getPathAddress() {
    NetworkParameters params = bipWallet.getParams();
    return bipWallet.getDerivation().getPathAddress(this, params);
  }

  @Override
  public String toString() {
    UtxoConfig utxoConfig = getUtxoConfigOrDefault();
    return getAccount()
        + " / "
        + bipWallet.getId()
        + ": "
        + UtxoUtil.getInstance().utxoToKey(this)
        + ", state={"
        + utxoState
        + "}, utxoConfig={"
        + utxoConfig
        + "}";
  }

  public String getDebug() {
    StringBuilder sb = new StringBuilder();
    sb.append(toString());
    sb.append(", path=").append(getPathAddress());

    String poolId = getUtxoState().getPoolId();
    sb.append(", poolId=").append((poolId != null ? poolId : "null"));
    sb.append(", mixsDone=").append(getMixsDone());
    sb.append(", state={").append(getUtxoState().toString()).append("}");
    return sb.toString();
  }

  @Override
  public void setConfirmInfo(UtxoConfirmInfo confirmInfo) {
    super.setConfirmInfo(confirmInfo);
    setMixableStatus();
  }
}
