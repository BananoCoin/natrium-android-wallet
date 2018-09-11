package com.banano.kaliumwallet.model;

public enum PriceConversion {
    BTC("BTC"),
    NANO("NANO"),
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
