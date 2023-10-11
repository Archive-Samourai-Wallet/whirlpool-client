package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.cahoots.CahootsResultImpl;
import com.samourai.whirlpool.client.tx0.Tx0Result;
import java.util.Arrays;
import java.util.Map;
import org.bitcoinj.core.Transaction;

public class Tx0x2Result extends CahootsResultImpl<Tx0x2Context, Tx0x2> {
  public Tx0x2Result(Tx0x2Context cahootsContext, Tx0x2 cahoots) {
    super(
        cahootsContext,
        cahoots,
        0, // TODO
        0, // TODO
        "Pools: " + Arrays.toString(cahoots.getPoolIds().toArray()),
        null,
        cahoots.getPsbtByPoolId().values().iterator().next());
  }

  @Override
  public void pushTx(IPushTx pushTx) throws Exception {
    for (Map.Entry<String, Transaction> e : getCahoots().getTransactions().entrySet()) {
      String poolId = e.getKey();
      Transaction tx = e.getValue();
      getCahootsContext()
          .getTx0Service()
          .pushTx0(tx, poolId, getCahootsContext().getServerApiInitiator());
    }
  }

  public Tx0Result getTx0Result() {
    return getCahootsContext().getTx0ResultInitiator();
  }
}
