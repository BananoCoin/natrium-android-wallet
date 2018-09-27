package co.banano.natriumwallet.network.model.response;

import co.banano.natriumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Account history
 */

public class AccountHistoryResponse extends BaseResponse {
    // entries are sorted newest to oldest
    @SerializedName("history")
    private List<AccountHistoryResponseItem> history;

    public AccountHistoryResponse() {
    }

    public AccountHistoryResponse(List<AccountHistoryResponseItem> history) {
        this.history = history;
    }

    public List<AccountHistoryResponseItem> getHistory() {
        return history;
    }

    public void setHistory(List<AccountHistoryResponseItem> history) {
        this.history = history;
    }
}
