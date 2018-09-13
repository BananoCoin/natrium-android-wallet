package com.banano.natriumwallet.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Preconfigured representatives to choose from
 */

public class PreconfiguredRepresentatives {

    private static List<String> representatives = Arrays.asList(
            "xrb_3o7uzba8b9e1wqu5ziwpruteyrs3scyqr761x7ke6w1xctohxfh5du75qgaj"
    );

    public static String getRepresentative() {
        int index = new Random().nextInt(representatives.size());
        String rep = representatives.get(index);
        Timber.d("Representative: %s", rep);
        return rep;
    }
}
