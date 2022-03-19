package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.ISweepBackend;

public interface DataSourceWithSweep {

  ISweepBackend getSweepBackend();
}
