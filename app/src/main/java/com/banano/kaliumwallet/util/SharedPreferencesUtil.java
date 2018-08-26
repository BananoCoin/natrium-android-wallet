package com.banano.kaliumwallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;
import java.util.UUID;

import com.banano.kaliumwallet.model.AvailableCurrency;
import com.banano.kaliumwallet.model.AvailableLanguage;
import com.banano.kaliumwallet.model.PreconfiguredRepresentatives;

/**
 * Shared Preferences utility module
 */
public class SharedPreferencesUtil {
    private static final String LOCAL_CURRENCY = "local_currency";
    private static final String DEFAULT_LOCALE = "app_locale_default";
    private static final String LANGUAGE = "app_language";
    private static final String APP_INSTALL_UUID = "app_install_uuid";
    private static final String CONFIRMED_SEED_BACKEDUP = "confirmed_seed_backedup";
    private static final String FROM_NEW_WALLET = "from_new_wallet";
    private static final String CHANGED_REPRESENTATIVE = "user_set_representative";

    private final SharedPreferences mPrefs;

    public SharedPreferencesUtil(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private boolean has(String key) {
        return mPrefs.contains(key);
    }

    private String get(String key, String defValue) {
        return mPrefs.getString(key, defValue);
    }

    private boolean get(String key, boolean defValue) {
        return mPrefs.getBoolean(key, defValue);
    }

    private void set(String key, String value) {
        SharedPreferences.Editor editor = mPrefs.edit();

        if (value != null) {
            editor.putString(key, value);
        } else {
            editor.remove(key);
        }

        editor.apply();
    }

    private void set(String key, boolean value) {
        SharedPreferences.Editor editor = mPrefs.edit();

        editor.putBoolean(key, value);

        editor.apply();
    }

    public boolean hasLocalCurrency() {
        return has(LOCAL_CURRENCY);
    }

    public AvailableCurrency getLocalCurrency() {
        return AvailableCurrency.valueOf(get(LOCAL_CURRENCY, AvailableCurrency.USD.toString()));
    }

    public void setLocalCurrency(AvailableCurrency localCurrency) {
        set(LOCAL_CURRENCY, localCurrency.toString());
    }

    public void clearLocalCurrency() {
        set(LOCAL_CURRENCY, null);
    }

    public void setDefaultLocale(Locale locale) {
        set(DEFAULT_LOCALE, locale.toString());
    }

    public Locale getDefaultLocale() {
        String localeStr = get(DEFAULT_LOCALE, Locale.getDefault().toString()).replace("-", "_");
        String[] args = localeStr.split("_");
        if (args.length > 2) {
            return new Locale(args[0], args[1], args[2]);
        } else if (args.length > 1) {
            return new Locale(args[0], args[1]);
        } else if (args.length == 1) {
            return new Locale(args[0]);
        }
        return Locale.getDefault();
    }

    public boolean hasLanguage() {
        return has(LANGUAGE);
    }

    public AvailableLanguage getLanguage() {
        return AvailableLanguage.valueOf(get(LANGUAGE, AvailableLanguage.DEFAULT.toString()));
    }

    public void setLanguage(AvailableLanguage language) {
        set(LANGUAGE, language.toString());
    }

    public void clearLanguage() {
        set(LANGUAGE, null);
    }

    public boolean hasAppInstallUuid() {
        return has(APP_INSTALL_UUID);
    }

    public String getAppInstallUuid() {
        return get(APP_INSTALL_UUID, UUID.randomUUID().toString());
    }

    public void setAppInstallUuid(String appInstallUuid) {
        set(APP_INSTALL_UUID, appInstallUuid);
    }

    public void setFromNewWallet(Boolean fromNewWallet) {
        set(FROM_NEW_WALLET, fromNewWallet);
    }

    public void clearFromNewWallet() {
        set(FROM_NEW_WALLET, false);
    }

    public boolean hasConfirmedSeedBackedUp() {
        return has(CONFIRMED_SEED_BACKEDUP);
    }

    public Boolean getConfirmedSeedBackedUp() {
        return get(CONFIRMED_SEED_BACKEDUP, false);
    }

    public void setConfirmedSeedBackedUp(Boolean confirmedSeedBackedUp) {
        set(CONFIRMED_SEED_BACKEDUP, confirmedSeedBackedUp);
    }

    public void clearConfirmedSeedBackedUp() {
        set(CONFIRMED_SEED_BACKEDUP, false);
    }

    public boolean hasCustomRepresentative() {
        return has(CHANGED_REPRESENTATIVE);
    }

    public String getCustomRepresentative() {
        return get(CHANGED_REPRESENTATIVE, PreconfiguredRepresentatives.getRepresentative());
    }

    public void setCustomRepresentative(String representative) {
        set(CHANGED_REPRESENTATIVE, representative);
    }

    public void clearAll() {
        clearLocalCurrency();
        clearLanguage();
        clearConfirmedSeedBackedUp();
    }

}
