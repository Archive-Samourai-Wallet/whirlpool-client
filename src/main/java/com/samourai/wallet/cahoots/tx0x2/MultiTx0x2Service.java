package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.TypeInteraction;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.AbstractCahootsService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import java.util.ArrayList;
import java.util.List;

import com.samourai.wallet.util.TxUtil;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTx0x2Service extends AbstractCahootsService<MultiTx0x2, MultiTx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(MultiCahootsService.class);
  private Tx0x2Service tx0x2Service;

  public MultiTx0x2Service(
      BipFormatSupplier bipFormatSupplier, NetworkParameters params, Tx0x2Service tx0x2Service) {
    super(CahootsType.TX0X2_MULTI, bipFormatSupplier, params, TypeInteraction.TX_BROADCAST_MULTI);
    this.tx0x2Service = tx0x2Service;
  }

  //
  // sender: step 0
  //
  @Override
  public MultiTx0x2 startInitiator(MultiTx0x2Context multiTx0x2Context) throws Exception {
    List<Tx0x2Context> tx0x2ContextList = multiTx0x2Context.getTx0x2ContextList();
    Tx0x2 payload0;
    List<Tx0x2> tx0x2List = new ArrayList<>();
    for (Tx0x2Context tx0x2Context : tx0x2ContextList) {
      payload0 = tx0x2Service.startInitiator(tx0x2Context);
      tx0x2List.add(payload0);
    }
    MultiTx0x2 multiPayload0 = new MultiTx0x2(params, tx0x2List);

    if (log.isDebugEnabled()) {
      log.debug("# MULTI Tx0x2 INITIATOR => step=" + multiPayload0.getStep());
    }
    return multiPayload0;
  }

  //
  // counterparty: step 1
  //
  @Override
  public MultiTx0x2 startCollaborator(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    MultiTx0x2 multiPayload1 = doStep1(multiTx0x2Context, multiTx0x2);
    if (log.isDebugEnabled()) {
      log.debug("# MULTI Tx0x2 COUNTERPARTY => step=" + multiPayload1.getStep());
    }
    return multiPayload1;
  }

  @Override
  public MultiTx0x2 reply(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    int step = multiTx0x2.getStep();
    if (log.isDebugEnabled()) {
      log.debug("# MultiTx0x2 " + multiTx0x2Context.getTypeUser() + " <= step=" + step);
    }
    MultiTx0x2 payload;
    switch (step) {
      case 1:
        // sender
        payload = doStep2(multiTx0x2Context, multiTx0x2);
        break;
      case 2:
        // counterparty
        payload = doStep3(multiTx0x2Context, multiTx0x2);
        break;
      case 3:
        // sender
        payload = doStep4(multiTx0x2Context, multiTx0x2);
        break;
      default:
        throw new Exception("Unrecognized #Cahoots step");
    }
    if (payload == null) {
      throw new Exception("Cannot compose #Cahoots");
    }
    if (log.isDebugEnabled()) {
      log.debug(
          "# MultiTx0x2 " + multiTx0x2Context.getTypeUser() + " => step=" + payload.getStep());
    }
    return payload;
  }

  //
  // counterparty
  //
  private MultiTx0x2 doStep1(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    debug("BEGINING MULTI TX0X2 STEP 1", multiTx0x2Context);

    Tx0x2 payload1;
    List<Tx0x2> tx0x2List = new ArrayList<>();
    TransactionOutput higherPoolChange = null;
    for (int i = 0; i < multiTx0x2.getTx0x2List().size(); i++) {
      payload1 = tx0x2Service.doMultiStep1(
          multiTx0x2.getTx0x2List().get(i),
          multiTx0x2Context.getTx0x2ContextList().get(i),
          higherPoolChange);
      tx0x2List.add(payload1);

      higherPoolChange = payload1.getTransaction().getOutput(
          payload1.getTransaction().getOutputs().size() - 1);
    }

    MultiTx0x2 multiPayload1 = new MultiTx0x2(params, tx0x2List);
    multiPayload1.setStep(1);

    debug("END MULTI TX0X2 STEP 1", multiTx0x2Context);
    return multiPayload1;
  }

  //
  // sender
  //
  private MultiTx0x2 doStep2(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    debug("BEGING MULTI TX0X2 STEP 2", multiTx0x2Context);

    Tx0x2 payload2;
    List<Tx0x2> tx0x2List = new ArrayList<>();
    TransactionOutput higherPoolSenderChange = null;
    TransactionOutput higherPoolCounterpartyChange = null;
    for (int i = 0; i < multiTx0x2.getTx0x2List().size(); i++) {
      payload2 = tx0x2Service.doMultiStep2(
          multiTx0x2.getTx0x2List().get(i),
          multiTx0x2Context.getTx0x2ContextList().get(i),
          higherPoolSenderChange,
          higherPoolCounterpartyChange);
      tx0x2List.add(payload2);

      // higher pool changes used for lower pools
      higherPoolSenderChange = payload2.getTransaction().getOutput(
          payload2.getTransaction().getOutputs().size() - 1);

      higherPoolCounterpartyChange =
          TxUtil.getInstance().findOutputByAddress(
              multiTx0x2.getTx0x2List().get(i).getTransaction(),
              multiTx0x2.getTx0x2List().get(i).getCollabChange(),
              getBipFormatSupplier());
    }

    MultiTx0x2 multiPayload2 = new MultiTx0x2(params, tx0x2List);
    multiPayload2.setStep(2);

    debug("END MULTI TX0X2 STEP 2", multiTx0x2Context);
    return multiPayload2;
  }

  //
  // counterparty
  //
  private MultiTx0x2 doStep3(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    debug("BEGING MULTI TX0X2 STEP 3", multiTx0x2Context);

    Tx0x2 payload3;
    List<Tx0x2> tx0x2List = new ArrayList<>();
    int nbTx0x2s = multiTx0x2.getTx0x2List().size();
    for (int i = 0; i < nbTx0x2s; i++) {
      if (i != 0) {
        // lower pools; used for computeMaxSpendAmount()
        multiTx0x2Context.getTx0x2ContextList().get(i).setLowerPool(true);
      }
      if (i == nbTx0x2s - 1) {
        // 0.001btc pool; used for computeMaxSpendAmount()
        multiTx0x2Context.getTx0x2ContextList().get(i).setBottomPool(true);
      }

      payload3 = tx0x2Service.doStep3(
          multiTx0x2.getTx0x2List().get(i),
          multiTx0x2Context.getTx0x2ContextList().get(i));
      tx0x2List.add(payload3);
    }

    MultiTx0x2 multiPayload3 = new MultiTx0x2(params, tx0x2List);
    multiPayload3.setStep(3);

    debug("END MULTI TX0X2 STEP 3", multiTx0x2Context);
    return multiPayload3;
  }

  //
  // sender
  //
  private MultiTx0x2 doStep4(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2)
      throws Exception {
    debug("BEGING MULTI TX0X2 STEP 4", multiTx0x2Context);

    Tx0x2 payload4;
    List<Tx0x2> tx0x2List = new ArrayList<>();
    int nbTx0x2s = multiTx0x2.getTx0x2List().size();
    for (int i = 0; i < nbTx0x2s; i++) {
      if (i != 0) {
        // lower pools; used for computeMaxSpendAmount()
        multiTx0x2Context.getTx0x2ContextList().get(i).setLowerPool(true);
      }
      if (i == nbTx0x2s - 1) {
        // 0.001btc pool; used for computeMaxSpendAmount()
        multiTx0x2Context.getTx0x2ContextList().get(i).setBottomPool(true);
      }

      payload4 = tx0x2Service.doStep4(
          multiTx0x2.getTx0x2List().get(i),
          multiTx0x2Context.getTx0x2ContextList().get(i));
      tx0x2List.add(payload4);
    }

    MultiTx0x2 multiPayload4 = new MultiTx0x2(params, tx0x2List);
    multiPayload4.setStep(4);

    debug("END MULTI TX0X2 STEP 4", multiTx0x2Context);
    return multiPayload4;
  }

  protected void debug(String info, MultiTx0x2Context multiTx0x2Context) {
    if (log.isDebugEnabled()) {
      log.debug(
          "###### "
              + info
              + " "
              + multiTx0x2Context.getCahootsType()
              + "/"
              + multiTx0x2Context.getTypeUser());
    }
  }
}
