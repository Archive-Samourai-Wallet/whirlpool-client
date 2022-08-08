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
- See [whirlpool-client-cli DataSource](https://code.samourai.io/whirlpool/whirlpool-client-cli/-/blob/develop/src/main/java/com/samourai/whirlpool/cli/config/CliConfig.java#L60)
- See [Android DataSource](https://code.samourai.io/wallet/samourai-wallet-android/-/blob/develop/app/src/main/java/com/samourai/whirlpool/client/wallet/AndroidWhirlpoolWalletService.java#L110)
- See [JavaExample.java](src/test/java/JavaExample.java)

### 2) Custom backend with `WalletResponseDataSource`
This is another easy integration by instanciating a `WalletResponseDataSource`.  
Just implement `fetchWalletResponse()` and return all required datas as a `WalletResponse` object. The library will instanciate the providers for you.
Examples:
- See [Sparrow DataSource](https://github.com/sparrowwallet/sparrow/blob/416fc83b4db864bce9b0e487cb3d25f0f57b2f07/src/main/java/com/sparrowwallet/sparrow/whirlpool/dataSource/SparrowDataSource.java)
- See [JavaExample.java](src/test/java/JavaExample.java)

### 3) Custom backend with custom providers
This is a more complicated integration with your own `DataSource`.  
You get the full control on the library by implementing each provider by yourself.
Examples:
- See [JavaExample.java](src/test/java/JavaExample.java)

## III. DataPersister configuration
The library needs to store state data.  
By default this is stored on the filesystem using [FileDataPersisterFactory.java](https://code.samourai.io/whirlpool/whirlpool-client/-/blob/develop/src/main/java/com/samourai/whirlpool/client/wallet/data/dataPersister/FileDataPersisterFactory.java), but you can configure your `DataPersister`.  
Example:
- See [Sparrow DataPersister](https://github.com/sparrowwallet/sparrow/blob/416fc83b4db864bce9b0e487cb3d25f0f57b2f07/src/main/java/com/sparrowwallet/sparrow/whirlpool/dataPersister/SparrowDataPersister.java)
- See [JavaExample.java](src/test/java/JavaExample.java)

## IV. Instanciating WhirlpoolWallet
- instanciate `WhirlpoolWalletConfig`
- instanciate a new `WhirlpoolWallet` with this config
- open it with `WhirlpoolWalletService.openWallet()`

## V. Using WhirlpoolWallet
- start mixing service with `whirlpoolWallet.startAsync()` / `whirlpoolWallet.stop()`
- get state from WhirlpoolWallet:
    - mixing state: `whirlpoolWallet.getMixingState()`
    - pools, utxos, chain state... : `whirlpoolWallet.getXXXSupplier()`
- TX0 with `whirlpoolWallet.tx0Previews()` and `whirlpoolWallet.tx0()`
- Subscribe events with `WhirlpoolEventService.getInstance().register(this)` and `@Subscribe`
- See [JavaExample.java](src/test/java/JavaExample.java) for more details


## Mix to external/xpub
Example:
- See Sparrow
- See [JavaExample.java](src/test/java/JavaExample.java)


## Resources
 * [whirlpool](https://code.samourai.io/whirlpool/Whirlpool)
 * [whirlpool-protocol](https://code.samourai.io/whirlpool/whirlpool-protocol)
 * [whirlpool-server](https://code.samourai.io/whirlpool/whirlpool-server)
 * [whirlpool-client-cli](https://code.samourai.io/whirlpool/whirlpool-client-cli)
