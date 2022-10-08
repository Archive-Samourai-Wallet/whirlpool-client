package com.samourai.http.client;

public interface IWhirlpoolHttpClientService {
  IHttpClient getHttpClient(HttpUsage httpUsage);

  void stop();
}
