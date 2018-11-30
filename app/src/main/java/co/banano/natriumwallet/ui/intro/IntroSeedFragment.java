package co.banano.natriumwallet.ui.intro;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
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

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.CreatePin;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentIntroSeedBinding;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.FragmentUtility;
import co.banano.natriumwallet.ui.common.KeyboardUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.ui.home.HomeFragment;
import co.banano.natriumwallet.ui.pin.CreatePinDialogFragment;
import co.banano.natriumwallet.util.ExceptionHandler;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
                    binding.introImportSeed.setTextColor(getResources().getColor(R.color.ltblue));
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

            // Don't do anything if pin screen is visible
            Fragment createPinFragment = ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().findFragmentByTag(CreatePinDialogFragment.TAG);
            if (createPinFragment != null) {
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
