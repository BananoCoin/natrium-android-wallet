package com.banano.kaliumwallet.network.model.response;

import android.text.Spannable;
import android.text.SpannableString;

import com.google.gson.annotations.SerializedName;

import com.banano.kaliumwallet.model.Address;
import com.banano.kaliumwallet.network.model.BlockTypes;
import com.banano.kaliumwallet.util.NumberUtil;

/**
 * Account History Item
 */

public class AccountHistoryResponseItem {
    // valid block types: send, receive, change, open. this command only returns
    // send and receive data. if last txn is 'change', it wont be shown here
    @SerializedName("type")
    private String type;

    // for a send block, this is the destination account
    @SerializedName("account")
    private String account;

    // raw-value of the transaction
    @SerializedName("amount")
    private String amount;

    // hash of the block, use to get the full block data from server. also reference this for
    // creating new blocks or keeping track of new txns
    @SerializedName("hash")
    private String hash;

    private String contactName;

    public AccountHistoryResponseItem() {
    }

    public AccountHistoryResponseItem(String type, String account, String amount, String hash) {
        this.type = type;
        this.account = account;
        this.amount = amount;
        this.hash = hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccount() {
        return account;
    }

    public Spannable getAddressShort() {
        return new SpannableString(new Address(account).getShortString());
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        return NumberUtil.getRawAsUsableString(amount);
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isSend() {
        return this.type.equals(BlockTypes.SEND.toString());
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
}
