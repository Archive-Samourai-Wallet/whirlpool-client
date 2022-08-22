package com.samourai.whirlpool.client.wallet;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.util.MessageListener;
import com.samourai.whirlpool.client.event.UtxoChangesEvent;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
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

  protected WhirlpoolWallet computeWhirlpoolWallet() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);

    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    WhirlpoolWalletConfig whirlpoolWalletConfig = computeWhirlpoolWalletConfig();
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, seed, passphrase);
    whirlpoolWallet = whirlpoolWalletService.openWallet(whirlpoolWallet, passphrase);
    return whirlpoolWallet;
  }

  protected void mockUtxos(UnspentOutput... unspentOutputs) throws Exception {
    UtxoData utxoData = new UtxoData(unspentOutputs, new WalletResponse.Tx[] {});
    ((BasicUtxoSupplier) whirlpoolWallet.getUtxoSupplier()).setValue(utxoData);
  }
}
