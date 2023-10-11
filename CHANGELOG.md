# CHANGELOG
Changelog for whirlpool-client.

## Unreleased
### 

- refactor: encode OP_RETURN by counterparty + check maxSpendAmount *(2023-10-13)*
- refactor: upgrade ExtLibJ *(2023-10-04)*
- fix: Tx0x2 index gap for sender premixs *(2023-10-02)*
- refactor: single entry point for regular+cascade TX0 with WhirlpoolWallet.tx0() *(2023-09-30)*

## 0.25.0-beta3
### 

- refactor: fix tx0 decoy when spending utxos with same hash or with tx0MaxOutputs limit *(2023-09-26)*
- refactor: count tx0 decoy change for fee estimation + add Tx0PreviewConfig.decoyTx0x2Forced *(2023-09-26)*
- refactor: add utxo abstraction with UtxoRef, UtxoDetail, UtxoSpendable *(2023-09-24)*
- add Tx0Preview.decoyTx0x2 *(2023-09-16)*

## 0.25.0-beta2
### 

- build: add CHANGELOG generation *(2023-09-15)*
- refactor & condense doStepX() & doMultiStepX() *(2023-09-12)*
- apply CHANGE_SPLIT_THRESHOLD in the same way for 2-users tx0x2 & decoy tx0x2 *(2023-09-12)*
- Decoy tx0x2 change split threshold *(2023-09-12)*
- Set threshold denomination for Decoy Tx0x2 change split *(2023-09-11)*
- upgrade extlibj (branch feature/tx0x2) *(2023-09-05)*
- refactor computeChangeAmountsStonewall(): simplify *(2023-08-23)*
- refactor computeChangeAmountsStonewall(): reuse existing Tx0Preview calculations + add (pool.tx0MaxOutputs/2) cap for tx0x2Decoy *(2023-08-22)*
- enable tx0x2Decoy preview: move calculations from Tx0Service to Tx0PreviewService *(2023-08-22)*
- simplify tx0Cascade() changes detection *(2023-08-22)*
- optimize tx0(poolId) by using tx0PreviewService.tx0Preview(poolId) instead of computing all pools with .tx0Previews() *(2023-08-22)*
- Tx0x2 bug fix *(2023-07-26)*
- Additional Multi-Party (Tx0x2) test *(2023-07-26)*
- Decoy Tx0x2 stonewall improvement, check outpoints *(2023-07-06)*
- Tx0x2: deducing split miner fee counterparty change output & input bug fix *(2023-06-03)*
- tx0x2: updating counterparty change output & lower pool input bug *(2023-06-02)*
- tx0x2 - changes for updating counterparty change output, subtract split miner fee *(2023-05-02)*
- decoy tx0x2 calculations *(2023-04-18)*
- fix test config whirlpool wallet tx0 max outputs *(2023-04-18)*
- Tx0x2 cascading working *(2023-03-31)*
- Tx0x2 Cascade - Work in Progress... *(2023-03-22)*
- stonewall draft for TX0 decoy *(2023-03-17)*
- stonewall draft for TX0 decoy *(2023-03-17)*
- move TX0 change calculation to Tx0Service.computeChangeAmounts() *(2023-03-17)*
- fix NullPointerException *(2023-03-16)*
- MultiTx0x2 test improvement *(2023-03-10)*
- MultiTx0x2 JSONObject handling *(2023-03-09)*
- MultiTx0x2 foundation *(2023-03-06)*
- decoy tx0x2 cascading *(2023-02-17)*
- Allow choosing each cascading pools for TX0: *(2023-02-13)*
 * - move TX0 cascading from WhirpoolWallet.tx0() to WhirlpoolWallet.tx0Cascade()
 * - remove Tx0PreviewConfig.cascading
- move tx0 cascading implementation to Tx0Service *(2023-02-12)*
- prepare TX0X2 cascading: generate & sign full TX0 cascading list without pushing it *(2023-02-12)*
- remove unused config: tx0MinConfirmations *(2023-02-11)*
- restore check for counterpartyChangeOutput != null *(2023-01-30)*
- Tx0x2: Steps 3 & 4 verify *(2022-12-15)*
- tx0x2 fillings *(2022-12-14)*
- prepare for tx0x2 *(2022-12-04)*
- add AbstractCahootsTest *(2022-12-04)*

## 0.25.0-beta1
### 

- upgrade extlibj *(2023-06-09)*
- upgrade extlibj: BipWallet refactoring *(2023-06-05)*
- upgrade extlibj: BipWallet refactoring *(2023-06-05)*
- upgrade extlibj *(2023-05-23)*
- fix nullpointer on MixFailEvent.mixProgress (CLI) *(2023-05-12)*
- add WhirlpoolWalletConfig.extraLiquidityClientsPerPool *(2023-05-12)*

## 0.24.3
### 

- use ExtLibJ 0.0.41 + use renamed SegwitAddress.segwitRedeemScript *(2023-01-17)*
- fix invalid changeValueTotal *(2023-01-17)*
- add AbstractCahootsTest *(2022-12-02)*

## 0.24.2
### 

- update extlibj and fix constructor *(2022-11-11)*
- up dep *(2022-11-10)*
- Chain supplier refactor *(2022-11-10)*

## 0.24.1
### 

- extlibj 0.0.35 *(2022-11-02)*
- make tx0Cascade internal and use tx0() with Tx0Config.cascading=true *(2022-10-27)*
- move WhirlpoolServer to ExtLibJ *(2022-10-27)*

## 0.24.0
### 

