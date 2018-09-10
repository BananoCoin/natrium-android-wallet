package com.banano.kaliumwallet.bus;

public class ContactAdded {
    private String name;

    public ContactAdded(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
