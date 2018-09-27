package co.banano.natriumwallet.network.model.request;

import co.banano.natriumwallet.network.model.Actions;
import co.banano.natriumwallet.network.model.BaseRequest;
import com.google.gson.annotations.SerializedName;

/**
 * Account Check Request
 */

public class AccountCheckRequest extends BaseRequest {
    @SerializedName("action")
    private String action;

    @SerializedName("account")
    private String account;

    public AccountCheckRequest() {
        this.action = Actions.CHECK.toString();
    }

    public AccountCheckRequest(String account) {
        this.action = Actions.CHECK.toString();
        this.account = account;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
