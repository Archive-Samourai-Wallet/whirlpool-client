package com.samourai.wallet.cahoots.tx0x2;

import com.samourai.soroban.cahoots.TypeInteraction;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.AbstractCahootsService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTx0x2Service extends AbstractCahootsService<MultiTx0x2, MultiTx0x2Context> {
  private static final Logger log = LoggerFactory.getLogger(MultiCahootsService.class);
  private Tx0x2Service tx0x2Service;

  public MultiTx0x2Service(BipFormatSupplier bipFormatSupplier, NetworkParameters params, Tx0x2Service tx0x2Service) {
    super(CahootsType.TX0X2_MULTI, bipFormatSupplier, params, TypeInteraction.TX_BROADCAST_MULTI);
    this.tx0x2Service = tx0x2Service;
  }

  //
  // sender: step 0
  //
  @Override
  public MultiTx0x2 startInitiator(MultiTx0x2Context multiTx0x2Context) throws Exception {
    List<Tx0x2Context> tx0x2ContextList = multiTx0x2Context.getTx0x2ContextList();
    List<Tx0x2> tx0x2List = new ArrayList<>();

    for (Tx0x2Context tx0x2Context : tx0x2ContextList) {
      Tx0x2 tx0x2Payload = tx0x2Service.startInitiator(tx0x2Context);
      tx0x2List.add(tx0x2Payload);
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
    MultiTx0x2 multiPayload1 = doStep1(multiTx0x2Context, multiTx0x2); // TODO
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
      log.debug("# MultiTx0x2 "+multiTx0x2Context.getTypeUser()+" => step="+payload.getStep());
    }
    return payload;
  }

  //
  // counterparty
  //
  private MultiTx0x2 doStep1(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2) throws Exception {
    debug("BEGINING MULTI TX0X2 STEP 1", multiTx0x2Context);

    // TODO - Work in Progress. Starting with 0.01 => 0.001 test.

    List<Tx0x2> tx0x2List = new ArrayList<>();

    // initial pool
    Tx0x2Context initialTx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
    Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(0);
    Tx0x2 payloadInitialPool = tx0x2Service.doStep1(tx0x2, initialTx0x2Context);
    tx0x2List.add(payloadInitialPool);

    // lower pool
    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(1);
    tx0x2 = multiTx0x2.getTx0x2List().get(1);
    Tx0x2 payloadLower = tx0x2Service.doMultiStep1(payloadInitialPool, initialTx0x2Context, tx0x2, tx0x2Context); // Should fail currently
    tx0x2List.add(payloadLower);

//    for (int i = 0; i < multiTx0x2.getTx0x2List().size(); i++) {
//      Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(i);
//      Tx0x2 payload = tx0x2Service.doStep1(
//          multiTx0x2.getTx0x2List().get(i),
//          multiTx0x2Context.getTx0x2ContextList().get(i));
//      tx0x2List.add(payload);
//    }

    MultiTx0x2 multiCahoots = new MultiTx0x2(multiTx0x2);
    multiCahoots.setTx0x2List(tx0x2List);
    multiCahoots.setStep(1);

    debug("END MULTI TX0X2 STEP 1", multiTx0x2Context);
    return multiCahoots;
  }

  //
  // sender
  //
  private MultiTx0x2 doStep2(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2) throws Exception {
    debug("BEGING MULTI TX0X2 STEP 2", multiTx0x2Context);

    // TODO - Work in Progress

    List<Tx0x2> tx0x2List = new ArrayList<>();
//    int size = multiTx0x2Context.getTx0x2ContextList().size();
    int size = multiTx0x2.getTx0x2List().size();

    // Initial
    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
    Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(0);
    Tx0x2 payloadInitialPool = tx0x2Service.doStep2(tx0x2, tx0x2Context);
    tx0x2List.add(payloadInitialPool);

    // lower pool
//    tx0x2 = multiTx0x2.getTx0x2List().get(1);
//    Tx0x2 payloadLower = tx0x2Service.doMultiStep2(tx0x2, tx0x2Context); // Should fail currently
//    tx0x2List.add(payloadLower);


//    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
//    for (int i = 0; i < size; i++) {
//      Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(i);
//      Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(i);
//      Tx0x2 payload = tx0x2Service.doStep2(tx0x2, tx0x2Context);
//      tx0x2List.add(payload);
//    }

    MultiTx0x2 multiCahoots = new MultiTx0x2(multiTx0x2);
    multiCahoots.setTx0x2List(tx0x2List);
    multiCahoots.setStep(2);

    debug("END MULTI TX0X2 STEP 2", multiTx0x2Context);
    return multiCahoots;
  }

  //
  // counterparty
  //
  private MultiTx0x2 doStep3(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2) throws Exception {
    debug("BEGING MULTI TX0X2 STEP 3", multiTx0x2Context);

    // TODO - Work in Progress

    List<Tx0x2> tx0x2List = new ArrayList<>();
//    int size = multiTx0x2Context.getTx0x2ContextList().size();
    int size = multiTx0x2.getTx0x2List().size();

    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
    Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(0);
    Tx0x2 payload = tx0x2Service.doStep3(tx0x2, tx0x2Context);
    tx0x2List.add(payload);

//    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
//    for (int i = 0; i < size; i++) {
//      Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(i);
//      Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(i);
//      Tx0x2 payload = tx0x2Service.doStep3(tx0x2, tx0x2Context);
//      tx0x2List.add(payload);
//    }

    MultiTx0x2 multiCahoots = new MultiTx0x2(multiTx0x2);
    multiCahoots.setTx0x2List(tx0x2List);
    multiCahoots.setStep(3);

    debug("END MULTI TX0X2 STEP 3", multiTx0x2Context);
    return multiCahoots;
  }

  //
  // sender
  //
  private MultiTx0x2 doStep4(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2) throws Exception {
    debug("BEGING MULTI TX0X2 STEP 4", multiTx0x2Context);

    // TODO - Work in Progress

    List<Tx0x2> tx0x2List = new ArrayList<>();
//    int size = multiTx0x2Context.getTx0x2ContextList().size();
    int size = multiTx0x2.getTx0x2List().size();

    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
    Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(0);
    Tx0x2 payload = tx0x2Service.doStep4(tx0x2, tx0x2Context);
    tx0x2List.add(payload);


//    Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(0);
//    for (int i = 0; i < size; i++) {
//      Tx0x2Context tx0x2Context = multiTx0x2Context.getTx0x2ContextList().get(i);
//      Tx0x2 tx0x2 = multiTx0x2.getTx0x2List().get(i);
//      Tx0x2 payload = tx0x2Service.doStep4(tx0x2, tx0x2Context);
//      tx0x2List.add(payload);
//    }

    MultiTx0x2 multiCahoots = new MultiTx0x2(multiTx0x2);
    multiCahoots.setTx0x2List(tx0x2List);
    multiCahoots.setStep(4);

    debug("END MULTI TX0X2 STEP 4", multiTx0x2Context);
    return multiCahoots;
  }

  @Override
  public void verifyResponse(MultiTx0x2Context multiTx0x2Context, MultiTx0x2 multiTx0x2, MultiTx0x2 request) throws Exception {
    super.verifyResponse(multiTx0x2Context, multiTx0x2, request);
    // TODO maybe?
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
