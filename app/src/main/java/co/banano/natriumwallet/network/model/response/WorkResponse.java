package co.banano.natriumwallet.network.model.response;

import co.banano.natriumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

/**
 * Pushed price data - currently sent every minute to all clients
 */

public class WorkResponse extends BaseResponse {
    @SerializedName("work")
    private String work;

    public WorkResponse() {
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }
}
