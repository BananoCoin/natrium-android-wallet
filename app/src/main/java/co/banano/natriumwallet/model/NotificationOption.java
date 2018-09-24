package co.banano.natriumwallet.model;

public enum NotificationOption {
    ON("ON"),
    OFF("OFF");


    private String name;

    NotificationOption(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
