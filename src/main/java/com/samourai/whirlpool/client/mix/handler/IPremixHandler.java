package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.utxo.UtxoDetail;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;

public interface IPremixHandler {

  UtxoDetail getUtxo();

  void signTransaction(Transaction tx, int inputIndex, NetworkParameters params) throws Exception;

  String signMessage(String message);

  String computeUserHash(String salt);
}
