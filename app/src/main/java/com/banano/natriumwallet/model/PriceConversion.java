package com.banano.natriumwallet.model;

public enum PriceConversion {
    BTC("BTC"),
    NONE("NONE");

    private String type;

    PriceConversion(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
