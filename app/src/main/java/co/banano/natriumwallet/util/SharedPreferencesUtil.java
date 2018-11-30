package co.banano.natriumwallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import co.banano.natriumwallet.model.AuthMethod;
import co.banano.natriumwallet.model.AvailableCurrency;
import co.banano.natriumwallet.model.AvailableLanguage;
import co.banano.natriumwallet.model.NotificationOption;
import co.banano.natriumwallet.model.PreconfiguredRepresentatives;
import co.banano.natriumwallet.model.PriceConversion;
import com.github.ajalt.reprint.core.Reprint;

import java.util.Currency;
import java.util.Locale;

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
    private static final String DEFAULT_CONTACT_ADDED = "default_contact_added";
    private static final String FCM_TOKEN = "fcm_token";
    private static final String PUSH_NOTIFICATIONS = "push_notifications";
    private static final String APP_BACKGROUNDED = "app_backgrounded";

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

    private AvailableCurrency getDefaultCurrency() {
        String symbol = Currency.getInstance(getDefaultLocale()).getCurrencyCode();
        for (AvailableCurrency value: AvailableCurrency.values()) {
            if (symbol.equals(value.toString())) {
                return value;
            }
        }
        return AvailableCurrency.USD;
    }

    public AvailableCurrency getLocalCurrency() {
        return AvailableCurrency.valueOf(get(LOCAL_CURRENCY, getDefaultCurrency().toString()));
    }

    public void setLocalCurrency(AvailableCurrency localCurrency) {
       set(LOCAL_CURRENCY, localCurrency.toString());
    }

    public Locale getDefaultLocale() {
        String localeStr = get(DEFAULT_LOCALE, Locale.getDefault().toString()).replace("-", "_");
        return LocaleUtil.getLocaleFromStr(localeStr);
    }

    public void setDefaultLocale(Locale locale) {
        set(DEFAULT_LOCALE, locale.toString());
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

    public NotificationOption getNotificationSetting() {
        return NotificationOption.valueOf(get(PUSH_NOTIFICATIONS, NotificationOption.ON.toString()));
    }

    public void setNotificationSetting(NotificationOption option) {
        set(PUSH_NOTIFICATIONS, option.toString());
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

    public String getFcmToken() {
        return get(FCM_TOKEN, null);
    }

    public void setFcmToken(String fcmToken) {
        set(FCM_TOKEN, fcmToken);
    }

    public boolean isDefaultContactAdded() {
        return get(DEFAULT_CONTACT_ADDED, false);
    }

    public void setDefaultContactAdded() {
        set(DEFAULT_CONTACT_ADDED, true);
    }

    public boolean isBackgrounded() {
        return get(APP_BACKGROUNDED, true);
    }

    public void setAppBackgrounded(boolean isBackgrounded) {
        set(APP_BACKGROUNDED, isBackgrounded);
    }
}
