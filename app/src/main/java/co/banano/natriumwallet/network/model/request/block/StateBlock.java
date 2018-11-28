package co.banano.natriumwallet.network.model.request.block;

import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.network.model.BlockTypes;
import co.banano.natriumwallet.util.NumberUtil;
import com.google.gson.annotations.SerializedName;

/**
 * Send BlockItem
 */
public class StateBlock extends Block {
    @SerializedName("type")
    private String type;

    @SerializedName("previous")
    private String previous;

    @SerializedName("account")
    private String account;

    @SerializedName("representative")
    private String representative;

    @SerializedName("balance")
    private String balance;

    @SerializedName("link")
    private String link;

    @SerializedName("signature")
    private String signature;

    private transient String sendAmount;
    private transient String privateKey;
    private transient String publicKey;

    public StateBlock() {
        this.type = BlockTypes.STATE.toString();
    }

    public StateBlock(BlockTypes blockType, String private_key, String previous,
                      String representative,
                      String balance, String link) {
        this.privateKey = private_key;
        this.publicKey = KaliumUtil.privateToPublic(private_key);
        Address linkAddress = new Address(link);
        link = linkAddress.isValidAddress() ? KaliumUtil.addressToPublic(linkAddress.getAddress()) : link;

        this.setInternal_block_type(blockType);
        this.type = BlockTypes.STATE.toString();
        this.previous = previous;
        this.account = KaliumUtil.publicToAddress(publicKey);
        this.representative = representative;
        if (blockType == BlockTypes.SEND || blockType == BlockTypes.RECEIVE) {
            this.sendAmount = balance;
        } else {
            this.balance = balance;
        }
        this.link = link;

        if (this.balance != null) {
            sign();
        }
    }

    private void sign() {
        String hash = KaliumUtil.computeStateHash(
                account,
                previous,
                representative,
                this.balance,
                link);
        this.signature = KaliumUtil.sign(privateKey, hash);
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
        sign();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String getWork() {
        return work;
    }

    @Override
    public void setWork(String work) {
        this.work = work;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(String sendAmount) {
        this.sendAmount = sendAmount;
    }
}