- integrate soroban-client as whirlpool-client dependency *(2022-10-21)*
- adapt for java-websocket-client: remove STOMP classes, rename IHttpClientService -> IWhirlpoolHttpClientService *(2022-10-08)*
- upgrade ExtLibJ: use Single result instead of Observable *(2022-10-04)*
- add WhirlpoolServer.signingAddress *(2022-09-15)*
- Tx0 Cascading clean up *(2022-09-12)*
- [tx0Cascade review] add Tx0PreviewConfig.cascading + set Tx0DataRequestV2.cascading *(2022-09-12)*
- [tx0Cascade review] use try/catch to avoid duplicate tx0Previews() *(2022-09-12)*
- [tx0Cascade review] await for change utxo *(2022-09-12)*
- Tx0 Cascade changes *(2022-09-09)*
- Tx0 Cascade improvements *(2022-09-08)*
- Tx0 cascade *(2022-09-07)*
- add BasicSupplier.mockValue() *(2022-09-07)*
- Cascading work *(2022-09-06)*
- add specs for tx0-cascade *(2022-08-25)*
- add Tx0.spendFroms *(2022-08-25)*
- add tx0Cascade specifications *(2022-08-25)*
- add Tx0.spendFroms *(2022-08-25)*
- Java integration documentation *(2022-08-08)*
- Java integration documentation *(2022-07-26)*
- Java integration documentation *(2022-07-26)*
- add DEELOPERS.md *(2022-07-26)*
- (ExtLibJ) adapt Cahoots for CLI *(2022-07-16)*

## 0.23.46
### 

- ExtLibJ 0.0.33.3 *(2022-07-23)*
- fix reconnect after delay MixSession.waitAndReconnectAsync() *(2022-07-22)*

## 0.23.45
### 

- extlibj 0.0.33.2 *(2022-07-16)*
- fix secretPointFactory for Android *(2022-07-16)*

## 0.23.44
### 

- adapt for ExtLibJ *(2022-07-09)*

## 0.23.43
### 

- use ExtLibJ 0.0.32 *(2022-07-06)*
- move XManagerClient to ExtLibJ *(2022-07-05)*

## 0.23.42
### 

- confirm postmix index on REGISTER_OUTPUT success *(2022-06-27)*
- whirlpool-protocol 0.23.9 + extlibj 0.0.31-1 *(2022-06-27)*
- add WhirlpoolWalletConfig.setFeeOpReturnImplV0() *(2022-06-27)*
- use xmanager-protocol 1.0.6 *(2022-06-17)*
- add utxoConfig.blocked + utxoConfig.note *(2022-06-14)*
- add IPersister *(2022-06-13)*
- DojoDataSource.resyncMixsDone(): fix "Buffering capacity exceeded" *(2022-06-13)*
- push WalletOpenEvent after updating WhirlpoolWalletService session *(2022-06-09)*
- protocol update: use RegisterInputRequest.blockHeight *(2022-06-01)*
- protocol update: allow retrying REGISTER_OUTPUT with same bordereau but different address *(2022-06-01)*
- ignore http errors on checkPostmixIndex() *(2022-05-17)*
- ClientUtils.fromJson() ignore unknown properties to allow protocol upgrades *(2022-05-13)*
- fix Dojo apiKey decryption *(2022-05-13)*
- use FeePayloadV1 *(2022-05-13)*
- Updated README Resources links to GitLab urls instead of older GitHub urls *(2022-05-04)*
- upgrade extlibj *(2022-04-22)*
- upgrade xmanager-protocol *(2022-04-20)*
- fix queuing all utxos on wallet startup *(2022-04-04)*
 * If WalletResponseSupplier is refreshed after calling WhirlpoolWallet.open(), but before calling WhirlpoolWallet.start(), then the UtxoData object is replaced, but the new UtxoData indicates no changes
- use extlibj 0.0.32 *(2022-04-04)*

## 0.23.41
### 

- extlibj 0.0.31 *(2022-03-21)*
- add DataSourceWithSweep *(2022-03-19)*

## 0.23.40
### 

- introduce methods for retrieving private keys to avoid requiring exporting bitcoinj packages *(2022-03-04)*
- use ServerApi.pushTx0() *(2022-03-02)*
- fix bip84 wallet initialization order *(2022-02-24)*

## 0.23.39
### 

- use jdk8 *(2022-02-16)*
- use BasicSupplier.validate() & BasicSupplier.onValueChange() *(2022-02-13)*
- add PaynymSupplier *(2022-02-13)*
- fix #25 confirmations not refreshing in whirlpool-gui *(2022-02-07)*
- use BipFormat *(2022-02-04)*

## 0.23.38
### 

- extlibj 0.0.28 *(2022-01-16)*
- upgrade logback *(2022-01-16)*
- fix ConcurrentModificationException on UtxoConfigPersister *(2022-01-15)*
- rename XManagerClient.verifyAddressIndexResponse() -> verifyAddressIndexResponseAsync(), verifyAddressIndexResponseOrException() -> verifyAddressIndexResponse() *(2022-01-14)*
- add Tx0PreviewService + pool.getTx0PreviewMin() + pool.getTx0PreviewMinSpendValue() *(2022-01-03)*
- fix resyncMixsDone() *(2021-12-10)*
- update dependencies *(2021-12-10)*

## 0.23.37.2
### 

- adapt for Android/bip47 *(2022-01-04)*
- check postmixIndex on IO thread: whirlpoolWallet.start() -> startAsync() + whirlpoolWallet.refreshUtxos() -> refreshUtxosAsync() + whirlpoolWallet.checkPostmixIndex() -> checkPostmixIndexAsync() + *(2022-01-04)*
- add config.postmixIndexCheck *(2022-01-04)*

## 0.23.37.1
### 

- add utxoSupplier.isMixableUtxo() to avoid remixing POSTMIX change *(2022-01-03)*

## 0.23.37
### 

