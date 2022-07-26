# Java integration

## I. Getting started
Get the latest version of `whirlpool-client` from JitPack repository (Maven, Gradle).  
 [![](https://jitpack.io/v/io.samourai.code.whirlpool/whirlpool-client.svg)](https://jitpack.io/#io.samourai.code.whirlpool/whirlpool-client)  
This will automatically fetch:
 - [BitcoinJ](https://code.samourai.io/wallet/bitcoinj) - basic Bitcoin stack
 - [ExtLibJ](https://code.samourai.io/wallet/ExtLibJ) - Samourai stack (including Cahoots, Taproot...)

See [JavaExample.java](src/test/java/JavaExample.java) for an overview of Whirlpool integration.


## II. DataSource configuration
Configuring a [DataSource](/-/blob/develop/src/main/java/com/samourai/whirlpool/client/wallet/data/dataSource/DataSource.java) provides all the data required by the library, through multiple providers:
- `WalletSupplier` provides current wallet state (deposit, premix, postmix wallets)
- `UtxoSupplier` provides UTXOs state
- `MinerFeeSupplier` provides miner fee state
- `ChainSupplier` provides current chain state (block-height)
- `PoolSupplier` provides mixing pools state
- `PaynymSupplier` provides Paynym state
- `Tx0PreviewService` provides TX0 preview information
- `pushTx()` provides TX broadcast service

These providers are instanciated by the DataSource.
As described in JavaExample, the DataSource can be:
- a `DojoDataSourceFactory`: for using Samourai or Dojo backend
- a simplified DataSource using `WalletResponseDataSource`: just implement `fetchWalletResponse()` to return all required datas as a `WalletResponse` object. The library will instanciate the providers for you.
- a custom `DataSource`: by implementing each provider by yourself



## Resources
 * [whirlpool](https://code.samourai.io/whirlpool/Whirlpool)
 * [whirlpool-protocol](https://code.samourai.io/whirlpool/whirlpool-protocol)
 * [whirlpool-server](https://code.samourai.io/whirlpool/whirlpool-server)
 * [whirlpool-client-cli](https://code.samourai.io/whirlpool/whirlpool-client-cli)
