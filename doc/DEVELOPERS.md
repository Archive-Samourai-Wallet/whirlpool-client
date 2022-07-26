# Java integration

## I. Getting started
Get the latest version of `whirlpool-client` from JitPack repository (Maven, Gradle).  
 [![](https://jitpack.io/v/io.samourai.code.whirlpool/whirlpool-client.svg)](https://jitpack.io/#io.samourai.code.whirlpool/whirlpool-client)  
This will automatically fetch:
 - [BitcoinJ](https://code.samourai.io/wallet/bitcoinj) - basic Bitcoin stack
 - [ExtLibJ](https://code.samourai.io/wallet/ExtLibJ) - Samourai stack (including Cahoots, Taproot...)

See [JavaExample.java](src/test/java/JavaExample.java) for an overview of Whirlpool integration.


## II. DataSource configuration
Configuring a [DataSource](/-/blob/develop/src/main/java/com/samourai/whirlpool/client/wallet/data/dataSource/DataSource.java) provides all the data required by the library.  
Such data is accessed through multiple providers:
- `WalletSupplier` provides current wallet state (deposit, premix, postmix wallets)
- `UtxoSupplier` provides UTXOs state
- `MinerFeeSupplier` provides miner fee state
- `ChainSupplier` provides current chain state (block-height)
- `PoolSupplier` provides mixing pools state
- `PaynymSupplier` provides Paynym state
- `Tx0PreviewService` provides TX0 preview information
- `pushTx()` provides TX broadcast service

These providers are instanciated by the DataSource. You can configure it to use your own data rather than using the Samourai backend.

### 1) Samourai or Dojo backend
This is the easiest integration by instanciating a `DojoDataSourceFactory`.  
Examples:
- See [whirlpool-client-cli integration](https://code.samourai.io/whirlpool/whirlpool-client-cli/-/blob/develop/src/main/java/com/samourai/whirlpool/cli/config/CliConfig.java#L60)
- See [Android integration](https://code.samourai.io/wallet/samourai-wallet-android/-/blob/develop/app/src/main/java/com/samourai/whirlpool/client/wallet/AndroidWhirlpoolWalletService.java#L110)

### 2) Custom backend with `WalletResponseDataSource`
This is another easy integration by instanciating a `WalletResponseDataSource`.  
Just implement `fetchWalletResponse()` and return all required datas as a `WalletResponse` object. The library will instanciate the providers for you.
Examples:
- See [Sparrow integration](https://github.com/sparrowwallet/sparrow/blob/416fc83b4db864bce9b0e487cb3d25f0f57b2f07/src/main/java/com/sparrowwallet/sparrow/whirlpool/dataSource/SparrowDataSource.java)

### 3) Custom backend with custom providers
This is a more complicated integration with your own `DataSource`.  
You get the full control on the library by implementing each provider by yourself.



## Resources
 * [whirlpool](https://code.samourai.io/whirlpool/Whirlpool)
 * [whirlpool-protocol](https://code.samourai.io/whirlpool/whirlpool-protocol)
 * [whirlpool-server](https://code.samourai.io/whirlpool/whirlpool-server)
 * [whirlpool-client-cli](https://code.samourai.io/whirlpool/whirlpool-client-cli)
