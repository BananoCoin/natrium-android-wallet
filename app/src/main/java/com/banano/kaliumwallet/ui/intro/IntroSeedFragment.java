package com.banano.kaliumwallet.ui.intro;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.CreatePin;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.databinding.FragmentIntroSeedBinding;
import com.banano.kaliumwallet.model.Credentials;
import com.banano.kaliumwallet.network.AccountService;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseFragment;
import com.banano.kaliumwallet.ui.common.FragmentUtility;
import com.banano.kaliumwallet.ui.common.KeyboardUtil;
import com.banano.kaliumwallet.ui.common.WindowControl;
import com.banano.kaliumwallet.ui.home.HomeFragment;
import com.banano.kaliumwallet.util.ExceptionHandler;
import com.banano.kaliumwallet.util.SharedPreferencesUtil;
import com.hwangjr.rxbus.annotation.Subscribe;

import javax.inject.Inject;

import io.realm.Realm;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * The Intro Screen to the app
 */

public class IntroSeedFragment extends BaseFragment {
    public static String TAG = IntroSeedFragment.class.getSimpleName();
    @Inject
    Realm realm;
    @Inject
    AccountService accountService;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    private FragmentIntroSeedBinding binding;
    private boolean nextTriggered = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        nextTriggered = false;
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // Disable screen capture
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_intro_seed, container, false);
        view = binding.getRoot();

        // subscribe to bus
        RxBus.get().register(this);

        // bind data to view
        binding.setHandlers(new ClickHandlers());

        // Override back button press
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                goToWelcome();
                return true;
            }
            return false;
        });

        // Keyboard stuff
        binding.introImportContainer.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            KeyboardUtil.hideKeyboard(getActivity());
            return false;
        });

        // Hide keyboard in seed field when return is pushed
        binding.introImportSeed.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.introImportSeed.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Colorize seed when correct
        binding.introImportSeed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String curText = charSequence.toString().trim();
                if (isValidSeed(curText)) {
                    binding.introImportSeed.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    binding.introImportSeed.setTextColor(getResources().getColor(R.color.white_60));
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
        // Screenshots again
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Subscribe
    public void receiveCreatePin(CreatePin createPin) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                credentials.setPin(createPin.getPin());
            }
        });
        goToHomeScreen();
    }

    private boolean isValidSeed(String seed) {
        if (Credentials.isValidSeed(seed)) {
            return true;
        }
        return false;
    }

    private void goToWelcome() {
        // go to welcome screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    new IntroWelcomeFragment(),
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    IntroWelcomeFragment.TAG
            );
        }
    }

    private void goToHomeScreen() {
        // go to home screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().clearStack();
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    HomeFragment.newInstance(),
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    HomeFragment.TAG
            );
        }
    }

    private void createAndStoreCredentials(String seed) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.createObject(Credentials.class);
            credentials.setSeed(seed);
        });
    }

    public class ClickHandlers {
        public void onClickPaste(View view) {
            // copy address to clipboard
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                binding.introImportSeed.setText(clipboard.getPrimaryClip().getItemAt(0).getText().toString());
            }
        }

        public void onClickBack(View view) {
            goToWelcome();
        }

        public void onClickNext(View view) {
            if (!isValidSeed(binding.introImportSeed.getText().toString().trim())) {
                binding.introSeedInvalid.setVisibility(View.VISIBLE);
                return;
            } else {
                binding.introSeedInvalid.setVisibility(View.INVISIBLE);
            }

            if (!nextTriggered) {
                nextTriggered = true;
            } else {
                return;
            }

            createAndStoreCredentials(binding.introImportSeed.getText().toString().trim());
            accountService.open();

            sharedPreferencesUtil.setConfirmedSeedBackedUp(true);

            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                if (credentials.getPin() == null) {
                    showCreatePinScreen();
                } else {
                    goToHomeScreen();
                }
            } else {
                ExceptionHandler.handle(new Exception("Problem accessing generated seed"));
            }
        }
    }
}
