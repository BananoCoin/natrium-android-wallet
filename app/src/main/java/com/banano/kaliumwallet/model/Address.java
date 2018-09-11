package com.banano.kaliumwallet.model;

import android.net.Uri;

import com.banano.kaliumwallet.KaliumUtil;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

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
        String[] parts = value.split("_");
        if (parts.length != 2) {
            return false;
        }
        if (!parts[0].equals("ban")) {
            return false;
        }
        if (parts[1].length() != 60) {
            return false;
        }
        checkCharacters:
        for (int i = 0; i < parts[1].length(); i++) {
            char letter = parts[1].toLowerCase().charAt(i);
            for (int j = 0; j < KaliumUtil.addressCodeCharArray.length; j++) {
                if (KaliumUtil.addressCodeCharArray[j] == letter) {
                    continue checkCharacters;
                }
            }
            return false;
        }
        byte[] shortBytes = KaliumUtil.hexToBytes(KaliumUtil.decodeAddressCharacters(parts[1]));
        byte[] bytes = new byte[37];
        // Restore leading null bytes
        System.arraycopy(shortBytes, 0, bytes, bytes.length - shortBytes.length, shortBytes.length);
        byte[] checksum = new byte[5];
        byte[] state = new byte[Sodium.crypto_generichash_statebytes()];
        byte[] key = new byte[Sodium.crypto_generichash_keybytes()];
        NaCl.sodium();
        Sodium.crypto_generichash_blake2b_init(state, key, 0, 5);
        Sodium.crypto_generichash_blake2b_update(state, bytes, 32);
        Sodium.crypto_generichash_blake2b_final(state, checksum, checksum.length);
        for (int i = 0; i < checksum.length; i++) {
            if (checksum[i] != bytes[bytes.length - 1 - i]) {
                return false;
            }
        }
        return true;
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
        Pattern p = Pattern.compile("(ban)(_)(1|3)[13456789abcdefghijkmnopqrstuwxyz]{59}");
        Matcher matcher = p.matcher(address);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }
}
