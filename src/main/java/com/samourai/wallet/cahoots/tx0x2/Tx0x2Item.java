package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.wallet.util.Z85;
import com.samourai.whirlpool.client.tx0.Tx0;
import org.json.JSONObject;

// Tx0 info transmitted to counterparty
public class Tx0x2Item {
  private static final Z85 z85 = Z85.getInstance();

  // Tx0Data
  private String feePaymentCode;
  private byte[] feePayload;

  // Tx0
  private long premixValue;
  private int nbPremixCounterparty;
  private long changeAmountCounterparty;

  public Tx0x2Item(Tx0 tx0) {
    this(
        tx0.getTx0Data().getFeePaymentCode(),
        tx0.getTx0Data().getFeePayload(),
        tx0.getPremixValue(),
        tx0.getTx0x2Preview().getNbPremixCounterparty(),
        tx0.getTx0x2Preview().getChangeAmountCounterparty());
  }

  public Tx0x2Item(
      String feePaymentCode,
      byte[] feePayload,
      long premixValue,
      int nbPremixCounterparty,
      long changeAmountCounterparty) {
    this.feePaymentCode = feePaymentCode;
    this.feePayload = feePayload;
    this.premixValue = premixValue;
    this.nbPremixCounterparty = nbPremixCounterparty;
    this.changeAmountCounterparty = changeAmountCounterparty;
  }

  public JSONObject toJSONObjectCahoots() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("feePaymentCode", feePaymentCode);
    jsonObject.put("feePayload", z85.encode(feePayload));
    jsonObject.put("premixValue", premixValue);
    jsonObject.put("nbPremixCounterparty", nbPremixCounterparty);
    jsonObject.put("changeAmountCounterparty", changeAmountCounterparty);
    return jsonObject;
  }

  public static Tx0x2Item fromJSONObjectCahoots(JSONObject jsonObject) {
    String feePaymentCode = jsonObject.getString("feePaymentCode");
    byte[] feePayload = z85.decode(jsonObject.getString("feePayload"));
    long premixValue = jsonObject.getLong("premixValue");
    int nbPremixCounterparty = jsonObject.getInt("nbPremixCounterparty");
    long changeAmountCounterparty = jsonObject.getLong("changeAmountCounterparty");
    return new Tx0x2Item(
        feePaymentCode, feePayload, premixValue, nbPremixCounterparty, changeAmountCounterparty);
  }

  public String getFeePaymentCode() {
    return feePaymentCode;
  }

  public byte[] getFeePayload() {
    return feePayload;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public int getNbPremixCounterparty() {
    return nbPremixCounterparty;
  }

  public long getChangeAmountCounterparty() {
    return changeAmountCounterparty;
  }
}