- extlibj 0.0.26 *(2021-10-25)*
- fix resyncMixsDone() for external datasources *(2021-10-25)*
- keep utxo's last error message until new mix *(2021-10-25)*
- fix failed mix utxo not re-queued *(2021-10-25)*
- add whirlpoolWallet.getDebug() *(2021-10-25)*
- less logs "onMixFail(CANCEL/STOP): won't retry" *(2021-10-22)*
- fix WalletResponseDataSource erasing local indexes with highest used wallet index *(2021-10-22)*

## 0.23.36
### 

- detect unsupported Java versions *(2021-10-21)*
- fix mix stopping on first "output already registered" error *(2021-10-21)*

## 0.23.35
### 

- enable tx0 strict mode + add config.tx0MaxRetry + config.tx0StrictMode *(2021-10-13)*
- remove fakeOutput *(2021-10-11)*

## 0.23.34
### 

- add config.postmixIndexAutoFix + events *(2021-09-23)*

## 0.23.33
### 

- fix checkPostmixIndex() *(2021-09-18)*

## 0.23.31
### 

- JavaExample: add external destination *(2021-09-01)*

## 0.23.30-early5
### 

- check postmix index on mix fail *(2021-09-10)*
- add whirlpoolWallet.tx0Previews() *(2021-09-10)*
- rename config.partnerId => config.partner *(2021-09-10)*
- add config.indexRangePostmix *(2021-09-09)*
- add config.partnerId *(2021-09-09)*
- newfees *(2020-04-25)*

## 0.23.30-early4
### 

- add ExternalDestination.postmixHandler *(2021-09-03)*
- adapt for Sparrow *(2021-08-21)*

## 0.23.30-early3
### 

- add WalletResponseDataSource.refresh() *(2021-08-20)*
- remove utxoConfig.poolId *(2021-08-20)*
- add DataSupplier + DataPersister *(2021-08-19)*
- add constructor MinerFeeSupplier(int feeMin, int feeMax, MinerFee initialValue) *(2021-08-18)*
- add walletDataSupplier.computeFile *(2021-08-16)*
- rename walletDataSupplier.computeXXXFile => computeFileXXX *(2021-08-16)*
- add openWallet(WhirlpoolWalletConfig, HD_Wallet) *(2021-08-16)*
- add pushTx strictMode *(2021-08-13)*
- update example *(2021-08-10)*
- add example for getSpendFromBalanceMin() *(2021-08-08)*

## 0.23.30-early1
### 

- use extlibj 0.0.19-dsk1 *(2021-08-08)*
- add @Subscribe examples *(2021-08-08)*
- add @Subscribe examples *(2021-08-08)*
- WalletDataSupplier: add walletIdentifier, remove filenames *(2021-08-07)*
- add BackendWalletDataSupplier *(2021-08-06)*
- remove persistence backup() *(2021-08-06)*
- add Pool.premixValueMin & Pool.spendFromBalanceMin + move poolSupplier to WalletDataSupplier *(2021-08-06)*
- update default configuration (autoMix=true, maxClients=1 for mobile/5 for desktop, liquidityClient=false for mobile/true for desktop) *(2021-08-05)*
- add MixSuccessEvent + MixFailEvent + Tx0Event *(2021-07-28)*
- add whirlpoolWallet.walletIdentifier *(2021-07-27)*
- fix autoTx0 *(2021-07-13)*
- add UtxoSupplier.findUtxosByAddress() *(2021-07-05)*
- add BackendWsApi *(2021-06-25)*
- sign all AddressTypes *(2021-06-18)*
- fetch utxos for all AddressTypes *(2021-06-11)*
- add MinerFeeChangeEvent *(2021-06-07)*
- add SpendBuilder *(2021-06-05)*
- adapt for Android *(2021-05-31)*
- add ChainBlockChangeEvent *(2021-05-17)*
- add ChainSupplier *(2021-05-16)*
- add UtxoRequestEvent+UtxoResponseEvent *(2021-05-11)*
- add UtxoData.txs *(2021-05-10)*
- add PoolsChangeEvent + MixStateChangeEvent *(2021-05-07)*
- add WalletopenEvent + WalletCloseEvent *(2021-04-30)*
- use junit 4.13.2 for jdk 1.6 compatibility *(2021-04-27)*
- add WhirlpoolEventService *(2021-04-27)*
- add XManagerClientExample *(2021-04-18)*

## 0.23.29
### 

- fix "Invalid fake change detected" *(2021-03-23)*

## 0.23.28
### 

- ignore duplicate SubscribePoolResponse *(2021-03-15)*
- improve CLI connectivity after long downtimes *(2021-03-15)*

## 0.23.27
### 

- adapt for android *(2021-03-09)*

## 0.23.26
### 

- try second next index on postmixIndex already used *(2021-02-09)*
- add pool.tx0MaxOutputs *(2021-01-28)*
- add Tx0NotifyRequest.poolId *(2021-01-28)*
- disable tx0FakeOutput on mainnet *(2021-01-14)*

## 0.23.25
### 

- forward http errors to GUI *(2020-12-14)*
- add tx0Notify *(2020-12-11)*
- check outputIndex on startup *(2020-12-11)*
- fix index reuse on /wallet update *(2020-12-10)*
- add tx0 fake outputs *(2020-12-07)*
- add Tx0Preview.mixMinerFee + premixMinerFee *(2020-12-04)*
- Minor typo fixed in MixFailReason.java *(2020-10-07)*

## 0.23.24
### 

- fix too many remixing threads per pool *(2020-11-25)*
- add externalDestination support *(2020-11-25)*
- remove mixsTarget selection to increase mix privacy *(2020-11-12)*
- limit tx0 to 70 premixs *(2020-11-12)*
- adapt for android *(2020-11-12)*
- MinerFeeSupplier: validate fee values *(2020-11-06)*

