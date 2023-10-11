package com.samourai.whirlpool.client.wallet;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.util.MessageListener;
import com.samourai.wallet.utxo.BipUtxo;
import com.samourai.wallet.utxo.UtxoOutPoint;
import com.samourai.wallet.utxo.UtxoRef;
import com.samourai.whirlpool.client.event.UtxoChangesEvent;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.junit.jupiter.api.Assertions;

public class AbstractWhirlpoolWalletTest extends AbstractTest {
  protected UtxoSupplier utxoSupplier;
  protected UtxoConfigSupplier utxoConfigSupplier;

  protected WhirlpoolUtxoChanges lastUtxoChanges;
  protected WhirlpoolWallet whirlpoolWallet;
  private int newUtxoIndex;

  public AbstractWhirlpoolWalletTest() throws Exception {
    super();
  }

  public void setup(boolean isOpReturnV0) throws Exception {
    super.setup(isOpReturnV0);
    WhirlpoolEventService.getInstance()
        .register(
            new MessageListener<UtxoChangesEvent>() {
              @Subscribe
              @Override
              public void onMessage(UtxoChangesEvent message) {
                lastUtxoChanges = message.getUtxoData().getUtxoChanges();
              }
            });

    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    whirlpoolWallet = computeWhirlpoolWallet(seedWords, passphrase, whirlpoolWalletConfig);
    utxoSupplier = whirlpoolWallet.getUtxoSupplier();
    utxoConfigSupplier = whirlpoolWallet.getUtxoConfigSupplier();
    newUtxoIndex = 61;
  }

  @Override
  protected void onPushTx0(Tx0PushRequest request, Transaction tx) throws Exception {
    super.onPushTx0(request, tx);

    // mock utxos from tx0 outputs
    List<BipUtxo> unspentOutputs = new LinkedList<>();
    for (TransactionOutput txOut : tx.getOutputs()) {
      HD_Address address = whirlpoolWallet.getWalletDeposit().getAddressAt(0, 61).getHdAddress();
      BipUtxo unspentOutput =
          newUtxo(tx.getHashAsString(), txOut.getIndex(), txOut.getValue().getValue(), address);
      unspentOutputs.add(unspentOutput);
    }
    mockUtxos(unspentOutputs.toArray(new BipUtxo[] {}));
  }

  protected WhirlpoolWallet computeWhirlpoolWallet(
      String seedWords, String passphrase, WhirlpoolWalletConfig whirlpoolWalletConfig)
      throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);

    WhirlpoolWalletService whirlpoolWalletService = new WhirlpoolWalletService();
    WhirlpoolWallet whirlpoolWallet = new WhirlpoolWallet(whirlpoolWalletConfig, seed, passphrase);
    whirlpoolWalletService.openWallet(whirlpoolWallet, passphrase);

    // reset
    for (BipWallet bipWallet : whirlpoolWallet.getWalletSupplier().getWallets()) {
      bipWallet.getIndexHandlerReceive().set(0, true);
      bipWallet.getIndexHandlerChange().set(0, true);
    }
    return whirlpoolWallet;
  }

  protected List<BipUtxo> mockUtxos(BipUtxo... unspentOutputs) throws Exception {
    UtxoData utxoData = new UtxoData(unspentOutputs, new WalletResponse.Tx[] {});
    ((BasicUtxoSupplier) whirlpoolWallet.getUtxoSupplier())._mockValue(utxoData);
    return Arrays.asList(unspentOutputs);
  }

  protected void assertUtxosEquals(
      Collection<UtxoOutPoint> utxos1, Collection<? extends BipUtxo> utxos2) {
    Assertions.assertEquals(utxos1.size(), utxos2.size());
    for (UtxoOutPoint utxo1 : utxos1) {
      Assertions.assertTrue(utxosContains(utxos2, utxo1));
    }
  }

  protected boolean utxosContains(Collection<? extends UtxoRef> unspentOutputs, UtxoRef utxo) {
    return unspentOutputs.stream()
            .filter(
                unspentOutput ->
                    unspentOutput.getTxHash().equals(utxo.getTxHash())
                        && utxo.getTxOutputIndex() == unspentOutput.getTxOutputIndex())
            .count()
        > 0;
  }

  protected boolean utxosContains(
      Collection<? extends BipUtxo> unspentOutputs, String hash, int index) {
    return unspentOutputs.stream()
            .filter(
                unspentOutput ->
                    unspentOutput.getTxHash().equals(hash)
                        && index == unspentOutput.getTxOutputIndex())
            .count()
        > 0;
  }

  protected BipUtxo newUtxo(String hash, int index, long value) throws Exception {
    HD_Address hdAddress =
        whirlpoolWallet.getWalletDeposit().getAddressAt(0, newUtxoIndex++).getHdAddress();
    return newUtxo(hash, index, value, hdAddress);
  }
}
