package co.banano.natriumwallet.network.model.request.block;

import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.network.model.BlockTypes;
import com.google.gson.annotations.SerializedName;

/**
 * Subscribe to websocket server for updates regarding the specified account.
 * First action to take when connecting when app opens or reconnects, IF a wallet already exists
 */
public class OpenBlock extends Block {
    @SerializedName("type")
    private String type;

    @SerializedName("source")
    private String source;

    @SerializedName("representative")
    private String representative;

    @SerializedName("account")
    private String account;

    @SerializedName("signature")
    private String signature;

    public OpenBlock() {
        this.type = BlockTypes.OPEN.toString();
    }

    public OpenBlock(String private_key, String source,
                     String representative) {
        this.type = BlockTypes.OPEN.toString();
        this.representative = representative;
        this.account = KaliumUtil.publicToAddress(KaliumUtil.privateToPublic(private_key));
        this.source = source;
        String hash = KaliumUtil.computeOpenHash(source, KaliumUtil.addressToPublic(representative), KaliumUtil.privateToPublic(private_key));
        this.signature = KaliumUtil.sign(private_key, hash);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "OpenBlock{" +
                "type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", representative='" + representative + '\'' +
                ", account='" + account + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
