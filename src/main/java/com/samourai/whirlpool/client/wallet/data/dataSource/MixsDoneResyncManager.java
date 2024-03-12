package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.beans.TxsResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixsDoneResyncManager {
  private static final Logger log = LoggerFactory.getLogger(MixsDoneResyncManager.class);

  public MixsDoneResyncManager() {}

  public void resync(
      Collection<WhirlpoolUtxo> postmixUtxos, Map<String, TxsResponse.Tx> postmixTxs) {
    log.info("Resynchronizing mix counters...");

    int fixedUtxos = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : postmixUtxos) {
      if (log.isDebugEnabled()) {
        log.debug("resynchronizing: " + whirlpoolUtxo.getUtxo().tx_hash);
      }
      int mixsDone = recountMixsDone(whirlpoolUtxo, postmixTxs);
      if (mixsDone != whirlpoolUtxo.getMixsDone()) {
        log.info(
            "Fixed "
                + whirlpoolUtxo.getUtxo().getUtxoName()
                + ": "
                + whirlpoolUtxo.getMixsDone()
                + " => "
                + mixsDone);
        whirlpoolUtxo.setMixsDone(mixsDone);
        fixedUtxos++;
      }
    }
    log.info("Resync success: " + fixedUtxos + "/" + postmixUtxos.size() + " utxos updated.");
  }

  private int recountMixsDone(WhirlpoolUtxo whirlpoolUtxo, Map<String, TxsResponse.Tx> postmixTxs) {
    int mixsDone = 0;
    TxsResponse.Tx tx = postmixTxs.get(whirlpoolUtxo.getUtxo().tx_hash);
    while (tx != null) {
      if (tx == null || tx.inputs == null) {
        return mixsDone;
      }
      // find previous tx
      TxsResponse.Tx prevTx = null;
      for (TxsResponse.TxInput txInput : tx.inputs) {
        prevTx = postmixTxs.get(txInput.prev_out.txid);
        if (prevTx != null) {
          mixsDone++;
          break;
        }
      }
      tx = prevTx;
    }
    return mixsDone;
  }
}