## 0.23.23
### 

- allow additional thread for concurrent liquidity remixing *(2020-10-17)*
- allow additional thread for concurrent liquidity remixing *(2020-10-17)*
- manage network errors *(2020-10-13)*
- use backendApi.fetchWallet() *(2020-10-07)*
- use backendApi.fetchWallet() *(2020-10-06)*
- move IHttpClient + AbstractOrchestrator + rxjava to extlibj *(2020-08-26)*
- add config.resyncOnFirstRun *(2020-07-10)*
- adjust FETCH_TXS_PER_PAGE *(2020-07-10)*
- fix mixsTarget=0 considered as mixsTarget=1 *(2020-07-03)*
- add whirlpoolWallet.refreshUtxosDelay() *(2020-07-02)*

## 0.23.22
### 

- add whirlpoolWallet.resync() *(2020-07-08)*
- fix utxoConfig lost while forwarding *(2020-07-06)*

## 0.23.21
### 

- fix mixsTarget=0 considered as mixsTarget=1 *(2020-07-03)*
- fix invalid value logged for nbCleaned *(2020-06-30)*

## 0.23.20
### 

- prepare next release *(2020-06-26)*
- fix premix priority on Android *(2020-06-26)*
- utxos priority: ignore old errors + decrease priority when mixing for too long *(2020-06-25)*
- faster exit *(2020-06-25)*

## 0.23.19
### 

- fix too many mixing *(2020-06-09)*
- fix mix concurrency *(2020-06-09)*
- fix mixs counter "4/3" *(2020-06-09)*
- backup state on startup *(2020-06-09)*
- use data suppliers *(2020-06-09)*
- change groupId *(2020-05-14)*
- move to code.samourai.io *(2020-04-25)*

## 0.23.18
### 

- add HttpUsage *(2020-04-14)*
- fix (whirlpool-gui) Initialization failed: Unable to save CLI configuration - Windows 10 *(2020-04-13)*

## 0.23.17
### 

- fix concurrency + optimize onUtxoChanges *(2020-04-04)*

## 0.23.16
### 

- require maxClients (default=5) *(2020-04-03)*
- fix mixingPerPool indicator *(2020-04-03)*
- fix concurrency / GUI stuck on "fetching wallet state..." *(2020-04-03)*

## 0.23.15
### 

- fix safeWrite on raspberry *(2020-03-30)*
- fix safeWrite on raspberry *(2020-03-26)*

## 0.23.14
### 

- Transport: avoid waiting on android's mainThread when reconnecting *(2020-03-06)*
- MixOrchestrator: stop mixing utxo when spent + swap mixing when higher priority mixable utxo detected *(2020-03-05)*
- add pool parameter to WhirlpoolWallet.getTx0Config() *(2020-02-29)*

## 0.23.11
### 

- fix postRegisterOutput completable *(2020-02-22)*
- log poolId *(2020-02-22)*
- use FileLock *(2020-02-22)*
- use FileLock *(2020-02-22)*
- write files safely *(2020-02-22)*
- add mixState.nbMixingMustMix, nbMixingLiquidity, nbQueuedMustMix, nbQueuedLiquidity *(2020-02-21)*
- use daemon threads *(2020-02-21)*
- upgrade hibernate-validator *(2020-02-18)*
- #17 random utxo selection *(2020-02-18)*

## 0.23.10
### 

- move maxOutputs to Tx0Config *(2020-02-14)*
- #17 random utxo selection *(2020-02-13)*
- add WhirlpoolWalletConfig.overspendPerPool *(2020-02-11)*
- add WhirlpoolUtxoPriorityComparatorTest *(2020-02-07)*
- #17 UTXO selection for freeriding based on confirmations *(2020-02-06)*

## 0.23.9
### 

- adapt for android *(2020-02-06)*

## 0.23.8
### 

- fix NPE *(2020-02-03)*

## 0.23.7
### 

- xmanager-protocol 1.0.0 *(2020-02-02)*
- add XManagerClient *(2020-02-02)*

## 0.23.6
### 

- adapt for android *(2020-01-17)*

## 0.23.5
### 

- extlibj 0.0.10 *(2020-01-16)*

## 0.23.4
### 

- fix android httpObservable *(2020-01-10)*

## 0.23.3
### 

- add JacksonHttpClient *(2020-01-09)*

## 0.23.1
### 

- add Tx0Config.changeWallet *(2020-01-03)*

## 0.23.0
### 

- add MixFailReason.CANCEL *(2019-12-26)*
- add WhirlpoolWalletConfig.isMobile() *(2019-12-24)*
- use Observable for IHttpClient *(2019-12-23)*
- use Observable for IHttpClient *(2019-12-23)*
- use Observable for IHttpClient *(2019-12-23)*
- use Observable for IHttpClient *(2019-12-23)*
- use Observable for IHttpClient *(2019-12-23)*
- force rebuild *(2019-12-23)*
- remove WhirlpoolClientConfig.clientHash *(2019-12-20)*
- add WhirlpoolUtxoStatus.STOP + WhirlpoolClientConfig.clientHash *(2019-12-19)*
- add tx0Response.feeDiscountPercent *(2019-12-18)*
- restore Tx0ServiceTest *(2019-12-16)*
- add whirlpoolWallet.tx0Preview() *(2019-12-14)*
- add whirlpoolWallet.tx0Preview() *(2019-12-13)*
- tx0MinConfirmations = 0 *(2019-12-13)*
- use BehaviorSubject *(2019-12-13)*
- make WhirlpoolUtxo.mixsTarget nullable + add WhirlpoolUtxo.mixsTargetOrDefault *(2019-12-13)*
- observe mixing state *(2019-12-13)*
- use Observable *(2019-12-11)*
- don't retry mixing on fatal errors *(2019-12-09)*

