package co.banano.natriumwallet.network.model.request;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import co.banano.natriumwallet.network.model.Actions;
import co.banano.natriumwallet.network.model.BaseRequest;

public class AccountsBalancesRequest extends BaseRequest {
    @SerializedName("action")
    private String action;

    @SerializedName("accounts")
    private List<String> accounts;


    public AccountsBalancesRequest(List<String> accounts) {
        this.action = Actions.BALANCES.toString();
        this.accounts = accounts;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<String> accounts) {
        this.accounts = accounts;
    }
}