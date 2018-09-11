package com.banano.kaliumwallet.util;

import java.security.SecureRandom;

public final class SecureRandomUtil {

    private static final SecureRandom SECURE_RANDOM;
    private static int isAndroid = -1;

    static {
        if (isAndroidRuntime()) {
            new LinuxSecureRandom();
        }
        SECURE_RANDOM = new SecureRandom();
    }

    private SecureRandomUtil() {
    }

    public static SecureRandom secureRandom() {
        return SECURE_RANDOM;
    }

    static boolean isAndroidRuntime() {
        if (isAndroid == -1) {
            final String runtime = System.getProperty("java.runtime.name");
            isAndroid = (runtime != null && runtime.equals("Android Runtime")) ? 1 : 0;
        }
        return isAndroid == 1;
    }

}
