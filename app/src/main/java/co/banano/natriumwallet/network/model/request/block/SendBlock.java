package co.banano.natriumwallet.network.model.request.block;

import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.network.model.BlockTypes;
import co.banano.natriumwallet.util.NumberUtil;
import com.google.gson.annotations.SerializedName;

/**
 * Send BlockItem
 */
public class SendBlock extends Block {
    @SerializedName("type")
    private String type;

    @SerializedName("previous")
    private String previous;

    @SerializedName("destination")
    private String destination;

    @SerializedName("balance")
    private String balance;

    @SerializedName("signature")
    private String signature;

    public SendBlock() {
        this.type = BlockTypes.SEND.toString();
    }

    public SendBlock(String private_key, String previous, String destination, String balance) {
        this.type = BlockTypes.SEND.toString();
        this.previous = previous;
        this.destination = destination;
        this.balance = NumberUtil.getRawAsHex(balance);
        String hash = KaliumUtil.computeSendHash(previous, KaliumUtil.addressToPublic(destination), this.balance);
        this.signature = KaliumUtil.sign(private_key, hash);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
