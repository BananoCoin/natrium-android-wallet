package com.banano.kaliumwallet.util;

import java.util.Locale;

/**
 * Localization utilities
 */
public class LocaleUtil {

    public static Locale getLocaleFromStr(String localeStr) {
        localeStr = localeStr.replace("-", "_");
        String[] args = localeStr.split("_");
        if (args.length > 2) {
            return new Locale(args[0], args[1], args[2]);
        } else if (args.length > 1) {
            return new Locale(args[0], args[1].toUpperCase());
        } else if (args.length == 1) {
            return new Locale(args[0]);
        }
        return Locale.getDefault();
    }
}
