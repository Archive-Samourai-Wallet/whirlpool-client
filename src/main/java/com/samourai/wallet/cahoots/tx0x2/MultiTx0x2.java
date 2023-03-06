package com.samourai.wallet.cahoots.tx0x2;

 import com.samourai.soroban.cahoots.CahootsContext;
 import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;

 import java.util.HashMap;
 import java.util.List;

 import com.samourai.wallet.cahoots.psbt.PSBT;
 import com.samourai.wallet.cahoots.stonewallx2.STONEWALLx2;
 import com.samourai.wallet.cahoots.stowaway.Stowaway;
 import com.samourai.wallet.send.beans.SpendTx;
 import com.samourai.wallet.send.exceptions.SpendException;
 import com.samourai.wallet.send.provider.UtxoKeyProvider;
 import org.bitcoinj.core.ECKey;
 import org.bitcoinj.core.NetworkParameters;
 import org.bitcoinj.core.Transaction;
 import org.json.JSONObject;

public class MultiTx0x2 extends Cahoots {
  protected List<Tx0x2> tx0x2List;

  private MultiTx0x2() { ; }

  public MultiTx0x2(NetworkParameters params, List<Tx0x2> tx0x2List) {
    super(CahootsType.TX0X2_MULTI.getValue(), params);
    this.tx0x2List = tx0x2List;
  }

  MultiTx0x2(MultiTx0x2 multiTx0x2) {
    super(multiTx0x2);
      for (Tx0x2 tx0x2 : multiTx0x2.tx0x2List) {
        this.tx0x2List.add(tx0x2.copy());
      }
  }

  public MultiTx0x2(JSONObject obj) {
    this.fromJSON(obj);
  }

  protected List<Tx0x2> getTx0x2List() {
    return tx0x2List;
  }

  protected void setTx0x2List(List<Tx0x2> tx0x2List) {
    this.tx0x2List = tx0x2List;
  }

  @Override
  public JSONObject toJSON() {
    // TODO fix for list
    JSONObject jsonObject = super.toJSON();
    jsonObject.put("tx0x2List", tx0x2List.get(0).toJSON());
    return jsonObject;
  }

  @Override
  public void fromJSON(JSONObject cObj) {
    // TODO fix for list
    super.fromJSON(cObj);
    Tx0x2 obj = tx0x2List.get(0);
    obj = new Tx0x2(cObj.getJSONObject("tx0x2List"));
  }

  // TODO cleanup methods below
  @Override
  public void signTx(HashMap<String, ECKey> hashMap) {

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
    // inital tx0 outpoints
    return tx0x2List.get(0).getOutpoints();
  }

  @Override
  public String getDestination() {
    // inital tx0 destination
    return tx0x2List.get(0).getDestination();
  }

  @Override
  public long getSpendAmount() {
    // inital tx0 spend amount
    return tx0x2List.get(0).getSpendAmount();
  }

  @Override
  public Transaction getTransaction() {
    // inital tx0 transaction
    return tx0x2List.get(0).getTransaction();
  }

  @Override
  public PSBT getPSBT() {
    // inital tx0 psbt
    return tx0x2List.get(0).getPSBT();
  }

  @Override
  public SpendTx getSpendTx(CahootsContext cahootsContext, UtxoKeyProvider utxoKeyProvider) throws SpendException {
    // TODO
    return null;
  }
}
