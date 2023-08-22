package com.samourai.whirlpool.client.wallet;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxoChangesEvent;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.BeforeEach;

public class AbstractWhirlpoolWalletTest extends AbstractTest {
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;

  protected WhirlpoolUtxoChanges lastUtxoChanges;
  protected WhirlpoolWallet whirlpoolWallet;
  protected WhirlpoolWalletConfig config;

  public AbstractWhirlpoolWalletTest() throws Exception {
    super();
  }

  @BeforeEach
  public void setup() throws Exception {
    WhirlpoolEventService.getInstance()
        .register(
            new MessageListener<UtxoChangesEvent>() {
              @Subscribe
              @Override
              public void onMessage(UtxoChangesEvent message) {
                lastUtxoChanges = message.getUtxoData().getUtxoChanges();
              }
            });

    whirlpoolWallet = computeWhirlpoolWallet();
    utxoSupplier = whirlpoolWallet.getUtxoSupplier();
    utxoConfigSupplier = whirlpoolWallet.getUtxoConfigSupplier();
    config = whirlpoolWallet.getConfig();
  }

  @Override
  protected void onPushTx0(Tx0PushRequest request, Transaction tx) throws Exception {
    super.onPushTx0(request, tx);

    // mock utxos from tx0 outputs
    List<UnspentOutput> unspentOutputs = new LinkedList<>();
    for (TransactionOutput txOut : tx.getOutputs()) {
      HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
      UnspentOutput unspentOutput =
          newUnspentOutput(
              tx.getHashAsString(), txOut.getIndex(), txOut.getValue().getValue(), address);
      unspentOutputs.add(unspentOutput);
    }
    mockUtxos(unspentOutputs.toArray(new UnspentOutput[] {}));
  }

  protected WhirlpoolWallet computeWhirlpoolWallet() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);

    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    WhirlpoolWalletConfig whirlpoolWalletConfig = computeWhirlpoolWalletConfig();
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, seed, passphrase);
    whirlpoolWallet = whirlpoolWalletService.openWallet(whirlpoolWallet, passphrase);

    // reset
    for (BipWallet bipWallet : whirlpoolWallet.getWalletSupplier().getWallets()) {
      bipWallet.getIndexHandlerReceive().set(0, true);
      bipWallet.getIndexHandlerChange().set(0, true);
    }
    return whirlpoolWallet;
  }

  protected void mockUtxos(UnspentOutput... unspentOutputs) throws Exception {
    UtxoData utxoData = new UtxoData(unspentOutputs, new WalletResponse.Tx[] {});
    ((BasicUtxoSupplier) whirlpoolWallet.getUtxoSupplier())._mockValue(utxoData);
  }

  protected boolean utxosContains(
      Collection<UnspentOutput> unspentOutputs, String hash, int index) {
    return unspentOutputs.stream()
            .filter(
                unspentOutput ->
                    unspentOutput.tx_hash.equals(hash) && index == unspentOutput.tx_output_n)
            .count()
        > 0;
  }

  protected Collection<Pool> findPoolsLowerOrEqual(String maxPoolId, PoolSupplier poolSupplier) {
    Pool highestPool = poolSupplier.findPoolById(maxPoolId);
    return poolSupplier.getPools().stream()
        .filter(pool -> pool.getDenomination() <= highestPool.getDenomination())
        .collect(Collectors.toList());
  }
}
