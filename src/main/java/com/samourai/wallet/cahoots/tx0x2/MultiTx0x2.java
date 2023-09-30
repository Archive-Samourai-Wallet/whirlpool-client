package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.send.beans.SpendTx;
import com.samourai.wallet.send.exceptions.SpendException;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.TxUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

public class MultiTx0x2 extends Cahoots {
  // tx0x2 from tx0x2List at index; index 0 == initial pool level
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

  @Override
  public long getFeeAmount() {
    long fees = 0L;
    for (Tx0x2 tx0x2 : tx0x2List) {
      fees += tx0x2.getFeeAmount();
    }
    return fees;
  }

  // inital tx0x2 outpoints
  @Override
  public HashMap<String, Long> getOutpoints() {
    return tx0x2List.get(0).getOutpoints();
  }

  // inital tx0x2 destination
  @Override
  public String getDestination() {
    return tx0x2List.get(0).getDestination();
  }

  // inital tx0x2 spend amount
  @Override
  public long getSpendAmount() {
    return tx0x2List.get(0).getSpendAmount();
  }

  // inital tx0x2 transaction
  @Override
  public Transaction getTransaction() {
    return tx0x2List.get(0).getTransaction();
  }

  // tx0x2 transaction from tx0x2List for specified pool
  public Transaction getTransaction(String pool) throws Exception {
    for (Tx0x2 tx0x2 : tx0x2List) {
      if (tx0x2.getPoolId().equals(pool)) {
        return tx0x2.getTransaction();
      }
    }
    throw new Exception("Transaction not found for pool: " + pool);
  }

  // all tx0x2 transactions from tx0x2List
  public List<Transaction> getTransactions() {
    return tx0x2List.stream().map(tx0x2 -> tx0x2.getTransaction()).collect(Collectors.toList());
  }

  // inital tx0x2 psbt
  @Override
  public PSBT getPSBT() {
    return tx0x2List.get(0).getPSBT();
  }

  // forward initial Tx0x2 SpendTx
  @Override
  public SpendTx getSpendTx(CahootsContext cahootsContext, UtxoKeyProvider utxoKeyProvider)
      throws SpendException {
    CahootsContext tx0x2Context = ((MultiTx0x2Context) cahootsContext).getTx0x2ContextList().get(0);
    return tx0x2List.get(0).getSpendTx(tx0x2Context, utxoKeyProvider);
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
