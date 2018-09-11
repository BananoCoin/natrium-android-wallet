package com.banano.kaliumwallet.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Preconfigured representatives to choose from
 */

public class PreconfiguredRepresentatives {

    private static List<String> representatives = Arrays.asList(
            "ban_1ka1ium4pfue3uxtntqsrib8mumxgazsjf58gidh1xeo5te3whsq8z476goo"
    );

    public static String getRepresentative() {
        int index = new Random().nextInt(representatives.size());
        String rep = representatives.get(index);
        Timber.d("Representative: %s", rep);
        return rep;
    }
}
