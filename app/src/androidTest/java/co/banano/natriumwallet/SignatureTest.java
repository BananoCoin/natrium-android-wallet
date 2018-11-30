package co.banano.natriumwallet;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

