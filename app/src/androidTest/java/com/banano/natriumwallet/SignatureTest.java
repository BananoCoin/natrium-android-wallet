package com.banano.natriumwallet;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import static org.junit.Assert.assertEquals;

/**
 * Test the block signing
 */


@RunWith(AndroidJUnit4.class)
public class SignatureTest {
    public SignatureTest() {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void derivePublicFromPrivate() throws Exception {
        // random key pair
        Sodium sodium = NaCl.sodium();
        byte[] pk = new byte[Sodium.crypto_sign_publickeybytes()];
        byte[] sk = new byte[Sodium.crypto_sign_secretkeybytes()];
        Sodium.crypto_sign_ed25519_keypair(pk, sk);

        assertEquals(KaliumUtil.bytesToHex(pk), KaliumUtil.privateToPublic(KaliumUtil.bytesToHex(sk)));

        // random key pair
        String priv = "49FF617E9074857402411B346D92174572EB5DE02CC9469C22E9681D8565E6D5";
        String pub = "6C32F3E6ED921D2D98A3573B665FE7F8A35D510186AA9F1B365D283BBAA93DFB";

        assertEquals(pub, KaliumUtil.privateToPublic(priv));
    }

    @Test
    public void signingABlock() throws Exception {
        String yourPrivateKey = "1F7B5B5D966DCF95DD401D504A088B81256C09D1196697A2DBF79BCFA4171E2B";
        String oneOfYourBlocksHashes = "E23C078FA2C60AE5F64FC0C432F21650FEC582A4174D415F2CAAEC2457A36844";
        String theSignatureOnThatBlock = "D95854A073F74B02B8BF35B89098A297BEB0C8EED56AE4BBB0BD60A3E2BBA236734CD57AAD02C1C6769369BA9DB2917A11F42F53537A72AD226B7C386A19BD02";

        assertEquals(theSignatureOnThatBlock, KaliumUtil.sign(yourPrivateKey, oneOfYourBlocksHashes));
    }

    @After
    public void tearDown() throws Exception {

    }
}