## 0.22.2
### 

- extlibj 0.0.9 *(2019-12-06)*
- add ConfirmInputRequest.userHash *(2019-12-04)*
- add Tx0Config.badbankChange *(2019-11-26)*
- adapt for Android *(2019-11-22)*
- adapt for Android *(2019-11-22)*

## 0.22.1
### 

- adapt for Android *(2019-11-15)*
- update extlibj 0.0.8 *(2019-11-15)*
- rebuild *(2019-11-07)*
- rebuild *(2019-11-07)*
- Tx0Service: spend from multiple inputs *(2019-11-07)*
- Multiple fixs *(2019-11-06)*
 * Fix WhirlpoolWallet.stop() slow
 * Fix WhirlpoolWallet.stop() resetting UtxoConfig
 * rename SamouraiApi -&gt; BackendApi
 * rename SamouraiFee -&gt; MinerFee
- add Tx0.changeOutput *(2019-11-01)*
- fix utxoConfig reset on whirlpoolWallet.stop() *(2019-11-01)*
- speed-up whirlpoolWallet.stop() + refresh utxoStatus *(2019-11-01)*
- add MixOrchestratorState.toString() *(2019-10-18)*
- adapt for Android *(2019-10-18)*
- adapt for Android *(2019-10-18)*
- adapt for Android *(2019-10-06)*
- adapt for Android *(2019-10-06)*
- adapt for Android *(2019-10-06)*
- add whirlpoolWalletConfig.tx0Service *(2019-10-05)*
- adapt for Android *(2019-10-05)*
- adapt for Android *(2019-10-05)*
- adapt for Android *(2019-10-05)*
- add Tx0Service.buildTx0() *(2019-10-05)*
- add Tx0Service.buildTx0() *(2019-10-05)*

## 0.22.0
### 

- move BackendServer to whirlpool-protocol *(2019-09-29)*
- upgrade jackson-databind 2.9.10 *(2019-09-29)*
- add config.tx0MinConfirmations *(2019-09-28)*
- protocol update: Tx0Data: add feeValue + feeChange *(2019-09-28)*
- fix indexHandler.confirmUnconfirmed() *(2019-09-27)*
- fix indexHandler.confirmUnconfirmed() *(2019-09-19)*
- fix #9: refresh wallet indexs (to avoid address reuse while using mobile wallet) *(2019-09-17)*
- command-line utxos list: show mixsDone/mixsTarget *(2019-09-17)*
- override utxoConfig min target with config.mixs-target *(2019-09-17)*
- fix receiveAddress-index gap *(2019-09-17)*
- onMixFail: check if utxo still mixable *(2019-09-17)*
- fix receiveAddress-index gap *(2019-09-14)*

## 0.21.8
### 

- set default maxClients=null + remove idle threads *(2019-09-09)*
- add maxClientsPerPool *(2019-09-02)*
- better mix priority management *(2019-09-02)*
- fix "Insufficient utxo value for Tx0" *(2019-08-26)*
- Tx0FeeTarget.DEFAULT -> Tx0FeeTarget.MIN *(2019-08-26)*

## 0.21.7
### 

- double-check address-reuse + prev-tx-reuse before signing *(2019-08-09)*
- add DOJO pairing *(2019-08-08)*
- rename "trying to join a mix" -> "waiting for a mix" *(2019-07-22)*

## 0.21.6
### 

- retry failed utxo after errorDelay *(2019-07-12)*
- fix resource exhaustion too many threads #7 *(2019-07-12)*
- retry on mix fail *(2019-07-12)*
- resume postmix on startup only when autoMix=true *(2019-07-12)*
- fix onionUrl *(2019-07-12)*
- move BackendServer from extlibj to whirlpool-client *(2019-07-12)*
- upgrade jackson-databind 2.9.9.1 *(2019-07-11)*
- WhirlpoolWalletCacheData: 2 attempts before failing *(2019-07-09)*
- add whirlpoolServer.computeServerUrl() *(2019-07-09)*

## 0.21.5
### 

- update JavaExample *(2019-07-02)*
- keep utxo.lastActivity unchanged on mixStop() *(2019-06-30)*
- add IStompClientService *(2019-06-30)*
- exit MixSession when done *(2019-06-30)*

## 0.21.4
### 

- fix nullpointer when using SCODE *(2019-06-29)*
- add debug for tx0 fee *(2019-06-29)*
- fix premixBalanceCap enforcement *(2019-06-25)*
- AGPL 3.0 license *(2019-06-12)*

## 0.21.3
### 

- fix startup problem when --debug=false *(2019-05-25)*

## 0.21.2
### 

- add config refreshFeeDelay + refreshPoolsDelay *(2019-05-24)*
- adjust tx0MinerFee for bech32 *(2019-05-24)*

## 0.21.1
### 

- fix invalid pool selection for --autoTx0 *(2019-05-22)*

## 0.21.0
### 

- add mustMixBalanceCap *(2019-05-22)*
- fix configuration override from commandline args *(2019-05-21)*
- use BackendApi *(2019-05-20)*
- fetch feeAddress from whirlpool server + use BackendApi *(2019-05-19)*
- add autoTx0FeeTarget *(2019-05-14)*
- poolId mandatory for tx0 *(2019-05-14)*
- remove poolIdsByPriority *(2019-05-14)*
- fix utxoConfig saving + keep last activity unchanged when setting pool/mixsTarget *(2019-05-13)*

## 0.20.3
### 

- no check for premixBalanceMax as client doesn't know minerFeeMaxHard *(2019-05-09)*

