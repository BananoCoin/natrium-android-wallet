package com.banano.natriumwallet.util;

import timber.log.Timber;

public class ExceptionHandler {
    public static void handle(Throwable t) {
        if (t != null) {
            // Log to console
            Timber.e(t.getMessage());
        }
    }
}