package co.banano.natriumwallet.network.model.response;

import co.banano.natriumwallet.network.model.BaseResponse;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class BlocksInfoResponse extends BaseResponse {
    @SerializedName("blocks")
    private HashMap<String, BlockInfoItem> blocks;

    public BlocksInfoResponse() {
    }

    public HashMap<String, BlockInfoItem> getBlocks() {
        return blocks;
    }

    public void setBlocks(HashMap<String, BlockInfoItem> blocks) {
        this.blocks = blocks;
    }
}
