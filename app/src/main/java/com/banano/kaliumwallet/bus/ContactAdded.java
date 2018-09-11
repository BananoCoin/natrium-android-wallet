package com.banano.kaliumwallet.bus;

public class ContactAdded {
    private String name;
    private String address;

    public ContactAdded(String name, String address) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
