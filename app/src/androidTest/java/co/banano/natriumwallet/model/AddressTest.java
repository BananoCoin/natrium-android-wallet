package co.banano.natriumwallet.model;

import androidx.test.runner.AndroidJUnit4;

import co.banano.natriumwallet.di.activity.TestActivityComponent;
import co.banano.natriumwallet.util.SharedPreferencesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * Test the Kalium Address functions
 */


@RunWith(AndroidJUnit4.class)
public class AddressTest {
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    KaliumWallet nanoWallet;
    private TestActivityComponent testActivityComponent;

    public AddressTest() {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testBasicAddress() throws Exception {
        String addressString = "ban_3wm37qz19zhei7nzscjcopbrbnnachs4p1gnwo5oroi3qonw6inwgoeuufdp";
        Address address = new Address(addressString);
        assertEquals("ban_3wm37qz19zhei7nzscjcopbrbnnachs4p1gnwo5oroi3qonw6inwgoeuufdp", address.getAddress());
        assertEquals(null, address.getAmount());
        assertEquals(true, address.isValidAddress());
    }

    @Test
    public void testInvalidAddress() throws Exception {
        String invalidAddressString = "xrb_3wm37qz19zhei7nzscjcopbrbnnachs4p1gnwo5oroi3qonw6inwgoeuufdp";
        Address address = new Address(invalidAddressString);
        assertEquals(false, address.isValidAddress());
    }

    @Test
    public void testAddressParsing() throws Exception {
        String addressString = "ban:ban_3wm37qz19zhei7nzscjcopbrbnnachs4p1gnwo5oroi3qonw6inwgoeuufdp?amount=10&label=Developers%20Fund&message=Donate%20Now";
        Address address = new Address(addressString);
        assertEquals("ban_3wm37qz19zhei7nzscjcopbrbnnachs4p1gnwo5oroi3qonw6inwgoeuufdp", address.getAddress());
        assertEquals(new BigDecimal("10").toString(), address.getAmount());
    }

    @After
    public void tearDown() throws Exception {
    }
}
