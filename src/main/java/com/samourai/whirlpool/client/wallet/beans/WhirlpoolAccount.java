package com.samourai.whirlpool.client.wallet.beans;

import java.util.Arrays;
import java8.util.Optional;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public enum WhirlpoolAccount {
  DEPOSIT(0, "deposit", true),
  PREMIX(Integer.MAX_VALUE - 2, "premix", true),
  POSTMIX(Integer.MAX_VALUE - 1, "postmix", true),
  BADBANK(Integer.MAX_VALUE - 3, "badbank", false);

  private int accountIndex;
  private String persistKey;
  private boolean active;

  WhirlpoolAccount(int accountIndex, String persistKey, boolean active) {
    this.accountIndex = accountIndex;
    this.persistKey = persistKey;
    this.active = active;
  }

  public int getAccountIndex() {
    return accountIndex;
  }

  public String getPersistKeyMain() {
    return persistKey;
  }

  public String getPersistKeyChange() {
    return persistKey + "_change";
  }

  public boolean isActive() {
    return active;
  }

  public static Optional<WhirlpoolAccount> find(int index) {
    for (WhirlpoolAccount whirlpoolAccount : values()) {
      if (whirlpoolAccount.getAccountIndex() == index) {
        return Optional.of(whirlpoolAccount);
      }
    }
    return Optional.empty();
  }

  public static WhirlpoolAccount[] getListByActive(final boolean active) {
    return StreamSupport.stream(Arrays.asList(values()))
        .filter(
            new Predicate<WhirlpoolAccount>() {
              @Override
              public boolean test(WhirlpoolAccount account) {
                return account.isActive() == active;
              }
            })
        .collect(Collectors.<WhirlpoolAccount>toList())
        .toArray(new WhirlpoolAccount[] {});
  }
}
