package com.banano.kaliumwallet.di.activity;

import android.content.Context;

import com.banano.kaliumwallet.model.KaliumWallet;
import com.banano.kaliumwallet.network.AccountService;
import com.banano.kaliumwallet.network.model.Actions;
import com.banano.kaliumwallet.network.model.BaseResponse;
import com.banano.kaliumwallet.network.model.BlockTypes;
import com.banano.kaliumwallet.network.model.response.AccountCheckResponse;
import com.banano.kaliumwallet.network.model.response.AccountHistoryResponse;
import com.banano.kaliumwallet.network.model.response.BlockResponse;
import com.banano.kaliumwallet.network.model.response.BlocksInfoResponse;
import com.banano.kaliumwallet.network.model.response.CurrentPriceResponse;
import com.banano.kaliumwallet.network.model.response.ErrorResponse;
import com.banano.kaliumwallet.network.model.response.ProcessResponse;
import com.banano.kaliumwallet.network.model.response.SubscribeResponse;
import com.banano.kaliumwallet.network.model.response.TransactionResponse;
import com.banano.kaliumwallet.network.model.response.WarningResponse;
import com.banano.kaliumwallet.network.model.response.WorkResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dagger.Module;
import dagger.Provides;
import io.gsonfire.GsonFireBuilder;

@Module
public class ActivityModule {
    private final Context mContext;
    private KaliumWallet mWallet;

    public ActivityModule(Context context) {
        mContext = context;
    }

    @Provides
    Context providesActivityContext() {
        return mContext;
    }

    @Provides
    KaliumWallet providesNanoWallet(Context context) {
        if (mWallet == null) {
            mWallet = new KaliumWallet(context);
        }
        return mWallet;
    }

    @Provides
    @ActivityScope
    AccountService providesAccountService(Context context) {
        return new AccountService(context);
    }

    @Provides
    @ActivityScope
    Gson providesGson() {
        // configure gson to detect and set proper types
        GsonFireBuilder builder = new GsonFireBuilder()
                .registerPreProcessor(BaseResponse.class, (clazz, src, gson) -> {
                    // figure out the response type based on what fields are in the response
                    if (src.isJsonObject() && src.getAsJsonObject().get("messageType") == null) {
                        if (src.getAsJsonObject().get("uuid") != null ||
                                (src.getAsJsonObject().get("frontier") != null &&
                                        src.getAsJsonObject().get("representative_block") != null) ||
                                (src.getAsJsonObject().get("error") != null &&
                                        src.getAsJsonObject().get("currency") != null)) {
                            // subscribe response
                            src.getAsJsonObject().addProperty("messageType", Actions.SUBSCRIBE.toString());
                        } else if (src.getAsJsonObject().get("history") != null) {
                            // history response
                            src.getAsJsonObject().addProperty("messageType", Actions.HISTORY.toString());
                            // if history is an empty string, make it an array instead
                            if (!src.getAsJsonObject().get("history").isJsonArray()) {
                                src.getAsJsonObject().add("history", new JsonArray());
                            }
                        } else if (src.getAsJsonObject().get("currency") != null) {
                            // current price
                            src.getAsJsonObject().addProperty("messageType", Actions.PRICE.toString());
                        } else if (src.getAsJsonObject().get("work") != null) {
                            // work response
                            src.getAsJsonObject().addProperty("messageType", Actions.WORK.toString());
                        } else if (src.getAsJsonObject().get("error") != null) {
                            // error response
                            src.getAsJsonObject().addProperty("messageType", Actions.ERROR.toString());
                        } else if (src.getAsJsonObject().get("warning") != null) {
                            // warning response
                            src.getAsJsonObject().addProperty("messageType", Actions.WARNING.toString());
                        } else if (src.getAsJsonObject().get("block") != null && src.getAsJsonObject().get("account") != null
                                && src.getAsJsonObject().get("hash") != null) {
                            // block response
                            src.getAsJsonObject().addProperty("messageType", "block");
                        } else if (src.getAsJsonObject().get("ready") != null) {
                            // account check response
                            src.getAsJsonObject().addProperty("messageType", Actions.CHECK.toString());
                        } else if (src.getAsJsonObject().get("hash") != null) {
                            // process block response
                            src.getAsJsonObject().addProperty("messageType", Actions.PROCESS.toString());
                        } else if (src.getAsJsonObject().get("type") != null &&
                                src.getAsJsonObject().get("type").getAsString().equals(BlockTypes.STATE.toString())) {
                            // state block response
                            src.getAsJsonObject().addProperty("messageType", Actions.PROCESS.toString());
                        } else if (src.getAsJsonObject().get("blocks") != null) {
                            JsonElement blocksElement = src.getAsJsonObject().get("blocks");
                            if (blocksElement.isJsonObject()) {
                                JsonObject blocks = blocksElement.getAsJsonObject();
                                String key = blocks.keySet().iterator().next();
                                if (key != null) {
                                    if (blocks.get(key).getAsJsonObject().has("block_account")) {
                                        // get blocks info response
                                        src.getAsJsonObject().addProperty("messageType", Actions.GET_BLOCKS_INFO.toString());
                                    }
                                }
                            }
                        } else if (src.getAsJsonObject().get("representative") != null) {
                            src.getAsJsonObject().addProperty("messageType", Actions.REPRESENTATIVE.toString());
                        }
                    }
                }).registerTypeSelector(BaseResponse.class, readElement -> {
                    // return proper type based on the message type that was set
                    if (readElement.isJsonObject() && readElement.getAsJsonObject().get("messageType") != null) {
                        String kind = readElement.getAsJsonObject().get("messageType").getAsString();
                        if (kind.equals(Actions.SUBSCRIBE.toString())) {
                            return SubscribeResponse.class;
                        } else if (kind.equals(Actions.HISTORY.toString())) {
                            return AccountHistoryResponse.class;
                        } else if (kind.equals(Actions.PRICE.toString())) {
                            return CurrentPriceResponse.class;
                        } else if (kind.equals(Actions.WORK.toString())) {
                            return WorkResponse.class;
                        } else if (kind.equals(Actions.ERROR.toString())) {
                            return ErrorResponse.class;
                        } else if (kind.equals(Actions.WARNING.toString())) {
                            return WarningResponse.class;
                        } else if (kind.equals(Actions.CHECK.toString())) {
                            return AccountCheckResponse.class;
                        } else if (kind.equals(Actions.PROCESS.toString())) {
                            return ProcessResponse.class;
                        } else if (kind.equals(Actions.GET_BLOCKS_INFO.toString())) {
                            return BlocksInfoResponse.class;
                        } else if (kind.equals("block")) {
                            return TransactionResponse.class;
                        } else if (kind.equals("contents")) {
                            return BlockResponse.class;
                        } else {
                            return null; // returning null will trigger Gson's default behavior
                        }
                    } else {
                        return null;
                    }
                });

        return builder.createGson();
    }
}
