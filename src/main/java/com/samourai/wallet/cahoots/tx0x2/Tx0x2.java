package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.util.Z85;
import com.samourai.wallet.utxo.UtxoOutPoint;
import com.samourai.wallet.utxo.UtxoOutPointImpl;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import java.util.*;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

public class Tx0x2 extends Cahoots<Tx0x2Context> {
  // set on step0 by initiator
  private List<String> poolIds;
  private long minSpendValueEach;
  private long maxSpendValueEach;

  // set on step1 by counterparty
  private Map<String, String>
      counterpartyChangeAddressByPoolId; // counterparty change addresses (1 per pool)
  private Collection<UtxoOutPoint> counterpartyInputs; // counterparty inputs for initial pool

  // set on step2 by initiator
  Map<String, Tx0x2Item> tx0x2ItemByPoolId;
  private Map<String, PSBT> psbtByPoolId = null;
  private long totalSamouraiFeeCounterparty; // total samouraiFee for counterparty
  private long totalMinerFeeCounterparty; // total minerFee for counterparty

  private Tx0x2() {
    ;
  }

  private Tx0x2(Tx0x2 c) {
    super(c);
    this.poolIds = c.poolIds;
    this.minSpendValueEach = c.minSpendValueEach;
    this.maxSpendValueEach = c.maxSpendValueEach;
    this.tx0x2ItemByPoolId = c.tx0x2ItemByPoolId;
    this.counterpartyChangeAddressByPoolId = c.counterpartyChangeAddressByPoolId;
    this.counterpartyInputs = c.counterpartyInputs;
    this.psbtByPoolId = c.psbtByPoolId;
    this.totalSamouraiFeeCounterparty = c.totalSamouraiFeeCounterparty;
    this.totalMinerFeeCounterparty = c.totalMinerFeeCounterparty;
  }

  public Tx0x2(JSONObject obj) {
    this.fromJSON(obj);
  }

  public Tx0x2(
      NetworkParameters params,
      List<String> poolIds,
      long minSpendValueEach,
      long maxSpendValueEach) {
    super(CahootsType.TX0X2.getValue(), params);
    this.poolIds = poolIds;
    this.minSpendValueEach = minSpendValueEach;
    this.maxSpendValueEach = maxSpendValueEach;
    this.tx0x2ItemByPoolId = null;
    this.counterpartyChangeAddressByPoolId = null;
    this.counterpartyInputs = null;
    this.psbtByPoolId = null;
    this.totalSamouraiFeeCounterparty = 0;
    this.totalMinerFeeCounterparty = 0;
  }

  public void doStep1(
      Map<String, String> counterpartyChangeAddressByPoolId,
      Collection<UtxoOutPoint> counterpartyInputs) {
    this.counterpartyChangeAddressByPoolId = counterpartyChangeAddressByPoolId;
    this.counterpartyInputs = counterpartyInputs;
    this.setStep(1);
  }

  public void doStep2(Tx0Result tx0Result) {
    // update tx0x2Info
    this.tx0x2ItemByPoolId = new LinkedHashMap<>();
    for (Tx0 tx0 : tx0Result.getList()) {
      String poolId = tx0.getPool().getPoolId();
      tx0x2ItemByPoolId.put(poolId, new Tx0x2Item(tx0));
    }

    // update psbt
    this.psbtByPoolId = new LinkedHashMap<>();
    for (Tx0 tx0 : tx0Result.getList()) {
      psbtByPoolId.put(tx0.getPool().getPoolId(), new PSBT(tx0.getTx()));
    }

    totalSamouraiFeeCounterparty =
        tx0Result.getList().stream()
            .mapToLong(tx0 -> tx0.getTx0x2CahootsResult().getSamouraiFeeCounterparty())
            .sum();
    totalMinerFeeCounterparty =
        tx0Result.getList().stream()
            .mapToLong(tx0 -> tx0.getTx0x2CahootsResult().getTx0MinerFeeCounterparty())
            .sum();
    this.setStep(2);
  }

  public void doStep3(Map<String, PSBT> psbtByPoolId) {
    this.psbtByPoolId = psbtByPoolId;
    this.setStep(3);
  }

  public void doStep4(Map<String, PSBT> psbtByPoolId) {
    this.psbtByPoolId = psbtByPoolId;
    this.setStep(4);
  }

  public Tx0x2 copy() {
    return new Tx0x2(this);
  }

