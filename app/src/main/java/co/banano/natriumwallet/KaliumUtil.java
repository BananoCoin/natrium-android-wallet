package co.banano.natriumwallet;

/*
  Utilities for crypto functions
 */

import com.rotilho.jnano.commons.NanoAccounts;
import com.rotilho.jnano.commons.NanoAmount;
import com.rotilho.jnano.commons.NanoBaseAccountType;
import com.rotilho.jnano.commons.NanoBlocks;
import com.rotilho.jnano.commons.NanoHelper;
import com.rotilho.jnano.commons.NanoKeys;
import com.rotilho.jnano.commons.NanoSignatures;

import java.security.SecureRandom;

import co.banano.natriumwallet.util.SecureRandomUtil;

public class KaliumUtil {
    public final static String addressCodeArray = "13456789abcdefghijkmnopqrstuwxyz";
    public final static char[] addressCodeCharArray = addressCodeArray.toCharArray();
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Generate a new Wallet Seed
     *
     * @return Wallet Seed
     */
    public static String generateSeed() {
        int numchars = 64;
        SecureRandom random = SecureRandomUtil.secureRandom();
        byte[] randomBytes = new byte[numchars / 2];
        random.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(numchars);
        for (byte b : randomBytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Convert a wallet seed to private key
     *
     * @param seed Wallet seed
     * @return private key
     */
    public static String seedToPrivate(String seed) {
        return NanoHelper.toHex(NanoKeys.createPrivateKey(NanoHelper.toByteArray(seed), 0));
    }

    /**
     * Convert a private key to a public key
     *
     * @param privateKey private key
     * @return public key
     */
    public static String privateToPublic(String privateKey) {
        return NanoHelper.toHex(NanoKeys.createPublicKey(NanoHelper.toByteArray(privateKey)));
    }

    /**
     * Compute hash to use to generate an open work block
     *
     * @param source         Source address
     * @param representative Representative address
     * @param account        Account address
     * @return Open Hash
     */
    public static String computeOpenHash(String source, String representative, String account) {
        return NanoBlocks.hashOpenBlock(source, representative, account);
    }

    /**
     * Compute hash to use to generate a receive work block
     *
     * @param previous Previous transation
     * @param source   Source address
     * @return String of hash
     */
    public static String computeReceiveHash(String previous, String source) {
        return NanoBlocks.hashReceiveBlock(previous, source);
    }

    /**
     * Compute hash to use to generate a send work block
     *
     * @param previous    Previous transation
     * @param destination Destination address
     * @param balance     Raw NANO balance
     * @return String of hash
     */
    public static String computeSendHash(String previous, String destination, String balance) {
        return NanoBlocks.hashSendBlock(previous, destination, NanoAmount.ofRaw(balance));
    }

    /**
     * Compute hash for a universal (state) block
     *
     * @param account        This account's ban_ address.
     * @param previous       Previous head block on account; 0 if open block.
     * @param representative Representative ban_ address.
     * @param balance        Resulting balance
     * @param link           Multipurpose Field
     * @return String of hash
     */
    public static String computeStateHash(String account,
                                          String previous,
                                          String representative,
                                          String balance,
                                          String link) {
        return NanoBlocks.hashStateBlock(account, previous, representative, NanoAmount.ofRaw(balance), link);
    }

    /**
     * Compute hash to use to generate a change work block
     *
     * @param previous       Previous transaction
     * @param representative Representative address
     * @return String of hash
     */
    public static String computeChangeHash(String previous, String representative) {
        return NanoBlocks.hashChangeBlock(previous, representative);
    }

    /**
     * Sign a message with a private key
     *
     * @param private_key Private Key
     * @param data        Message
     * @return Signed message
     */
    public static String sign(String private_key, String data) {
        return NanoSignatures.sign(NanoHelper.toByteArray(private_key), data);
    }

    /**
     * Convert a Public Key to an Address
     *
     * @param publicKey Public Key
     * @return ban address
     */
    public static String publicToAddress(String publicKey) {
        return NanoAccounts.createAccount(NanoBaseAccountType.NANO, NanoHelper.toByteArray(publicKey));

    }

    /**
     * Convert an address to a public key
     *
     * @param encodedAddress encoded Address
     * @return Public Key
     */
    public static String addressToPublic(String encodedAddress) {
        return NanoHelper.toHex(NanoAccounts.toPublicKey(encodedAddress));
    }
}
