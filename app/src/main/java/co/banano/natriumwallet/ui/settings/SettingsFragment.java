package co.banano.natriumwallet.ui.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import co.banano.natriumwallet.BuildConfig;
import co.banano.natriumwallet.MainActivity;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.CreatePin;
import co.banano.natriumwallet.bus.Logout;
import co.banano.natriumwallet.bus.PinComplete;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentSettingsBinding;
import co.banano.natriumwallet.model.AuthMethod;
import co.banano.natriumwallet.model.AvailableCurrency;
import co.banano.natriumwallet.model.AvailableLanguage;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.NotificationOption;
import co.banano.natriumwallet.model.StringWithTag;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.FragmentUtility;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.ui.contact.ContactOverviewFragment;
import co.banano.natriumwallet.ui.webview.WebViewDialogFragment;
import co.banano.natriumwallet.util.LocaleUtil;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.github.ajalt.reprint.core.AuthenticationFailureReason;
import com.github.ajalt.reprint.core.Reprint;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * Settings main screen
 */
public class SettingsFragment extends BaseFragment {
    public static String TAG = SettingsFragment.class.getSimpleName();
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    Realm realm;
    @Inject
    AccountService accountService;
    private FragmentSettingsBinding binding;
    private AlertDialog fingerprintDialog;
    private boolean backupSeedPinEntered = false;
    private boolean languageInitialized = false;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of SettingsFragment
     */
    public static SettingsFragment newInstance() {
        Bundle args = new Bundle();
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inject
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }
        backupSeedPinEntered = false;

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_settings, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());
        binding.setVersion(getString(R.string.version_display, BuildConfig.VERSION_NAME));

        // subscribe to bus
        RxBus.get().register(this);

        // set up currency spinner
        List<StringWithTag> availableCurrencies = getAllCurrencies();
        ArrayAdapter<StringWithTag> spinnerArrayAdapter = new ArrayAdapter<>(getContext(),
                R.layout.view_spinner_item,
                availableCurrencies
        );
        spinnerArrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);
        binding.settingsCurrencySpinner.setVisibility(View.VISIBLE);
        binding.settingsCurrencySpinner.setAdapter(spinnerArrayAdapter);
        binding.settingsCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // save local currency to shared preferences
                StringWithTag swt = (StringWithTag) adapterView.getItemAtPosition(i);
                AvailableCurrency key = (AvailableCurrency) swt.tag;
                if (key != null) {
                    sharedPreferencesUtil.setLocalCurrency(key);
                    final HashMap<String, String> customData = new HashMap<>();
                    customData.put("currency", key.toString());
                    // update currency amounts
                    accountService.requestSubscribe();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        // set selected item with value saved in shared preferences
        binding.settingsCurrencySpinner.setSelection(getIndexOfCurrency(sharedPreferencesUtil.getLocalCurrency(), availableCurrencies));

        // Setup language spinner
        languageInitialized = false;
        List<StringWithTag> availableLanguages = getAllLanguages();
        ArrayAdapter<StringWithTag> languageAdapter = new ArrayAdapter<>(getContext(),
                R.layout.view_spinner_item,
                availableLanguages
        );
        languageAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);
        binding.settingsLanguageSpinner.setVisibility(View.VISIBLE);
        binding.settingsLanguageSpinner.setAdapter(languageAdapter);
        binding.settingsLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!languageInitialized) {
                    languageInitialized = true;
                    return;
                }
                StringWithTag swt = (StringWithTag) adapterView.getItemAtPosition(i);
                AvailableLanguage key = (AvailableLanguage) swt.tag;
                if (key != null) {
                    sharedPreferencesUtil.setLanguage(key);
                    Locale locale;
                    if (key == AvailableLanguage.DEFAULT) {
                        locale = sharedPreferencesUtil.getDefaultLocale();
                    } else {
                        locale = LocaleUtil.getLocaleFromStr(key.getLocaleString());
                    }
                    Locale.setDefault(locale);
                    Configuration config = new Configuration();
                    config.locale = locale;
                    getContext().getResources().updateConfiguration(config,
                            getContext().getResources().getDisplayMetrics());
                    if (getActivity() != null) {
                        Intent refresh = new Intent(getContext(), MainActivity.class);
                        startActivity(refresh);
                        getActivity().finish();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        if (sharedPreferencesUtil.getLanguage() == AvailableLanguage.DEFAULT) {
            binding.settingsLanguageSpinner.setSelection(0);
        } else {
            binding.settingsLanguageSpinner.setSelection(getIndexOfLanguage(sharedPreferencesUtil.getLanguage(), availableLanguages));
        }

        // Setup fingerprint/pin option. Only display if actually has sensor and a fingerprint registered
        if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered()) {
            binding.settingsAuthBottom.setVisibility(View.VISIBLE);
            binding.settingsAuthContainer.setVisibility(View.VISIBLE);
            binding.icFingerprint.setVisibility(View.VISIBLE);
            // Setup spinner
            List<StringWithTag> authMethods = new ArrayList<>();
            authMethods.add(new StringWithTag(getString(R.string.settings_fingerprint_method), AuthMethod.FINGERPRINT));
            authMethods.add(new StringWithTag(getString(R.string.settings_pin_method), AuthMethod.PIN));
            ArrayAdapter<StringWithTag> authAdapter = new ArrayAdapter<>(getContext(),
                    R.layout.view_spinner_item,
                    authMethods
            );
            authAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);
            binding.settingsAuthSpinner.setVisibility(View.VISIBLE);
            binding.settingsAuthSpinner.setAdapter(authAdapter);
            binding.settingsAuthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    StringWithTag swt = (StringWithTag) adapterView.getItemAtPosition(i);
                    AuthMethod key = (AuthMethod) swt.tag;
                    if (key != null) {
                        sharedPreferencesUtil.setAuthMethod(key);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            int i = 0;
            for (StringWithTag authMethod : authMethods) {
                if (authMethod.tag.equals(sharedPreferencesUtil.getAuthMethod())) {
                    binding.settingsAuthSpinner.setSelection(i);
                    break;
                }
                i++;
            }
        } else {
            binding.settingsAuthBottom.setVisibility(View.GONE);
            binding.settingsAuthContainer.setVisibility(View.GONE);
            binding.icFingerprint.setVisibility(View.GONE);
        }

        // Setup push notification setting
        // Setup spinner
        List<StringWithTag> notificationOptions = new ArrayList<>();
        notificationOptions.add(new StringWithTag(getString(R.string.generic_on), NotificationOption.ON));
        notificationOptions.add(new StringWithTag(getString(R.string.generic_off), NotificationOption.OFF));
        ArrayAdapter<StringWithTag> notifAdapter = new ArrayAdapter<>(getContext(),
                R.layout.view_spinner_item,
                notificationOptions
        );
        notifAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);
        binding.settingsNotificationSpinner.setVisibility(View.VISIBLE);
        binding.settingsNotificationSpinner.setAdapter(notifAdapter);
        binding.settingsNotificationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                StringWithTag swt = (StringWithTag) adapterView.getItemAtPosition(i);
                NotificationOption key = (NotificationOption) swt.tag;
                if (key != null) {
                    sharedPreferencesUtil.setNotificationSetting(key);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        int i = 0;
        for (StringWithTag notificationOption : notificationOptions) {
            if (notificationOption.tag.equals(sharedPreferencesUtil.getNotificationSetting())) {
                binding.settingsNotificationSpinner.setSelection(i);
                break;
            }
            i++;
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    /**
     * Pin entered correctly
     *
     * @param pinComplete PinComplete object
     */
    @Subscribe
    public void receivePinComplete(PinComplete pinComplete) {
        if (backupSeedPinEntered) {
            showBackupSeedDialog();
            backupSeedPinEntered = false;
        }
    }

    @Subscribe
    public void receiveCreatePin(CreatePin pinComplete) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                credentials.setPin(pinComplete.getPin());
            }
        });
        if (backupSeedPinEntered) {
            showBackupSeedDialog();
            backupSeedPinEntered = false;
        }
    }

    /**
     * Get list of all of the available currencies
     *
     * @return Lost of all currencies the app supports
     */
    private List<StringWithTag> getAllCurrencies() {
        List<StringWithTag> itemList = new ArrayList<>();
        for (AvailableCurrency currency : AvailableCurrency.values()) {
            itemList.add(new StringWithTag(currency.getFullDisplayName(), currency));
        }
        return itemList;
    }

    /**
     * Get Index of a particular currency
     *
     * @return Index of a particular currency in the spinner
     */
    private int getIndexOfCurrency(AvailableCurrency currency, List<StringWithTag> availableCurrencies) {
        int i = 0;
        for (StringWithTag availableCurrency : availableCurrencies) {
            if (availableCurrency.tag.equals(currency)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    /**
     * Get list of all of the available languages
     *
     * @return List of all locales app has translations for
     */
    private List<StringWithTag> getAllLanguages() {
        List<StringWithTag> itemList = new ArrayList<>();
        // Add current system language
        itemList.add(new StringWithTag(getString(R.string.settings_default_language_string), AvailableLanguage.DEFAULT));
        for (AvailableLanguage language : AvailableLanguage.values()) {
            if (language != AvailableLanguage.DEFAULT) {
                itemList.add(new StringWithTag(language.getDisplayName(), language));
            }
        }
        return itemList;
    }

    /**
     * Get Index of a particular language
     *
     * @return Index of a particular language in the spinner
     */
    private int getIndexOfLanguage(AvailableLanguage language, List<StringWithTag> availableLanguages) {
        int i = 0;
        for (StringWithTag availableLanguage : availableLanguages) {
            if (availableLanguage.tag.equals(language)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHANGE_RESULT) {
            if (resultCode == CHANGE_COMPLETE) {
                UIUtil.showToast(getString(R.string.change_representative_success), getContext());
            } else if (resultCode == CHANGE_FAILED) {
                UIUtil.showToast(getString(R.string.change_representative_error), getContext());
            }
        }
    }

    private void showChangeRepDialog() {
        // show change rep dialog
        ChangeRepDialogFragment dialog = ChangeRepDialogFragment.newInstance();
        dialog.setTargetFragment(this, CHANGE_RESULT);
        dialog.show(getFragmentManager(), ChangeRepDialogFragment.TAG);
        getFragmentManager().executePendingTransactions();
    }

    private void showBackupSeedDialog() {
        // show backup seed dialog
        BackupSeedDialogFragment dialog = BackupSeedDialogFragment.newInstance();
        dialog.show(getFragmentManager(), BackupSeedDialogFragment.TAG);
        getFragmentManager().executePendingTransactions();
    }

    private void showFingerprintDialog(View view) {
        int style = android.os.Build.VERSION.SDK_INT >= 21 ? R.style.AlertDialogCustom : android.R.style.Theme_Holo_Dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), style);
        builder.setMessage(getString(R.string.settings_fingerprint_title));
        builder.setView(view);
        SpannableString negativeText = new SpannableString(getString(android.R.string.cancel));
        negativeText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, negativeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setNegativeButton(negativeText, (dialog, which) -> Reprint.cancelAuthentication());

        fingerprintDialog = builder.create();
        fingerprintDialog.setCanceledOnTouchOutside(true);
        // display dialog
        fingerprintDialog.show();
    }

    private void showFingerprintError(AuthenticationFailureReason reason, CharSequence message, View view) {
        if (isAdded()) {
            final HashMap<String, String> customData = new HashMap<>();
            customData.put("description", reason.name());
            TextView textView = view.findViewById(R.id.fingerprint_textview);
            textView.setText(message.toString());
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.error));
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_error, 0, 0, 0);
        }
    }

    public class ClickHandlers {
        public void onClickAuthMethod(View view) {
            binding.settingsAuthSpinner.performClick();
        }

        public void onClickCurrency(View view) {
            binding.settingsCurrencySpinner.performClick();
        }

        public void onClickLanguage(View view) {
            binding.settingsLanguageSpinner.performClick();
        }

        public void onClickNotification(View view) {
            binding.settingsNotificationSpinner.performClick();
        }

        public void onClickChange(View view) {
            if (getActivity() instanceof WindowControl) {
                showChangeRepDialog();
            }
        }

        public void onClickBackupSeed(View view) {
            Credentials credentials = realm.where(Credentials.class).findFirst();

            if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered() && sharedPreferencesUtil.getAuthMethod() == AuthMethod.FINGERPRINT) {
                // show fingerprint dialog
                LayoutInflater factory = LayoutInflater.from(getContext());
                @SuppressLint("InflateParams") final View viewFingerprint = factory.inflate(R.layout.view_fingerprint, null);
                showFingerprintDialog(viewFingerprint);
                com.github.ajalt.reprint.rxjava2.RxReprint.authenticate()
                        .subscribe(result -> {
                            switch (result.status) {
                                case SUCCESS:
                                    fingerprintDialog.dismiss();
                                    showBackupSeedDialog();
                                    break;
                                case NONFATAL_FAILURE:
                                    showFingerprintError(result.failureReason, result.errorMessage, viewFingerprint);
                                    break;
                                case FATAL_FAILURE:
                                    showFingerprintError(result.failureReason, result.errorMessage, viewFingerprint);
                                    break;
                            }
                        });
            } else if (credentials != null && credentials.getPin() != null) {
                backupSeedPinEntered = true;
                showPinScreen(getString(R.string.settings_pin_title));
            } else if (credentials != null && credentials.getPin() == null) {
                backupSeedPinEntered = true;
                showCreatePinScreen();
            }
        }

        public void onClickShare(View view) {
            String playStoreUrl = "https://play.google.com/store/apps/details?id=" + getActivity().getPackageName();

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share_extra) + "\n" + playStoreUrl);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_title)));
        }

        public void onClickLogOut(View view) {
            if (getActivity() instanceof WindowControl) {

                // show the logout are-you-sure dialog
                AlertDialog.Builder builder;
                SpannableString title = new SpannableString(getString(R.string.settings_logout_alert_title));
                title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                SpannableString positive = new SpannableString(getString(R.string.settings_logout_alert_confirm_cta));
                positive.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, positive.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                SpannableString negative = new SpannableString(getString(R.string.settings_logout_alert_cancel_cta));
                negative.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, negative.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Sub dialog
                SpannableString warningTitle = new SpannableString(getString(R.string.settings_logout_warning_title));
                warningTitle.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, warningTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                SpannableString yes = new SpannableString(getString(R.string.settings_logout_warning_confirm));
                yes.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, yes.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                int style = android.os.Build.VERSION.SDK_INT >= 21 ? R.style.AlertDialogCustom : android.R.style.Theme_Holo_Dialog;
                builder = new AlertDialog.Builder(getContext(), style);
                builder.setTitle(title)
                        .setMessage(R.string.settings_logout_alert_message)
                        .setPositiveButton(positive, (dialog, which) -> {
                            AlertDialog.Builder builderWarning;
                            builderWarning = new AlertDialog.Builder(getContext(), style);
                            builderWarning.setTitle(warningTitle)
                                    .setMessage(R.string.settings_logout_warning_message)
                                    .setPositiveButton(yes, (dialogWarn, whichWarn) -> {
                                        RxBus.get().post(new Logout());
                                        //dismiss();
                                    })
                                    .setNegativeButton(negative, (dialogWarn, whichWarn) -> {
                                        // do nothing which dismisses the dialog
                                    })
                                    .show();
                        })
                        .setNegativeButton(negative, (dialog, which) -> {
                            // do nothing which dismisses the dialog
                        })
                        .show();
            }
        }

        public void onClickContacts(View view) {
            if (getActivity() instanceof WindowControl) {
                FragmentTransaction ft = ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right);
                ContactOverviewFragment fragment = ContactOverviewFragment.newInstance();
                ft.replace(R.id.settings_frag_container, fragment, ContactOverviewFragment.TAG).addToBackStack(null).commit();
                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
            }
        }

        public void onClickPrivacy(View view) {
            if (getActivity() instanceof WindowControl) {
                // show webview dialog
                WebViewDialogFragment dialog = WebViewDialogFragment.newInstance(getString(R.string.privacy_policy_url), "");
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        WebViewDialogFragment.TAG);

                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
            }
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (FragmentUtility.disableFragmentAnimations) {
            Animation a = new Animation() { };
            a.setDuration(0);
            return a;
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }
}
