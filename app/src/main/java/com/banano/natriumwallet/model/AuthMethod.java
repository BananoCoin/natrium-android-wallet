package com.banano.natriumwallet.model;

/**
 * Enum that maps locales to language strings for runtime language changes
 */
public enum AuthMethod {
    FINGERPRINT("FINGERPRINT"),
    PIN("PIN");


    private String name;

    AuthMethod(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}