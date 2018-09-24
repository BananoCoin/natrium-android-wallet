package co.banano.natriumwallet.network.model.request;

import co.banano.natriumwallet.network.model.Actions;
import co.banano.natriumwallet.network.model.BaseRequest;
import com.google.gson.annotations.SerializedName;

/**
 * Fetch work for a transaction
 */

public class WorkRequest extends BaseRequest {
    @SerializedName("action")
    private String action;

    @SerializedName("hash")
    private String hash;

    public WorkRequest() {
        this.action = Actions.WORK.toString();
    }

    public WorkRequest(String hash) {
        this.action = Actions.WORK.toString();
        this.hash = hash;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
