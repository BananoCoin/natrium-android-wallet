package co.banano.natriumwallet.model;

import android.net.Uri;

import com.rotilho.jnano.commons.NanoAccounts;
import com.rotilho.jnano.commons.NanoBaseAccountType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Address class
 */

public class Address implements Serializable {
    private String value;
    private String amount;

    public Address() {
    }

    public Address(String value) {
        this.value = parseAddress(value);
    }

    public String getShortString() {
        int frontStartIndex = 0;
        int frontEndIndex = 11;
        int backStartIndex = value.length() - 6;
        return value.substring(frontStartIndex, frontEndIndex) +
                "..." +
                value.substring(backStartIndex, value.length());
    }

    public String getAddress() {
        return value;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isValidAddress() {
        return NanoAccounts.isValid(NanoBaseAccountType.NANO, value);
    }

    private String parseAddress(String addressString) {
        String ret;
        if (addressString != null) {
            addressString = addressString.toLowerCase();
            ret = findAddress(addressString);
            String[] _split = addressString.split(":");
            if (_split.length > 1) {
                String _addressString = _split[1];
                Uri uri = Uri.parse(_addressString);
                if (uri.getQueryParameter("amount") != null && !uri.getQueryParameter("amount").equals("")) {
                    try {
                        this.amount = (new BigDecimal(uri.getQueryParameter("amount"))).toString();
                    } catch (NumberFormatException e) {
                    }
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * findAddress - Finds a ban_ address in a string
     *
     * @param address
     * @return
     */
    public static final String findAddress(String address) {
        Pattern p = Pattern.compile("(xrb|nano)(_)(1|3)[13456789abcdefghijkmnopqrstuwxyz]{59}");
        Matcher matcher = p.matcher(address);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }
}
