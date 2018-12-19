package co.banano.natriumwallet.bus;

public class TransferProcessResponse {
    private String account;
    private String hash;
    private String balance;

    public TransferProcessResponse(String account, String hash, String balance) {
        this.account = account.replace("nano_", "xrb_");
        this.hash = hash;
        this.balance = balance;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account.replace("nano_", "xrb_");
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}