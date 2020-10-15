package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoConfigSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolUtxo extends WhirlpoolUtxoConfig {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolUtxo.class);

  private UnspentOutput utxo;
  private WhirlpoolAccount account;
  private WhirlpoolUtxoState utxoState;

  private UtxoConfigSupplier utxoConfigSupplier;

  public WhirlpoolUtxo(
      UnspentOutput utxo,
      WhirlpoolAccount account,
      WhirlpoolUtxoStatus status,
      UtxoConfigSupplier utxoConfigSupplier) {
    super();
    this.utxo = utxo;
    this.account = account;
    this.utxoState = new WhirlpoolUtxoState(status);
    this.utxoConfigSupplier = utxoConfigSupplier;
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

  public void _setUtxo(UnspentOutput utxo) {
    this.utxo = utxo;
  }

  public WhirlpoolAccount getAccount() {
    return account;
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

  @Override
  public String toString() {
    return account + ": " + utxo.toString() + ": " + utxoState + " ; " + getUtxoConfigPersisted();
  }
}
