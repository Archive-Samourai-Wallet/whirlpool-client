FEATURE: TX0-CASCADE
____________________

Currently a lot of users are using TX0 as following:
- make a TX0 to a big mixing pool
- use previous TX0's change to do another TX0 to a smaller mixing pool
- repeat ...

We would like to make it easier to do this automatically :
- make a TX0 to choosen pool
- for each smaller pool available, make another TX0 from previous TX0's change
- such TX0 would be free of charge

1. IMPLEMENT WhirlpoolWallet.tx0Cascade

- checkout whirlpool-client => branch "features/tx0-cascade"
- read specifications in WhirlpoolWallet.tx0Cascade()
- implement WhirlpoolWallet.tx0Cascade()
- use WhirlpoolWalletTest.tx0Cascade() to test implementation


2. IMPLEMENT controller in wallet-cli

- more details coming soon


3. IMPLEMENT free fee policy for TX0-CASCADE

- more details coming soon
