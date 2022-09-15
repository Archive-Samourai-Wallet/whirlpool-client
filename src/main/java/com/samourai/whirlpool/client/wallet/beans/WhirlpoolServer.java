package com.samourai.whirlpool.client.wallet.beans;

import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

public enum WhirlpoolServer {
  TESTNET(
      "https://pool.whirl.mx:8081",
      "http://y5qvjlxvbohc73slq4j4qldoegyukvpp74mbsrjosnrsgg7w5fon6nyd.onion",
      TestNet3Params.get(),
      "mi42XN9J3eLdZae4tjQnJnVkCcNDRuAtz4"),
  INTEGRATION(
      "https://pool.whirl.mx:8082",
      "http://yuvewbfkftftcbzn54lfx3i5s4jxr4sfgpsbkvcflgzcvumyxrkopmyd.onion",
      TestNet3Params.get(),
      "mi42XN9J3eLdZae4tjQnJnVkCcNDRuAtz4"),
  MAINNET(
      "https://pool.whirl.mx:8080",
      "http://udkmfc5j6zvv3ysavbrwzhwji4hpyfe3apqa6yst7c7l32mygf65g4ad.onion",
      MainNetParams.get(),
      "1LYKqgLMJagcQ69Y9iLj4TNs4uH9surJ3w"),
  LOCAL_TESTNET(
      "http://127.0.0.1:8080",
      "http://127.0.0.1:8080",
      TestNet3Params.get(),
      "mi42XN9J3eLdZae4tjQnJnVkCcNDRuAtz4"),
  ;

  private String serverUrlClear;
  private String serverUrlOnion;
  private NetworkParameters params;
  private String signingAddress;

  WhirlpoolServer(
      String serverUrlClear,
      String serverUrlOnion,
      NetworkParameters params,
      String signingAddress) {
    this.serverUrlClear = serverUrlClear;
    this.serverUrlOnion = serverUrlOnion;
    this.params = params;
    this.signingAddress = signingAddress;
  }

  public String getServerUrlClear() {
    return serverUrlClear;
  }

  public String getServerUrlOnion() {
    return serverUrlOnion;
  }

  public String getServerUrl(boolean onion) {
    String serverUrl = onion ? getServerUrlOnion() : getServerUrlClear();
    return serverUrl;
  }

  public NetworkParameters getParams() {
    return params;
  }

  public String getSigningAddress() {
    return signingAddress;
  }

  public static Optional<WhirlpoolServer> find(String value) {
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