  @Override
  protected JSONObject toJSONObjectCahoots() throws Exception {
    JSONObject obj = super.toJSONObjectCahoots();
    obj.put("poolIds", poolIds);
    obj.put("minSpendValue", minSpendValueEach);
    obj.put("maxSpendValue", maxSpendValueEach);

    JSONArray jsonArray = new JSONArray();
    if (this.tx0x2ItemByPoolId != null) {
      for (Map.Entry<String, Tx0x2Item> e : tx0x2ItemByPoolId.entrySet()) {
        JSONObject entry = e.getValue().toJSONObjectCahoots();
        entry.put("key", e.getKey());
        jsonArray.put(entry);
      }
    }
    obj.put("tx0x2ItemByPoolId", jsonArray);

    jsonArray = new JSONArray();
    if (this.counterpartyChangeAddressByPoolId != null) {
      for (Map.Entry<String, String> e : counterpartyChangeAddressByPoolId.entrySet()) {
        JSONObject entry = new JSONObject();
        entry.put("key", e.getKey());
        entry.put("value", e.getValue());
        jsonArray.put(entry);
      }
    }
    obj.put("counterpartyChangeAddressByPoolId", jsonArray);
    jsonArray = new JSONArray();
    if (this.counterpartyInputs != null) {
      for (UtxoOutPoint o : counterpartyInputs) {
        JSONObject entry = new JSONObject();
        entry.put("txHash", o.getTxHash());
        entry.put("txOutputIndex", o.getTxOutputIndex());
        entry.put("value", o.getValueLong());
        entry.put("address", o.getAddress());
        entry.put("scriptBytes", Z85.getInstance().encode(o.getScriptBytes()));
        jsonArray.put(entry);
      }
    }
    obj.put("counterpartyInputs", jsonArray);

    jsonArray = new JSONArray();
    if (this.psbtByPoolId != null) {
      for (Map.Entry<String, PSBT> e : psbtByPoolId.entrySet()) {
        JSONObject entry = new JSONObject();
        entry.put("key", e.getKey());
        entry.put("value", Z85.getInstance().encode(e.getValue().toGZIP()));
        jsonArray.put(entry);
      }
    }
    obj.put("psbtByPoolId", jsonArray);
    obj.put("totalSamouraiFeeCounterparty", totalSamouraiFeeCounterparty);
    obj.put("totalMinerFeeCounterparty", totalMinerFeeCounterparty);
    return obj;
  }

  @Override
  protected void fromJSONObjectCahoots(JSONObject obj) throws Exception {
    super.fromJSONObjectCahoots(obj);

    this.poolIds = new LinkedList<>();
    JSONArray jsonArray = obj.getJSONArray("poolIds");
    for (int i = 0; i < jsonArray.length(); ++i) {
      this.poolIds.add(jsonArray.getString(i));
    }

    this.minSpendValueEach = obj.getLong("minSpendValue");
    this.maxSpendValueEach = obj.getInt("maxSpendValue");

    this.tx0x2ItemByPoolId = new LinkedHashMap<>();
    jsonArray = obj.getJSONArray("tx0x2ItemByPoolId");
    for (int i = 0; i < jsonArray.length(); ++i) {
      JSONObject entry = jsonArray.getJSONObject(i);
      Tx0x2Item tx0x2Item = Tx0x2Item.fromJSONObjectCahoots(entry);
      this.tx0x2ItemByPoolId.put(entry.getString("key"), tx0x2Item);
    }

    this.counterpartyChangeAddressByPoolId = new LinkedHashMap<>();
    jsonArray = obj.getJSONArray("counterpartyChangeAddressByPoolId");
    for (int i = 0; i < jsonArray.length(); ++i) {
      JSONObject entry = jsonArray.getJSONObject(i);
      this.counterpartyChangeAddressByPoolId.put(entry.getString("key"), entry.getString("value"));
    }

    this.counterpartyInputs = new LinkedList<>();
    jsonArray = obj.getJSONArray("counterpartyInputs");
    for (int i = 0; i < jsonArray.length(); ++i) {
      JSONObject entry = jsonArray.getJSONObject(i);
      UtxoOutPoint o =
          new UtxoOutPointImpl(
              entry.getString("txHash"),
              entry.getInt("txOutputIndex"),
              entry.getLong("value"),
              entry.getString("address"),
              null,
              Z85.getInstance().decode(entry.getString("scriptBytes")));
      this.counterpartyInputs.add(o);
    }

    this.psbtByPoolId = new LinkedHashMap<>();
    jsonArray = obj.getJSONArray("psbtByPoolId");
    for (int i = 0; i < jsonArray.length(); ++i) {
      JSONObject entry = jsonArray.getJSONObject(i);
      this.psbtByPoolId.put(
          entry.getString("key"),
          PSBT.fromBytes(Z85.getInstance().decode(entry.getString("value")), getParams()));
    }
    this.totalSamouraiFeeCounterparty = obj.getInt("totalSamouraiFeeCounterparty");
    this.totalMinerFeeCounterparty = obj.getInt("totalMinerFeeCounterparty");
  }

  public List<String> getPoolIds() {
    return poolIds;
  }

  public long getMinSpendValueEach() {
    return minSpendValueEach;
  }

  public long getMaxSpendValueEach() {
    return maxSpendValueEach;
  }

  public long getTotalSamouraiFeeCounterparty() {
    return totalSamouraiFeeCounterparty;
  }

  public long getTotalMinerFeeCounterparty() {
    return totalMinerFeeCounterparty;
  }

  public Map<String, Tx0x2Item> getTx0x2ItemByPoolId() {
    return tx0x2ItemByPoolId;
  }

  public Map<String, PSBT> getPsbtByPoolId() {
    return psbtByPoolId;
  }

  // all transactions from psbts
  public Map<String, Transaction> getTransactions() {
    return psbtByPoolId.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTransaction()));
  }

  public Transaction getTransaction(String poolId) {
    return psbtByPoolId.get(poolId).getTransaction();
  }

  public Map<String, String> getCounterpartyChangeAddressByPoolId() {
    return counterpartyChangeAddressByPoolId;
  }

  public Collection<UtxoOutPoint> getCounterpartyInputs() {
    return counterpartyInputs;
  }
}
