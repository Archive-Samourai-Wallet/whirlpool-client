package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiTx0x2Context extends CahootsContext {
  private List<Tx0x2Context> tx0x2ContextList;

  protected MultiTx0x2Context(
      CahootsWallet cahootsWallet,
      CahootsTypeUser typeUser,
      int account,
      Long feePerB,
      Tx0Service tx0Service,
      List<Tx0> tx0Initiators
  ) {
    super(
        cahootsWallet,
        typeUser,
        CahootsType.TX0X2_MULTI,
        account,
        feePerB,
        tx0Initiators != null ? Tx0Service.getTx0ListAmount(tx0Initiators) : null,
        null);
    this.tx0x2ContextList = computeTx0x2Context(tx0Service, tx0Initiators);
  }

  public static MultiTx0x2Context newInitiator(
          CahootsWallet cahootsWallet,
          int account,
          long feePerB,
          Tx0Service tx0Service,
          List<Tx0> tx0Initiators) {
    return new MultiTx0x2Context(
            cahootsWallet, CahootsTypeUser.SENDER, account, feePerB, tx0Service, tx0Initiators);
  }

  public static MultiTx0x2Context newCounterparty(
          CahootsWallet cahootsWallet, int account, Tx0Service tx0Service) {
    return new MultiTx0x2Context(
            cahootsWallet, CahootsTypeUser.COUNTERPARTY, account, null, tx0Service, null);
  }

  private List<Tx0x2Context> computeTx0x2Context(Tx0Service tx0Service, List<Tx0> tx0Initiators) {
    List<Tx0x2Context> tx0x2ContextList = new ArrayList<>();
    if (getTypeUser().equals(CahootsTypeUser.COUNTERPARTY)) {
      // TODO - Make this better
      for (int i = 0; i < 4; i++) {
        tx0x2ContextList.add(Tx0x2Context.newCounterparty(getCahootsWallet(), getAccount(), tx0Service));
      }
      return tx0x2ContextList;
    }

    for(Tx0 tx0Initiator : tx0Initiators) {
      tx0x2ContextList.add(
        Tx0x2Context.newInitiator(getCahootsWallet(), getAccount(),getFeePerB(), tx0Service, tx0Initiator)
      );
    }
    return tx0x2ContextList;
  }

  // TODO temp maybe delete this
//  private Tx0x2Context computeSingleTx0x2Context(Tx0Service tx0Service, List<Tx0> tx0Initiators) {
//    if (getTypeUser().equals(CahootsTypeUser.COUNTERPARTY)) {
//      return Tx0x2Context.newCounterparty(getCahootsWallet(), getAccount(), tx0Service);
//    }
//    return Tx0x2Context.newInitiator(getCahootsWallet(), getAccount(),getFeePerB(), tx0Service, tx0Initiators.get(0));
//  }

  public List<Tx0x2Context> getTx0x2ContextList() {
    return tx0x2ContextList;
  }

  private Long getTotalAmount(List<Tx0> tx0List) {
    long amount = 0L;
    for (Tx0 tx0 : tx0List) {
     amount += (tx0.getSpendValue() - tx0.getTx0MinerFee());
    }
    return amount;
  }
}
