package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.BipAddress;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfig;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolUtxo implements BipUtxo {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolUtxo.class);

  private BipUtxo utxo;
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
    super();
    this.utxo = utxo;
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
    if (!utxo.isConfirmed()) {
      return MixableStatus.UNCONFIRMED;
    }

    // ok
    return MixableStatus.MIXABLE;
  }

  // used by Sparrow
  public UtxoConfig getUtxoConfigOrDefault() {
    UtxoConfig utxoConfig = utxoConfigSupplier.getUtxo(utxo.getTxHash(), utxo.getTxOutputIndex());
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
    utxoConfigSupplier.setMixsDone(utxo.getTxHash(), utxo.getTxOutputIndex(), mixsDone);
  }

  public boolean isBlocked() {
    return getUtxoConfigOrDefault().isBlocked();
  }

  public void setBlocked(boolean blocked) {
    utxoConfigSupplier.setBlocked(utxo.getTxHash(), utxo.getTxOutputIndex(), blocked);
  }

  public String getNote() {
    return getUtxoConfigOrDefault().getNote();
  }

  public void setNote(String note) {
    utxoConfigSupplier.setNote(utxo.getTxHash(), utxo.getTxOutputIndex(), note);
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
    return bipWallet.getDerivation().getPathAddress(utxo, params);
  }

  @Override
  public String toString() {
    UtxoConfig utxoConfig = getUtxoConfigOrDefault();
    return getAccount()
        + " / "
        + bipWallet.getId()
        + ": "
        + utxo.toString()
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

  // implement BipUtxo

  @Override
  public String getTxHash() {
    return utxo.getTxHash();
  }

  @Override
  public int getTxOutputIndex() {
    return utxo.getTxOutputIndex();
  }

  @Override
  public long getValue() {
    return utxo.getValue();
  }

  @Override
  public String getAddress() {
    return utxo.getAddress();
  }

  @Override
  public Integer getConfirmedBlockHeight() {
    return utxo.getConfirmedBlockHeight();
  }

  @Override
  public void setConfirmedBlockHeight(Integer confirmedBlockHeight) {
    utxo.setConfirmedBlockHeight(confirmedBlockHeight);
    this.setMixableStatus();
  }

  @Override
  public int getConfirmations(int latestBlockHeight) {
    return utxo.getConfirmations(latestBlockHeight);
  }

  @Override
  public boolean isConfirmed() {
    return utxo.isConfirmed();
  }

  @Override
  public BipWallet getBipWallet(WalletSupplier walletSupplier) {
    return utxo.getBipWallet(walletSupplier);
  }

  @Override
  public BipAddress getBipAddress(WalletSupplier walletSupplier) {
    return utxo.getBipAddress(walletSupplier);
  }

  @Override
  public BipFormat getBipFormat(BipFormatSupplier bipFormatSupplier, NetworkParameters params) {
    return bipFormatSupplier.findByAddress(getAddress(), params);
  }

  @Override
  public boolean isBip47() {
    return utxo.isBip47();
  }

  @Override
  public Integer getChainIndex() {
    return utxo.getChainIndex();
  }

  @Override
  public Integer getAddressIndex() {
    return utxo.getAddressIndex();
  }

  @Override
  public byte[] getScriptBytes() {
    return utxo.getScriptBytes();
  }
}
