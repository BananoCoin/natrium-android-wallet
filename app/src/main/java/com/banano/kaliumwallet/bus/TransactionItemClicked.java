package com.banano.kaliumwallet.bus;

public class TransactionItemClicked {
    private String hash;
    private String account;

    public TransactionItemClicked(String hash, String account) {
        this.hash = hash;
        this.account = account;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
