package co.banano.natriumwallet.network.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

import co.banano.natriumwallet.network.model.BaseResponse;

public class PendingTransactionResponse extends BaseResponse {
    @SerializedName("blocks")
    private HashMap<String, PendingTransactionResponseItem> blocks;

    private String account;

    public PendingTransactionResponse() {
    }

    public HashMap<String, PendingTransactionResponseItem> getBlocks() {
        return blocks;
    }

    public void setBlocks(HashMap<String, PendingTransactionResponseItem> blocks) {
        this.blocks = blocks;
    }

    public String getAccount() {
        return account.replace("nano_", "xrb_");
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
