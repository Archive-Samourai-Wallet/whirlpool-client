package com.samourai.whirlpool.client.tx0x2;

import com.samourai.wallet.cahoots.Cahoots2x;
import com.samourai.wallet.cahoots.CahootsType;
import org.bitcoinj.core.NetworkParameters;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0x2 extends Cahoots2x {
  private static final Logger log = LoggerFactory.getLogger(Tx0x2.class);

  private String poolId;
  private long premixValue;
  private int maxOutputsEach;
  private long samouraiFeeValueEach;
  protected String strCollabChange = null;

  private Tx0x2() {
    ;
  }

  private Tx0x2(Tx0x2 c) {
    super(c);
    this.poolId = c.poolId;
    this.premixValue = c.premixValue;
    this.maxOutputsEach = c.maxOutputsEach;
    this.samouraiFeeValueEach = c.samouraiFeeValueEach;
    this.strCollabChange = c.strCollabChange;
  }

  public Tx0x2(JSONObject obj) {
    this.fromJSON(obj);
  }

  public Tx0x2(
      NetworkParameters params,
      byte[] fingerprint,
      String poolId,
      long premixValue,
      int maxOutputsEach,
      long samouraiFeeValueEach) {
    super(CahootsType.TX0X2.getValue(), params, -1, null, 0, fingerprint);
    this.poolId = poolId;
    this.premixValue = premixValue;
    this.maxOutputsEach = maxOutputsEach;
    this.samouraiFeeValueEach = samouraiFeeValueEach;
  }

  public Tx0x2 copy() {
    return new Tx0x2(this);
  }

  @Override
  protected JSONObject toJSONObjectCahoots() throws Exception {
    JSONObject obj = super.toJSONObjectCahoots();
    obj.put("poolId", poolId);
    obj.put("premixValue", premixValue);
    obj.put("maxOutputsEach", maxOutputsEach);
    obj.put("samouraiFeeValueEach", samouraiFeeValueEach);
    obj.put("collabChange", strCollabChange == null ? "" : strCollabChange);
    return obj;
  }

  @Override
  protected void fromJSONObjectCahoots(JSONObject obj) throws Exception {
    super.fromJSONObjectCahoots(obj);
    this.poolId = obj.getString("poolId");
    this.premixValue = obj.getLong("premixValue");
    this.maxOutputsEach = obj.getInt("maxOutputsEach");
    this.samouraiFeeValueEach = obj.getLong("samouraiFeeValueEach");
    if (obj.has("collabChange")) {
      this.strCollabChange = obj.getString("collabChange");
    } else {
      this.strCollabChange = "";
    }
  }

  public String getPoolId() {
    return poolId;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public int getMaxOutputsEach() {
    return maxOutputsEach;
  }

  public long getSamouraiFeeValueEach() {
    return samouraiFeeValueEach;
  }

  public String getCollabChange() {
    return this.strCollabChange;
  }

  public void setCollabChange(String strCollabChange) {
    this.strCollabChange = strCollabChange;
  }
}
