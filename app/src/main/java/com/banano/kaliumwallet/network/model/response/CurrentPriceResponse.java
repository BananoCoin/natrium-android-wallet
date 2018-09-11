package com.banano.kaliumwallet.network.model.response;

import com.banano.kaliumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

/**
 * Pushed price data - currently sent every minute to all clients
 */

public class CurrentPriceResponse extends BaseResponse {
    @SerializedName("currency")
    private String currency;

    @SerializedName("price")
    private String price;

    @SerializedName("btc")
    private String btc;

    @SerializedName("nano")
    private String nano;

    public CurrentPriceResponse() {
    }

    public CurrentPriceResponse(String currency, String price, String btc, String nano) {
        this.currency = currency;
        this.price = price;
        this.btc = btc;
        this.nano = nano;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getBtc() {
        // strip all unneeded characters
        return btc != null ? btc.replaceAll("[^\\d.]", "") : null;
    }

    public void setBtc(String btc) {
        this.btc = btc;
    }

    public String getNano() {
        // strip all unneeded characters
        return nano != null ? nano.replaceAll("[^\\d.]", "") : null;
    }

    public void setNano(String nano) {
        this.nano = nano;
    }
}
