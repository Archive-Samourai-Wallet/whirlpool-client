package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.AddressType;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolUtxo extends WhirlpoolUtxoConfig {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolUtxo.class);

  private UnspentOutput utxo;
  private Integer blockHeight; // null when unconfirmed
  private WhirlpoolAccount account;
  private AddressType addressType;
  private WhirlpoolUtxoState utxoState;

  private UtxoConfigSupplier utxoConfigSupplier;

  public WhirlpoolUtxo(
      UnspentOutput utxo,
      Integer blockHeight,
      WhirlpoolAccount account,
      AddressType addressType,
      WhirlpoolUtxoStatus status,
      UtxoConfigSupplier utxoConfigSupplier) {
    super();
    this.utxo = utxo;
    this.blockHeight = blockHeight;
    this.account = account;
    this.addressType = addressType;
    this.utxoState = new WhirlpoolUtxoState(status);
    this.utxoConfigSupplier = utxoConfigSupplier;
  }

  public static long sumValue(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    long sumValue = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      sumValue += whirlpoolUtxo.getUtxo().value;
    }
    return sumValue;
  }

  public int computeConfirmations(int latestBlockHeight) {
    if (blockHeight == null) {
      return 0;
    }
    return latestBlockHeight - blockHeight;
  }

  @Override
  protected UtxoConfigPersisted getUtxoConfigPersisted() {
    // always fetch fresh instance from supplier
    return utxoConfigSupplier.getUtxoConfigPersisted(this);
  }

  protected UtxoConfigSupplier getUtxoConfigSupplier() {
    return utxoConfigSupplier;
  }

  public UnspentOutput getUtxo() {
    return utxo;
  }

  public void _setUtxoConfirmed(UnspentOutput utxo, Integer blockHeight) {
    this.utxo = utxo;
    this.blockHeight = blockHeight;
  }

  public Integer getBlockHeight() {
    return blockHeight;
  }

  public WhirlpoolAccount getAccount() {
    return account;
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public WhirlpoolUtxoState getUtxoState() {
    return utxoState;
  }

  public boolean isAccountDeposit() {
    return WhirlpoolAccount.DEPOSIT.equals(account);
  }

  public boolean isAccountPremix() {
    return WhirlpoolAccount.PREMIX.equals(account);
  }

  public boolean isAccountPostmix() {
    return WhirlpoolAccount.POSTMIX.equals(account);
  }

  public String getPathFull() {
    return utxo.getPathFull(addressType.getPurpose(), account.getAccountIndex());
  }

  @Override
  public String toString() {
    return account
        + " / "
        + addressType
        + ": "
        + utxo.toString()
        + ": (#"
        + (blockHeight != null ? blockHeight : "unconfirmed")
        + ") "
        + utxoState
        + " ; "
        + getUtxoConfigPersisted();
  }
}
