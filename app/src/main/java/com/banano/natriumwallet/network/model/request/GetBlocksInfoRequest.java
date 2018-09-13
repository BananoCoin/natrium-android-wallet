package com.banano.natriumwallet.network.model.request;

import com.banano.natriumwallet.network.model.Actions;
import com.banano.natriumwallet.network.model.BaseRequest;
import com.google.gson.annotations.SerializedName;

/**
 * Retrieve hash history
 */

public class GetBlocksInfoRequest extends BaseRequest {
    @SerializedName("action")
    private String action;

    @SerializedName("hashes")
    private String[] hashes;

    @SerializedName("balance")
    private boolean balance;

    public GetBlocksInfoRequest() {
        this.action = Actions.GET_BLOCKS_INFO.toString();
        this.hashes = new String[]{""};
        this.balance = true;
    }

    public GetBlocksInfoRequest(String[] hashes) {
        this.action = Actions.GET_BLOCKS_INFO.toString();
        this.hashes = hashes;
        this.balance = true;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String[] getHashes() {
        return hashes;
    }

    public void setHashes(String[] hashes) {
        this.hashes = hashes;
    }
}
