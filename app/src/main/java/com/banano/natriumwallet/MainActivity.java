package com.banano.natriumwallet;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.banano.natriumwallet.bus.Logout;
import com.banano.natriumwallet.bus.OpenWebView;
import com.banano.natriumwallet.bus.RxBus;
import com.banano.natriumwallet.bus.SeedCreatedWithAnotherWallet;
import com.banano.natriumwallet.di.activity.ActivityComponent;
import com.banano.natriumwallet.di.activity.ActivityModule;
import com.banano.natriumwallet.di.activity.DaggerActivityComponent;
import com.banano.natriumwallet.di.application.ApplicationComponent;
import com.banano.natriumwallet.model.AvailableLanguage;
import com.banano.natriumwallet.model.Contact;
import com.banano.natriumwallet.model.Credentials;
import com.banano.natriumwallet.model.KaliumWallet;
import com.banano.natriumwallet.network.AccountService;
import com.banano.natriumwallet.ui.common.ActivityWithComponent;
import com.banano.natriumwallet.ui.common.FragmentUtility;
import com.banano.natriumwallet.ui.common.WindowControl;
import com.banano.natriumwallet.ui.home.HomeFragment;
import com.banano.natriumwallet.ui.intro.IntroNewWalletFragment;
import com.banano.natriumwallet.ui.intro.IntroWelcomeFragment;
import com.banano.natriumwallet.ui.webview.WebViewDialogFragment;
import com.banano.natriumwallet.util.SharedPreferencesUtil;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements WindowControl, ActivityWithComponent {
    protected ActivityComponent mActivityComponent;

    public static boolean appInForeground = false;

    @Inject
    Realm realm;
    @Inject
    AccountService accountService;
    @Inject
    KaliumWallet nanoWallet;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    private FragmentUtility mFragmentUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appInForeground = true;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // build the activity component
        mActivityComponent = DaggerActivityComponent
                .builder()
                .applicationComponent(KaliumApplication.getApplication(this).getApplicationComponent())
                .activityModule(new ActivityModule(this))
                .build();

        // perform dagger injections
        mActivityComponent.inject(this);

        // subscribe to bus
        RxBus.get().register(this);

        // set unique uuid (per app install)
        if (!sharedPreferencesUtil.hasAppInstallUuid()) {
            sharedPreferencesUtil.setAppInstallUuid(UUID.randomUUID().toString());
        }

        // Set default system locale to shared prefs
        sharedPreferencesUtil.setDefaultLocale(Locale.getDefault());

        // Set default language
        if (sharedPreferencesUtil.getLanguage() != AvailableLanguage.DEFAULT) {
            Locale locale = new Locale(sharedPreferencesUtil.getLanguage().getLocaleString());
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        if (!sharedPreferencesUtil.isDefaultContactAdded()) {
            realm.executeTransaction(realm -> {
                Contact newContact = realm.createObject(Contact.class, "xrb_1hmefcfq35td5f6rkh15hbpr4bkkhyyhmfhm7511jaka811bfp17xhkboyxo");
                newContact.setName("@NatriumDonations");
            });
            sharedPreferencesUtil.setDefaultContactAdded();
        }

        initUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInForeground = false;
        // stop websocket on pause
        if (accountService != null) {
            accountService.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInForeground = true;
        // start websocket on resume
        if (accountService != null && realm != null && !realm.isClosed() && realm.where(Credentials.class).findFirst() != null) {
            accountService.open();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister from bus
        RxBus.get().unregister(this);

        // close realm connection
        if (realm != null) {
            realm.close();
            realm = null;
        }

        // close wallet so app can clean up
        if (nanoWallet != null) {
            nanoWallet.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void initUi() {
        // set main content view
        setContentView(R.layout.activity_main);

        // create fragment utility instance
        mFragmentUtility = new FragmentUtility(getSupportFragmentManager());
        mFragmentUtility.setContainerViewId(R.id.container);

        // get wallet seed if it exists
        Credentials credentials = realm.where(Credentials.class).findFirst();

        if (credentials == null) {
            // if we don't have a wallet, start the intro
            mFragmentUtility.clearStack();
            mFragmentUtility.replace(new IntroWelcomeFragment());
        } else {
            mFragmentUtility.clearStack();
            if (sharedPreferencesUtil.getConfirmedSeedBackedUp()) {
                // go to home screen
                mFragmentUtility.replace(HomeFragment.newInstance());
            } else {
                // go to intro new wallet
                mFragmentUtility.replace(IntroNewWalletFragment.newInstance(true));
            }
        }
    }

    @Subscribe
    public void logOut(Logout logout) {
        // delete user seed data before logging out
        final RealmResults<Credentials> results = realm.where(Credentials.class).findAll();
        realm.executeTransaction(realm1 -> results.deleteAllFromRealm());

        // stop the websocket
        accountService.close();

        // clear wallet
        nanoWallet.clear();

        // null out component
        mActivityComponent = null;

        sharedPreferencesUtil.setConfirmedSeedBackedUp(false);
        sharedPreferencesUtil.setFromNewWallet(false);

        // go to the welcome fragment
        getFragmentUtility().clearStack();
        getFragmentUtility().replace(new IntroWelcomeFragment(), FragmentUtility.Animation.CROSSFADE);
    }

    @Subscribe
    public void openWebView(OpenWebView openWebView) {
        WebViewDialogFragment
                .newInstance(openWebView.getUrl(), openWebView.getTitle() != null ? openWebView.getTitle() : "")
                .show(getFragmentUtility().getFragmentManager(), WebViewDialogFragment.TAG);
    }

    @Subscribe
    public void seedCreatedWithAnotherWallet(SeedCreatedWithAnotherWallet seedCreatedWithAnotherWallet) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                credentials.setSeedIsSecure(true);
            }
        });
    }

    @Override
    public FragmentUtility getFragmentUtility() {
        return mFragmentUtility;
    }


    /**
     * Set the status bar to a particular color
     *
     * @param color color resource id
     */
    @Override
    public void setStatusBarColor(int color) {
        // we can only set it 5.x and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, color));
        }
    }

    @Override
    public ActivityComponent getActivityComponent() {
        if (mActivityComponent == null) {
            // build the activity component
            mActivityComponent = DaggerActivityComponent
                    .builder()
                    .applicationComponent(KaliumApplication.getApplication(this).getApplicationComponent())
                    .activityModule(new ActivityModule(this))
                    .build();
        }
        return mActivityComponent;
    }

    @Override
    public ApplicationComponent getApplicationComponent() {
        return KaliumApplication.getApplication(this).getApplicationComponent();
    }
}
