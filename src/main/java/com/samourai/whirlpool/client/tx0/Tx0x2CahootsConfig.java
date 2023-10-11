package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.util.UtxoUtil;
import com.samourai.wallet.utxo.UtxoOutPoint;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Tx0x2CahootsConfig {
  // counterparty inputs for initial pool
  private Collection<UtxoOutPoint> counterpartyInputs;
  // counterparty change addresses per pool
  private Map<String, String> counterpartyChangeAddressPerPoolId;

  public Tx0x2CahootsConfig(
      Collection<UtxoOutPoint> counterpartyInputs,
      Map<String, String> counterpartyChangeAddressPerPoolId) {
    this.counterpartyInputs = counterpartyInputs;
    this.counterpartyChangeAddressPerPoolId = counterpartyChangeAddressPerPoolId;
  }

  public Tx0x2CahootsConfig(Tx0x2CahootsConfig tx0x2CahootsConfig) {
    this.counterpartyInputs = tx0x2CahootsConfig.counterpartyInputs;
    this.counterpartyChangeAddressPerPoolId = tx0x2CahootsConfig.counterpartyChangeAddressPerPoolId;
  }

  public Collection<UtxoOutPoint> getCounterpartyInputs() {
    return counterpartyInputs;
  }

  public Map<String, String> getCounterpartyChangeAddressPerPoolId() {
    return counterpartyChangeAddressPerPoolId;
  }

  @Override
  public String toString() {
    return "counterpartyInputs="
        + counterpartyInputs.stream()
            .map(u -> UtxoUtil.getInstance().utxoToKey(u))
            .collect(Collectors.toList())
        + ", counterpartyChangeAddressPerPoolId="
        + counterpartyChangeAddressPerPoolId;
  }
}
