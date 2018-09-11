package com.banano.kaliumwallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;
import java.util.UUID;

import com.banano.kaliumwallet.model.AuthMethod;
import com.banano.kaliumwallet.model.AvailableCurrency;
import com.banano.kaliumwallet.model.AvailableLanguage;
import com.banano.kaliumwallet.model.PreconfiguredRepresentatives;
import com.banano.kaliumwallet.model.PriceConversion;
import com.github.ajalt.reprint.core.Reprint;

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
    private static final String AUTH_METHOD = "auth_method";
    private static final String PRICE_CONVERSION = "price_conversion";

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

    public AvailableCurrency getLocalCurrency() {
        return AvailableCurrency.valueOf(get(LOCAL_CURRENCY, AvailableCurrency.USD.toString()));
    }

    public void setLocalCurrency(AvailableCurrency localCurrency) {
        set(LOCAL_CURRENCY, localCurrency.toString());
    }

    public void setDefaultLocale(Locale locale) {
        set(DEFAULT_LOCALE, locale.toString());
    }

    public Locale getDefaultLocale() {
        String localeStr = get(DEFAULT_LOCALE, Locale.getDefault().toString()).replace("-", "_");
        return LocaleUtil.getLocaleFromStr(localeStr);
    }

    public AvailableLanguage getLanguage() {
        return AvailableLanguage.valueOf(get(LANGUAGE, AvailableLanguage.DEFAULT.toString()));
    }

    public void setLanguage(AvailableLanguage language) {
        set(LANGUAGE, language.toString());
    }

    public boolean hasAppInstallUuid() {
        return has(APP_INSTALL_UUID);
    }

    public void setAppInstallUuid(String appInstallUuid) {
        set(APP_INSTALL_UUID, appInstallUuid);
    }

    public void setFromNewWallet(Boolean fromNewWallet) {
        set(FROM_NEW_WALLET, fromNewWallet);
    }

    public Boolean getConfirmedSeedBackedUp() {
        return get(CONFIRMED_SEED_BACKEDUP, false);
    }

    public void setConfirmedSeedBackedUp(Boolean confirmedSeedBackedUp) {
        set(CONFIRMED_SEED_BACKEDUP, confirmedSeedBackedUp);
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

    public AuthMethod getAuthMethod() {
        if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered()) {
            return AuthMethod.valueOf(get(AUTH_METHOD, AuthMethod.FINGERPRINT.toString()));
        } else {
            return AuthMethod.PIN;
        }
    }

    public void setAuthMethod(AuthMethod method) {
        set(AUTH_METHOD, method.toString());
    }

    public PriceConversion getPriceConversion() {
        return PriceConversion.valueOf(get(PRICE_CONVERSION, PriceConversion.BTC.toString()));
    }

    public void setPriceConversion(PriceConversion conversion) {
        set(PRICE_CONVERSION, conversion.toString());
    }
}
