package com.banano.kaliumwallet.model;

import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.banano.kaliumwallet.KaliumApplication;
import com.banano.kaliumwallet.di.activity.ActivityModule;
import com.banano.kaliumwallet.di.activity.DaggerTestActivityComponent;
import com.banano.kaliumwallet.di.activity.TestActivityComponent;
import com.banano.kaliumwallet.util.SharedPreferencesUtil;

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
            kaliumWallet.setSendBananoAmount(kaliumWallet.getAccountBalanceBanano());
            Log.d("KaliumWalletTest", currency.getLocale().toString() + " " + kaliumWallet.getSendBananoAmountFormatted() + " " + kaliumWallet.getSendLocalCurrencyAmountFormatted());
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}
