package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.send.beans.SpendTx;
import com.samourai.wallet.send.exceptions.SpendException;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.samourai.wallet.util.TxUtil;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

public class MultiTx0x2 extends Cahoots {
  protected List<Tx0x2> tx0x2List;

  private MultiTx0x2() {
    ;
  }

  public MultiTx0x2(NetworkParameters params, List<Tx0x2> tx0x2List) {
    super(CahootsType.TX0X2_MULTI.getValue(), params);
    this.tx0x2List = tx0x2List;
  }

  MultiTx0x2(MultiTx0x2 multiTx0x2) {
    super(multiTx0x2);
    this.tx0x2List = new ArrayList<>();
    for (Tx0x2 tx0x2 : multiTx0x2.tx0x2List) {
      this.tx0x2List.add(tx0x2.copy());
    }
  }

  public MultiTx0x2(JSONObject obj) {
    this.fromJSON(obj);
  }

  public List<Tx0x2> getTx0x2List() {
    return tx0x2List;
  }

  protected void setTx0x2List(List<Tx0x2> tx0x2List) {
    this.tx0x2List = tx0x2List;
  }

  public Tx0x2 getTx0x2() {
    // inital tx0x2
    return tx0x2List.get(0);
  }

  public Tx0x2 getTx0x2(int index) throws Exception {
    // tx0x2 from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index);
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public Tx0x2 getTx0x2(String pool) throws Exception {
    // tx0x2 from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2;
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<Tx0x2> getTx0x2s() {
    return getTx0x2List();
  }

  @Override
  public long getFeeAmount() {
    long fees = 0L;
    for (Tx0x2 tx0x2 : tx0x2List) {
      fees += tx0x2.getFeeAmount();
    }
    return fees;
  }

  @Override
  public HashMap<String, Long> getOutpoints() {
    // inital tx0x2 outpoints
    return tx0x2List.get(0).getOutpoints();
  }

  public HashMap<String, Long> getOutpoints(int index) throws Exception {
    // tx0x2 outpoints from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index).getOutpoints();
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public HashMap<String, Long> getOutpoints(String pool) throws Exception {
    // tx0x2 outpoints from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getOutpoints();
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<HashMap<String, Long>> getOutpointsAll() {
    // all tx0x2 outpoints from tx0x2List
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getOutpoints()).collect(Collectors.toList());
  }

  @Override
  public String getDestination() {
    // inital tx0x2 destination
    return tx0x2List.get(0).getDestination();
  }

  public String getDestination(int index) throws Exception {
    // tx0x2 destination from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index).getDestination();
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public String getDestination(String pool) throws Exception {
    // tx0x2 destination from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getDestination();
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<String> getDestinations() {
    // all tx0x2 destinations from tx0x2List
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getDestination()).collect(Collectors.toList());
  }

  @Override
  public long getSpendAmount() {
    // inital tx0x2 spend amount
    return tx0x2List.get(0).getSpendAmount();
  }

  public long getSpendAmount(int index) throws Exception {
    // tx0x2 spendAmount from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index).getSpendAmount();
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public long getSpendAmount(String pool) throws Exception {
    // tx0x2 spendAmount from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getSpendAmount();
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<Long> getSpendAmounts() {
    // all tx0x2 spendAmounts from tx0x2List
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getSpendAmount()).collect(Collectors.toList());
  }

  @Override
  public Transaction getTransaction() {
    // inital tx0x2 transaction
    return tx0x2List.get(0).getTransaction();
  }

  public Transaction getTransaction(int index) throws Exception {
    // tx0x2 transaction from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index).getTransaction();
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public Transaction getTransaction(String pool) throws Exception {
    // tx0x2 transaction from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getTransaction();
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<Transaction> getTransactions() {
    // all tx0x2 transactions from tx0x2List
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getTransaction()).collect(Collectors.toList());
  }

  @Override
  public PSBT getPSBT() {
    // inital tx0x2 psbt
    return tx0x2List.get(0).getPSBT();
  }

  public PSBT getPSBT(int index) throws Exception {
    // tx0x2 psbt from tx0x2List at index; index 0 == initial pool level
    try {
      return tx0x2List.get(index).getPSBT();
    } catch (Exception e) {
      throw new Exception("Not available at index: " + index);
    }
  }

  public PSBT getPSBT(String pool) throws Exception {
    // tx0x2 psbt from tx0x2List for specified pool
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getPSBT();
      }
    }
    throw new Exception("Unavailable or unrecognized pool: " + pool);
  }

  public List<PSBT> getPSBTs() {
    // all tx0x2 psbts from tx0x2List
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getPSBT()).collect(Collectors.toList());
  }

  @Override
  public SpendTx getSpendTx(CahootsContext cahootsContext, UtxoKeyProvider utxoKeyProvider)
      throws SpendException {
    // forward initial Tx0x2 SpendTx
    CahootsContext tx0x2Context = ((MultiTx0x2Context)cahootsContext).getTx0x2ContextList().get(0);
    return getTx0x2().getSpendTx(tx0x2Context, utxoKeyProvider);
  }

  @Override
  public void signTx(HashMap<String, ECKey> keyBag) {
    for (Tx0x2 tx0x2 : tx0x2List) {
      tx0x2.signTx(keyBag);
    }
  }

  @Override
  public void pushTx(IPushTx pushTx) throws Exception {
    for (Tx0x2 tx0x2 : tx0x2List) {
      String txHex = TxUtil.getInstance().getTxHex(tx0x2.getTransaction());
      pushTx.pushTx(txHex);
    }
  }

  @Override
  public JSONObject toJSON() {
    JSONObject jsonObject = super.toJSON();
    List<JSONObject> tx0x2ObjList = new ArrayList<>();
    for (Tx0x2 tx0x2 : tx0x2List) {
      tx0x2ObjList.add(tx0x2.toJSON());
    }
    jsonObject.put("tx0x2List", tx0x2ObjList);

    return jsonObject;
  }

  @Override
  public void fromJSON(JSONObject cObj) {
    super.fromJSON(cObj);
    tx0x2List = new ArrayList<>();
    JSONArray jArray = cObj.getJSONArray("tx0x2List");
    for (int i = 0; i < jArray.length(); i++) {
      tx0x2List.add(new Tx0x2(jArray.getJSONObject(i)));
    }
  }
}
