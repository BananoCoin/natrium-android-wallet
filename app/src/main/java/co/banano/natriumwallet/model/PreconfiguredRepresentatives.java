package co.banano.natriumwallet.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Preconfigured representatives to choose from
 */

public class PreconfiguredRepresentatives {

    private static List<String> representatives = Arrays.asList(
            "xrb_1natrium1o3z5519ifou7xii8crpxpk8y65qmkih8e8bpsjri651oza8imdd"
    );

    public static String getRepresentative() {
        int index = new Random().nextInt(representatives.size());
        String rep = representatives.get(index);
        Timber.d("Representative: %s", rep);
        return rep;
    }
}