## 0.20.2
### 

- forward error message to client *(2019-05-09)*
- add feeMin, feeMax, feeFallback, feeTargetTx0, feeTargetPremix *(2019-05-09)*

## 0.20.1
### 

- add whirlpoolUtxo.mixStep *(2019-05-07)*
- add whirlpoolWallet.getZpubX() *(2019-05-07)*
- TX0: revert feeIndex+premixIndex on error *(2019-05-07)*
- fetchFees for confirming in 4 blocks *(2019-05-07)*
- log premixValueFinal *(2019-05-01)*
- resume POSTMIX on startup when mixsTarget=MIXS_TARGET_UNLIMITED *(2019-05-01)*

## 0.20.0
### 

- add debug *(2019-04-27)*
- add MixFailReason *(2019-04-27)*
- adapt for TOR *(2019-04-27)*
- prevent user-agent tracking *(2019-04-27)*
- upgrade dependencies + protocol *(2019-04-26)*
- adapt for sockJS *(2019-04-25)*
- use sockJS *(2019-04-19)*

## 0.1.0
### 

- fix refreshMixableStatus *(2019-04-05)*

## 0.0.9
### 

- use ConcurrentHashMap *(2019-04-04)*
- add startDelay to AutoTx0Orchestrator *(2019-04-04)*
- fix --auto-aggregate-postmix *(2019-04-04)*
- fix --auto-aggregate-postmix *(2019-04-04)*
- fix MixableStatus.UNCONFIRMED stuck *(2019-04-04)*
- add utxo.MixableStatus + fix --auto-aggregate-postmix *(2019-04-04)*
- add utxo.MixableStatus + fix --auto-aggregate-postmix *(2019-04-04)*
- add utxo.MixableStatus + fix --auto-aggregate-postmix *(2019-04-04)*
- add utxo.MixableStatus + fix --auto-aggregate-postmix *(2019-04-04)*
- add utxo.MixableStatus + fix --auto-aggregate-postmix *(2019-04-04)*
- use LastValueFallbackSupplier *(2019-04-03)*
- client-side connection lost detection *(2019-04-03)*
- fix utxo config lost (POSTMIX discovery) *(2019-04-03)*
- fix utxo config lost (PREMIX->POSTMIX) *(2019-04-03)*
- add logs *(2019-04-02)*
- fix utxo config lost (PREMIX->POSTMIX) *(2019-04-02)*
- add logs *(2019-04-02)*
- add logs *(2019-04-01)*

## 0.0.5
### 

- fix test *(2019-03-29)*
- Bip84ApiWallet: use apiIndex when > localIndex *(2019-03-29)*
- fix "pool not found" *(2019-03-28)*

## 0.0.3
### 

- update maven-surefire-plugin *(2019-03-22)*
- use XORUtil *(2019-03-22)*
- fix utxoConfig preservation *(2019-03-22)*
- fix autoAggregatePostmix *(2019-03-22)*
- add utxoConfig persistance *(2019-03-09)*
- mixOrchestrator: fix ConcurrentModificationException *(2019-03-09)*

## 0.0.2
### 

- fix tx0 + stop mixing clients on stop wallet *(2019-03-06)*
- protocol 0.19 *(2019-03-06)*
- stop all mixing clients when stopping wallet *(2019-03-06)*
- quick fix for insuffucient fee when mixing POSTMIX pool.getMixAnonymitySet(); *(2019-03-05)*

## 0.0.1
### 

