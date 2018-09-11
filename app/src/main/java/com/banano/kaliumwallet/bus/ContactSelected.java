package com.banano.kaliumwallet.bus;

/**
 * Bus event for when a user chooses a contact on the send screen
 */
public class ContactSelected {
    private String name;
    private String address;

    public ContactSelected(String name, String address) {
        this.name = name;
        this.address = address;
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
