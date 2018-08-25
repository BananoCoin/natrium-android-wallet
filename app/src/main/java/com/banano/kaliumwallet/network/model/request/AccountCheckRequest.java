package com.banano.kaliumwallet.network.model.request;

import com.google.gson.annotations.SerializedName;

import com.banano.kaliumwallet.network.model.Actions;
import com.banano.kaliumwallet.network.model.BaseRequest;

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