- maven-release-plugin *(2019-03-04)*
- prioritize premix before postmix *(2019-03-04)*
- fix mixsTarget @ postmix *(2019-03-04)*
- add whirlpoolWalletConfig.mixsTarget *(2019-03-04)*
- add notifiableException with cause *(2019-03-04)*
- remove resumeConfirmedInput + reset dialog on lost connection *(2019-03-02)*
- add resumeConfirmedInput + fix bugs on lost connexion *(2019-03-02)*
- preserve utxo config through tx0 & mix *(2019-03-02)*
- add WhirlpoolUtxoConfig *(2019-03-02)*
- add utxo configuration *(2019-03-02)*
- SamouraiApi: add mainnet *(2019-03-01)*
- protocol update: add poolsResponse.pool.feeValue *(2019-02-28)*
- log queued utxo reason *(2019-02-25)*
- auto-assign pool *(2019-02-25)*
- set utxo lastActivity *(2019-02-25)*
- add whirlpoolWallet.findPoolById() *(2019-02-25)*
- add whirlpoolWallet.getPoolsAvailable() + whirlpoolWallet.getPoolsByPreference() *(2019-02-25)*
- protocol update: add poolsResponse.feeValue *(2019-02-25)*
- protocol update: mixNbConfirmed -> nbConfirmed *(2019-02-25)*
- protocol update: minerFeeMin/Max -> mustMixMin/Max *(2019-02-25)*
- add WhirlpoolWalletService.testConnectivity() *(2019-02-25)*
- use ThrowingSupplier *(2019-02-25)*
- add whirlpoolWallet.mixStop() *(2019-02-22)*
- rename WhirlpoolUtxoStatus TX0 *(2019-02-19)*
- update JavaExample *(2019-02-19)*
- fix JavaExample link *(2019-02-19)*
- fix JavaExample link *(2019-02-19)*
- add Java example *(2019-02-19)*
- adapt for tx0 from GUI *(2019-02-19)*
- add WhirlpoolWalletCacheData: thread-safe cache data for WhirlpooWallet *(2019-02-19)*
- optimize fetchUtxos sync *(2019-02-19)*
- fix initPoolsByPriority() *(2019-02-19)*
- fix auto-aggregate-postmix *(2019-02-19)*
- log error on invalid poolId *(2019-02-19)*
- fix concurrency issues *(2019-02-19)*
- add tx0MaxOutputs *(2019-02-18)*
- optimize confirmed utxo detection *(2019-02-18)*
- mix: require 1 confirmation *(2019-02-18)*
- tx0: optimize *(2019-02-18)*
- tx0: fix unconfirmed utxo not refreshing *(2019-02-18)*
- tx0: spend whole utxo *(2019-02-18)*
- adapt for GUI *(2019-02-18)*
- tx0: wait for spendFrom confirmation *(2019-02-14)*
- add --tx0Delay *(2019-02-12)*
- fix sync *(2019-02-12)*
- fix sync *(2019-02-12)*
- add poolsByPriority *(2019-02-12)*
- fix bugs *(2019-02-12)*
- fix bugs *(2019-02-11)*
- fix sync *(2019-02-11)*
- add AutoMixOrchestrator *(2019-02-11)*
- add AutoTx0Orchestrator *(2019-02-11)*
- fix utxos cache *(2019-02-11)*
- fix protocol error: "mixStatus already completed: CONFIRM_INPUT" *(2019-02-10)*
- add utxo priority *(2019-02-10)*
- add multithreading mix *(2019-02-10)*
- add WhirlpoolUtxo *(2019-02-06)*
- add NotifiableException.status *(2019-01-29)*
- fix tx0 caching *(2019-01-27)*
- add Bip84ApiWallet.fetchBalance() *(2019-01-27)*
- add logs *(2019-01-27)*
- tx0Service: clear utxosDeposit cache before tx0 *(2019-01-27)*
- tx0Service: use change chain for change *(2019-01-27)*
- add WhirlpoolWalletService *(2019-01-27)*
- add Bip84PostmixHandler *(2019-01-22)*
- use FeeUtil *(2019-01-19)*
- Tx0Service: fix invalid nbPremix when generating maximum possible premixes outputs *(2019-01-19)*
- SamouraiApi.pushTx: add logs *(2019-01-19)*
- tx0Service: better UTXO selection *(2019-01-15)*
- add Tx0Service *(2019-01-14)*
- add PushTxService *(2019-01-07)*
- add FeeUtils *(2019-01-06)*
- add Tx0Service *(2019-01-06)*
- add Bip84Wallet *(2019-01-05)*
- add SamouraiApi *(2019-01-05)*
- remove keys from debug logs *(2018-12-29)*
- force rebuild *(2018-12-16)*
- force rebuild *(2018-12-16)*
- protocol update: add PoolsResponse.feePayload64 + scode *(2018-12-16)*
- add build status *(2018-12-06)*
- enable travis ci *(2018-12-06)*
- add IHttpClient.postUrlEncoded() *(2018-12-06)*
- Downgrade client to java 1.6 *(2018-12-05)*
- protocol upgrade: add PoolsResponse.feePaymentCode *(2018-11-24)*
- move xorMask to protocol *(2018-11-24)*
- protocol update: use WhirlpoolFee *(2018-11-24)*
- protocol update: map endpoints to /ws and /rest *(2018-11-22)*
- add clientCryptoService.xorMask *(2018-11-22)*
- protocol upgrade: remove RegisterInputRequest.pubkey64 *(2018-11-20)*
- Unlicense *(2018-11-11)*
- working release *(2018-11-09)*
- multiClientManager.isDone: wait for minimal number of successes *(2018-11-08)*
- use protocol 0.14 *(2018-11-06)*
- use ssl *(2018-11-06)*
- MixDialog: fix mixId tracking *(2018-11-06)*
- fix logPrefix *(2018-11-06)*
- multiClientManager.isDone: wait for minimal number of successes *(2018-11-06)*
- multiClientManager.waitDone: return boolean success *(2018-11-05)*
- use bech32Util.toBech32 *(2018-11-05)*
- use ExtLibJ.TxUtil *(2018-11-04)*
- use WhirlpoolProtocol.encodeByte/decodeByte *(2018-10-29)*
- encode bytes with Z85 *(2018-10-29)*
- update groupId to com.github.Samourai-Wallet *(2018-10-29)*
- use whirlpool-protocol:develop *(2018-10-26)*
- add simplified constructor MixParams(Pool, ...) *(2018-10-24)*
- add prettier + apply *(2018-10-24)*
- use ExtLibJ develop *(2018-10-24)*
- update readme *(2018-10-24)*
- fix typo *(2018-10-23)*
- fail on duplicate SubscribePoolResponse *(2018-10-20)*
- fix vpub *(2018-10-04)*
- MultiClientManager.onDone *(2018-10-04)*
- ignore duplicate SubscribePoolResponse *(2018-10-03)*
- add MultiClientManager *(2018-10-03)*
- Protocol upgrade: add PoolInfo.mixNbConfirmed *(2018-09-29)*
- protocol upgrade: REGISTER_INPUT on pool, CONFIRM_INPUT on mix *(2018-09-29)*
- protocol upgrade: mustMix queue management *(2018-09-25)*
- fix ClientUtils.prefixLogger() *(2018-09-25)*
- move httpClient / stompClient as generic components + add IStompMessage *(2018-09-25)*
- add MixHandler.getReceiveKey() *(2018-09-25)*
- log error *(2018-09-19)*
- update documentation *(2018-09-16)*
- protocol upgrade: publickKey -> publicKeyBase64 *(2018-09-16)*
- protocol upgrade: publickKey -> publicKeyBase64 *(2018-09-15)*
- move branch android -> master *(2018-09-15)*
- manage asynchronous connexion *(2018-09-15)*
- remove java implementation *(2018-09-15)*
- add StompTransport.onTransportConnected() *(2018-09-14)*
- IStompClient -> IWhirlpoolStompClient *(2018-09-13)*
- update dev doc *(2018-09-13)*
- add LoggingWhirlpoolClientListener.log *(2018-09-13)*
- IStompClient -> IWhirlpoolStompClient *(2018-09-13)*
- restore commandline application *(2018-09-13)*
- fix stomp transport *(2018-09-13)*
- update for samourai-wallet-android *(2018-09-13)*
- ExtLibJ update: add ISecretPoint and Bech32UtilGeneric *(2018-09-13)*
- ExtLibJ update: BIP47Util -> BIP47UtilGeneric *(2018-09-13)*
- downgrade to java 7 *(2018-09-12)*
- back to maven *(2018-09-12)*
- remove spring dependencies, add abstraction layer for StompClient and HttpClient *(2018-09-12)*
- switch to gradle *(2018-09-11)*
- move core mix logic to MixProcess *(2018-09-09)*
- check fees in verifyTx() again *(2018-09-09)*
- optimize subscribe process *(2018-09-09)*
- protocol upgrade: add ENDPOINT_CONNECT *(2018-09-08)*
- update developers documentation *(2018-09-08)*
- add --test-mode to disable tx0 verifications (when server is in testMode) *(2018-09-08)*
- don't reveal output if already signed, don't sign if already revealed output *(2018-09-06)*
- update test *(2018-08-31)*
- Forward REST errors to user *(2018-08-31)*
- protocol upgrade: (security) use receiveAddress as bordereau *(2018-08-31)*
- security: client-side hard limit for acceptable fees *(2018-08-30)*
- security: client verifies tx structure before signing it *(2018-08-29)*
- protocol upgrade: (privacy) RegisterOutputRequest provides verifiable inputsHash instead of random mixId (server could trick client providing different mixId to deanonymize mix) *(2018-08-28)*
- protocol upgrade: (privacy) compute registerOutputUrl on client-side, not from server (server could trick client providing different urls to deanonymize mix) *(2018-08-28)*
- use UUID for generating unique bordereau *(2018-08-28)*
- quit client on success or fail *(2018-08-27)*
- remove unused Premix classes *(2018-08-19)*
- protocol upgrade: no paymentCode is transmitted to server *(2018-08-19)*
- add optional argument: [--paynym-index=0] *(2018-08-16)*
- add LiquidityQueuedResponse *(2018-08-16)*
- add link to README-DEV *(2018-08-16)*
- add mixId to RegisterInputResponse *(2018-08-16)*
- Use interface to hide client complexity *(2018-08-16)*
 * rename new WhirlpoolClient() -&gt; WhirlpoolClientImpl.newClient()
 * rename whirlpoolClient.listPools() -&gt; whirlpoolClient.fetchPools()
