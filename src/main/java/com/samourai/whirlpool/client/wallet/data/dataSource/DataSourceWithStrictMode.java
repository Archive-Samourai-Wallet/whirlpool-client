package com.samourai.whirlpool.client.wallet.data.dataSource;

import java.util.Collection;

public interface DataSourceWithStrictMode {

  String pushTx(String txHex, Collection<Integer> strictModeVouts) throws Exception;
}
