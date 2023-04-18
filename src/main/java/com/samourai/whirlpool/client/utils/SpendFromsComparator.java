
package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.api.backend.beans.UnspentOutput;

import java.util.Comparator;

public class SpendFromsComparator implements Comparator<UnspentOutput> {
    // sort descending order
    public int compare(UnspentOutput o1, UnspentOutput o2) {
        return o1.value - o2.value < 0L ? 1 : -1;
    }
}