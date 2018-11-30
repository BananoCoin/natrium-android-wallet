package co.banano.natriumwallet.model;

import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.runner.AndroidJUnit4;
import android.util.Log;

import co.banano.natriumwallet.KaliumApplication;
import co.banano.natriumwallet.di.activity.ActivityModule;
import co.banano.natriumwallet.di.activity.DaggerTestActivityComponent;
import co.banano.natriumwallet.di.activity.TestActivityComponent;
import co.banano.natriumwallet.util.SharedPreferencesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import javax.inject.Inject;


@RunWith(AndroidJUnit4.class)
public class KaliumWalletTest {
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    KaliumWallet kaliumWallet;
    private TestActivityComponent testActivityComponent;

    public KaliumWalletTest() {
    }

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        // build the activity component
        testActivityComponent = DaggerTestActivityComponent
                .builder()
                .applicationComponent(KaliumApplication.getApplication(InstrumentationRegistry.getTargetContext().getApplicationContext()).getApplicationComponent())
                .activityModule(new ActivityModule(InstrumentationRegistry.getTargetContext()))
                .build();

        testActivityComponent.inject(this);
    }

    @Test
    @UiThreadTest
    public void setLocalCurrencyAmount() throws Exception {
        testActivityComponent.inject(kaliumWallet);
        kaliumWallet.setLocalCurrencyPrice(new BigDecimal("11.0402274899"));
        for (AvailableCurrency currency : AvailableCurrency.values()) {
            // set each potential currency
            sharedPreferencesUtil.setLocalCurrency(currency);
            kaliumWallet.setAccountBalance(new BigDecimal("123414233000000000000000000000000000000"));
            kaliumWallet.setSendNanoAmount(kaliumWallet.getAccountBalanceBanano());
            Log.d("KaliumWalletTest", currency.getLocale().toString() + " " + kaliumWallet.getSendNanoAmountFormatted() + " " + kaliumWallet.getSendLocalCurrencyAmountFormatted());
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}
