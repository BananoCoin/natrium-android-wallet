package com.banano.kaliumwallet.network.model.response;

import com.banano.kaliumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

/**
 * Error response from service
 */

public class WarningResponse extends BaseResponse {
    @SerializedName("warning")
    private String warning;

    public WarningResponse() {
    }

    public WarningResponse(String warning) {
        this.warning = warning;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
