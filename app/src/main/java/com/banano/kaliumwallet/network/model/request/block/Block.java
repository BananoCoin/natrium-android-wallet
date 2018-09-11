package com.banano.kaliumwallet.network.model.request.block;

import com.banano.kaliumwallet.network.model.BlockTypes;
import com.google.gson.annotations.SerializedName;

/**
 * Base block
 */
public class Block {
    @SerializedName("work")
    protected String work;

    private transient BlockTypes internal_block_type;

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public BlockTypes getInternal_block_type() {
        return internal_block_type;
    }

    public void setInternal_block_type(BlockTypes internal_block_type) {
        this.internal_block_type = internal_block_type;
    }
}
