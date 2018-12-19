package co.banano.natriumwallet.network.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

import co.banano.natriumwallet.network.model.BaseResponse;

public class AccountsBalancesResponse extends BaseResponse {
    @SerializedName("balances")
    private HashMap<String, AccountBalanceItem> balances;

    public AccountsBalancesResponse() {
    }

    public HashMap<String, AccountBalanceItem> getBalances() {
        return balances;
    }

    public void setBalances(HashMap<String, AccountBalanceItem> balances) {
        this.balances = balances;
    }
}
