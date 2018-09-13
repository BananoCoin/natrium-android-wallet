package com.banano.natriumwallet.network.model;

/**
 * Available Websocket Actions
 */

public enum Actions {
    CHECK("account_check"), SUBSCRIBE("account_subscribe"), HISTORY("account_history"),
    PENDING("pending"), PRICE("price_data"), PROCESS("process"), GET_BLOCK("block"),
    WORK("work_generate"), ERROR("error"), WARNING("warning"), GET_BLOCKS_INFO("blocks_info"),
    REPRESENTATIVE("account_representative");

    String actionName;

    Actions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString() {
        return actionName;
    }
}
