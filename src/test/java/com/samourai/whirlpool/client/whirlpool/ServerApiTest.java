package com.samourai.whirlpool.client.whirlpool;

import com.samourai.whirlpool.client.exception.PushTxErrorResponseException;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.Tx0DataRequestV2;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;
import com.samourai.whirlpool.protocol.rest.Tx0PushRequest;
import java.util.Arrays;
import org.bitcoinj.core.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerApiTest extends AbstractTest {
  private ServerApi serverApi;

  public ServerApiTest() throws Exception {
    super();
    String urlServer = WhirlpoolServer.TESTNET.getServerUrlClear();
    serverApi = new ServerApi(urlServer, httpClient, httpClient);
  }

  @Test
  public void pushTx_fail_invalid() throws Exception {
    String tx64 =
        WhirlpoolProtocol.encodeBytes(
            Utils.HEX.decode(
                "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff040000000000000000426a409ae6649a7b1fc9ab17f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f6859171027000000000000160014f6a884f18f4d7e78a4167c3e56773c3ae58e0164ee2b000000000000160014d49377882fdc939d951aa51a3c0ad6dd4a152e26d6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4d0247304402204e37d89e31eb2242049605dabc803579c717f41eea9e53e7a460e8ac7a3806800220460816a471b9dd9cae5b937368da68166d7b2d28a946a01bc1d6317018e3063801210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000"));
    Tx0PushRequest request = new Tx0PushRequest(tx64, "0.01btc");
    try {
      asyncUtil.blockingGet(serverApi.pushTx0(request)); // should fail
      Assertions.assertTrue(false);
    } catch (PushTxErrorResponseException e) {
      Assertions.assertEquals("Not a TX0", e.getMessage());
      Assertions.assertEquals("Not a TX0", e.getPushTxErrorResponse().message);
      Assertions.assertEquals("Not a TX0", e.getPushTxErrorResponse().pushTxErrorCode);
      Assertions.assertNull(e.getPushTxErrorResponse().voutsAddressReuse);
    }
  }

  @Test
  public void pushTx_fail_addressReuse() throws Exception {
    String tx64 =
        WhirlpoolProtocol.encodeBytes(
            Utils.HEX.decode(
                "01000000000101a385df25cf57eccb4857bf765ece5e310121e6ecd4f9fcafdcbc51d444e1e7f30000000000ffffffff1c0000000000000000426a40a6f5a9d16a01e954ff4d4d45997ad84034d67b13cb8b3cf1a11c1cc8219fa6fb1293d045fa7927af8413a59c84d8a18aad70a8971de091214703133955e72bd98813000000000000160014dc8afa52ec75659f57dffe795c3e0cc3f6fb3d22ce870100000000001600141a36dbc7cd5b35b4d2557299a7fa15c62fb96e8ace870100000000001600141e9c461092d5ad25eb3396d3e096092281768047ce8701000000000016001426cb0b86aecfbf5ca2c0eab4b7503130a0a86992ce870100000000001600142f38f6f78ba8ba595bc4744ffd5f0dba55518315ce87010000000000160014302a6b72b9f0b5df47be20eff5bd975949685924ce87010000000000160014330926563877580f2a2687d250f9272e54ed94a4ce87010000000000160014346ad4466a9390a93264af6fa506cb8eff6be1b3ce870100000000001600143c14ece4c110cbdd1da6850a1ce8578c832ae8c6ce87010000000000160014402cecc22da45655d3346c682e3ca39341c3c217ce87010000000000160014603015ccc7973fe0136d01d99250f2f943c8cce6ce87010000000000160014650b45fc8c7c225483f24ba3a219517ab6077120ce8701000000000016001472810c5baf7ede7c545b33c35f0f0101118b169ece8701000000000016001475c388e6d1ba3353ebe1c2437c9244c0351c035cce8701000000000016001478d81ac38aedff093545dca65c7827689be6e7f5ce87010000000000160014987f5959dc4546e0c8ed764e61b5dc6f4cb7c542ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014a0881cf3c129cf0701fed25bc43e688fed905320ce87010000000000160014b2979b61d705a241318d9d80dad5c60978a58ea9ce87010000000000160014c91c437c70783eb0f3229869ca2b4722614f8887ce87010000000000160014e672a3351028042bac08eb5263cf74d4d9819600ce87010000000000160014ea5c3d55f52d1bb704c6447088a596ca1288a86d6ff962000000000016001418b95e858c80b25886b2bcce12f6303011ddbec1024730440220658c1dfd67b1dfcf37762f5c0e4ccc25b66099463dcb0f681779547554f2344402206b37c3d2e3aa0da7452ed933babfc1acef43a5f381900bec729363974bb2a15a0121028dfb4e6d2f2e97a24507332a67fd77a0d3eac08c4354ceb655b43e2c11ea111b00000000"));
    Tx0PushRequest request = new Tx0PushRequest(tx64, "0.001btc");
    try {
      asyncUtil.blockingGet(serverApi.pushTx0(request)); // should fail
      Assertions.assertTrue(false);
    } catch (PushTxErrorResponseException e) {
      Assertions.assertEquals("address-reuse", e.getMessage());
      Assertions.assertEquals("address-reuse", e.getPushTxErrorResponse().message);
      Assertions.assertEquals("address-reuse", e.getPushTxErrorResponse().pushTxErrorCode);
      System.err.println(Arrays.toString(e.getPushTxErrorResponse().voutsAddressReuse.toArray()));
      Assertions.assertArrayEquals(
          new Integer[] {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
            26, 27
          },
          e.getPushTxErrorResponse().voutsAddressReuse.toArray());
    }
  }

  @Test
  public void fetchTx0DataV0() throws Exception {
    Tx0DataRequestV2 request = new Tx0DataRequestV2();
    request.partnerId = "FREESIDE";

    Tx0DataResponseV2 response = asyncUtil.blockingGet(serverApi.fetchTx0Data(request, true)).get();
    Assertions.assertEquals(4, response.tx0Datas.length);
    for (Tx0DataResponseV2.Tx0Data tx0Data : response.tx0Datas) {
      Assertions.assertEquals(64, WhirlpoolProtocol.decodeBytes(tx0Data.feePayload64).length);
    }
    Assertions.assertNull(response.tx0Datas[0].message);
  }

  @Test
  public void fetchTx0DataV1() throws Exception {
    Tx0DataRequestV2 request = new Tx0DataRequestV2();
    request.partnerId = "FREESIDE";

    Tx0DataResponseV2 response =
        asyncUtil.blockingGet(serverApi.fetchTx0Data(request, false)).get();
    Assertions.assertEquals(4, response.tx0Datas.length);
    for (Tx0DataResponseV2.Tx0Data tx0Data : response.tx0Datas) {
      Assertions.assertEquals(46, WhirlpoolProtocol.decodeBytes(tx0Data.feePayload64).length);
    }
    Assertions.assertNull(response.tx0Datas[0].message);
  }
}
