package com.banano.kaliumwallet.model;

import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.io.Serializable;
import java.math.BigDecimal;

import com.banano.kaliumwallet.KaliumUtil;

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

    private String parseAddress(String address) {
        if (address != null) {
            address = address.toLowerCase();
            String[] _split = address.split(":");
            if (_split.length > 1) {
                String _addressString = _split[1];
                Uri uri = Uri.parse(_addressString);
                if (uri.getPath() != null) {
                    address = uri.getPath();
                }
                if (uri.getQueryParameter("amount") != null && !uri.getQueryParameter("amount").equals("")) {
                    try {
                        this.amount = (new BigDecimal(uri.getQueryParameter("amount"))).toString();
                    } catch (NumberFormatException e) {
                    }
                }
            }
            return cleanAddress(address);
        }
        return null;
    }

    /**
     * cleanAddress - Extracts a ban_ address from a string
     *
     * @param address
     * @return
     */
    private String cleanAddress(String address) {
        if (address.contains("ban_")) {
            address = address.substring(address.lastIndexOf("ban_"));
            if (address.length() >= 64) {
                address = address.substring(0, 64);
            }
        }
        return address;
    }
}
