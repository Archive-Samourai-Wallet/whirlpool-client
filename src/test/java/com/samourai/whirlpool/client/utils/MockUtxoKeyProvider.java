package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipFormat.BipFormatSupplierImpl;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutPoint;

public class MockUtxoKeyProvider implements UtxoKeyProvider {
  private BipFormatSupplier bipFormatSupplier;
  private Map<String, ECKey> keys = new LinkedHashMap<String, ECKey>();

  public MockUtxoKeyProvider() {
    bipFormatSupplier = new BipFormatSupplierImpl();
  }

  public void setKey(TransactionOutPoint outPoint, ECKey key) {
    keys.put(outPoint.toString(), key);
  }

  @Override
  public byte[] _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    ECKey ecKey = keys.get(utxoHash + ":" + utxoIndex);
    if (ecKey == null) {
      return null;
    }
    return ecKey.getPrivKeyBytes();
  }

  @Override
  public BipFormatSupplier getBipFormatSupplier() {
    return bipFormatSupplier;
  }
}
