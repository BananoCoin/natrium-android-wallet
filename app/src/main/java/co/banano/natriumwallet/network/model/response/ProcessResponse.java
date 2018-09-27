package co.banano.natriumwallet.network.model.response;

import co.banano.natriumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

/**
 * hash response from service
 */

public class ProcessResponse extends BaseResponse {
    @SerializedName("hash")
    private String hash;

    public ProcessResponse() {
    }

    public ProcessResponse(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