- refactor post-mix account index value *(2018-08-14)*
- MixHandler: use post-mix account index *(2018-08-14)*
- Application: use post-mix account index *(2018-08-14)*
- remove --liquidity to autodetect liquidities from utxo-balance *(2018-08-13)*
- add ApplicationTest *(2018-08-12)*
- update doc *(2018-08-12)*
- add --utxo-balance *(2018-08-09)*
- move minerFee calculation to WhirlpoolProtocol *(2018-08-07)*
- log protocolVersion *(2018-08-07)*
- add minerFeeMin-minerFeeMax for variable input balances *(2018-08-07)*
- add multipool *(2018-08-04)*
- internal release build *(2018-07-31)*
- reorganize protocol packages *(2018-07-27)*
- require same protocol version for client + server *(2018-07-26)*
- upgrade to spring 2.0.3.RELEASE + add test dependencies *(2018-07-26)*
- internal release build *(2018-07-25)*
- add protocol versioning *(2018-07-24)*
- client automatically receives roundStatus on chanel subscription *(2018-07-24)*
- rename classes (Round -> Mix) *(2018-07-24)*
- add integration documentation *(2018-07-24)*
- rename classes (Round -> Mix) *(2018-07-24)*
- doc for client arguments *(2018-07-23)*
- static logger *(2018-07-23)*
- SimpleWhirlpoolClient: if --debug, show privkey for receive *(2018-07-12)*
- more logs *(2018-07-09)*
- more logs *(2018-07-09)*
- wait for next round when joining on a running round *(2018-07-09)*
- add RoundResultSuccess to listener *(2018-07-01)*
- fix WhirlpoolMultiRoundClientListener parameters *(2018-06-25)*
- new listener: roundSuccess() *(2018-06-17)*
- add argument --rounds for multiple rounds subscription *(2018-06-14)*
- add WhirlpoolMultiRoundClient for multiple rounds subscription *(2018-06-14)*
- add RoundParams + WhirlpoolClientListener *(2018-06-14)*
- client automatically receives roundStatus on chanel subscription *(2018-06-09)*
- manage round resuming on client reconnection *(2018-06-03)*
- externalize client configuration to WhirlpoolClientConfiguration *(2018-06-03)*
- reconnect and resume round on lost connection *(2018-06-03)*
- exit on ErrorResponse *(2018-06-01)*
- add argument --liquidity *(2018-05-12)*
- exit application when done *(2018-05-12)*
- enable debug logs with --debug *(2018-05-12)*
- clean logs *(2018-05-12)*
- add arguments for runnable application *(2018-05-12)*
- Add runnable Application *(2018-05-11)*
- WhirlpoolClient reads publicKey, networkId and registerOutputUrl from server *(2018-05-11)*
- doc: usage + build instructions *(2018-05-10)*
- WhirlpoolClient reads denomination and minerFee from server *(2018-05-10)*
- add whirlpool-client *(2018-05-09)*
- Initial commit *(2018-04-24)*

