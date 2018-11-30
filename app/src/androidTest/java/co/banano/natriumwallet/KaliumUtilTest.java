package co.banano.natriumwallet;

import androidx.test.runner.AndroidJUnit4;

import com.rotilho.jnano.commons.NanoHelper;

import co.banano.natriumwallet.network.model.request.block.OpenBlock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import timber.log.Timber;

import static org.junit.Assert.assertEquals;

/**
 * Test Utility Functions
 */
@RunWith(AndroidJUnit4.class)
public class KaliumUtilTest {
    private String seed;
    private String privateKey;
    private String publicKey;
    private String address;

    public KaliumUtilTest() {
    }

    @Before
    public void setUp() throws Exception {
        seed = "387151e6ea2a42eead77f26b1c0fc4c485df4e78902ada848ff97fc5dce85e81";
        privateKey = "C5469190B25E850CED298E57723258F716A4E1956AC2BC60DA023300476D1212";
        publicKey = "9D473FD0CAD0D43DD79B9FDCAC6FED51EDE7E78279A84142290487CF864B8B8F";
        address = "ban_39c99zaeon8n9qdsq9ywojqytnhfwzmr6yfaa734k369sy56q4whb1iu45sg";
    }

    @Test
    public void seedToPrivate() throws Exception {
        long lStartTime = System.nanoTime();

        String privateKey = KaliumUtil.seedToPrivate(seed);

        long lEndTime = System.nanoTime();
        long output = lEndTime - lStartTime;
        Timber.d("Seed to Private: " + output / 1000000);

        assertEquals(privateKey, this.privateKey);
    }

    @Test
    public void privateToPublic() throws Exception {
        long lStartTime = System.nanoTime();

        String publicKey = KaliumUtil.privateToPublic(privateKey);

        long lEndTime = System.nanoTime();
        long output = lEndTime - lStartTime;
        Timber.d("Private to Public: " + output / 1000000);

        assertEquals(publicKey, this.publicKey);
    }

    @Test
    public void publicToAddress() throws Exception {
        long lStartTime = System.nanoTime();

        String address = KaliumUtil.publicToAddress(publicKey);

        long lEndTime = System.nanoTime();
        long output = lEndTime - lStartTime;
        Timber.d("Public to Address: " + output / 1000000);

        assertEquals(address, this.address);
    }

    @Test
    public void addressToPublic() throws Exception {
        long lStartTime = System.nanoTime();

        String publicKey = KaliumUtil.addressToPublic(this.address);

        long lEndTime = System.nanoTime();
        long output = lEndTime - lStartTime;
        Timber.d("Public to Address: " + output / 1000000);

        assertEquals(publicKey, this.publicKey);
    }

    @Test
    public void openBlockTest() throws Exception {
        String source = "3CD78EE059E404252669B37E8195C7AD4FC6CAEA5AA2C0A4989CF9AB248B4949";
        String representative = "ban_3crzecs58y9gd1ucqcfcdsh56ywty5ixzqk41oa5d3i1ggm4bd6c9q5u34m3";
        String account = "ban_1148met4bfcfu6dxhyedxjehogu35uxdwyaaqx8nqdn8qs67kj7gs693s3qi";

        OpenBlock openBlock = new OpenBlock(privateKey, source, representative);
        Timber.d("OpenBlock: %s", openBlock.toString());
    }

    @Test
    public void hexStringToByteArray() {
        NanoHelper.toByteArray("fukSkBVmBBwKMmzgH78wl9h07MTWSvBVORsxFvoLPTBoUHKdRyFnbOVBuztny5yzn40DwIFbdeQyjkAOZu3PTgCU5Ulv9oswJhR4kdDp18axXPT3JeCJxA8NO0Ln7JB");
    }

    @After
    public void tearDown() throws Exception {
    }
}
