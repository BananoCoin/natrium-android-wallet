package co.banano.natriumwallet.ui.intro;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import co.banano.natriumwallet.bus.Logout;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentIntroNewWalletBinding;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.FragmentUtility;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.util.ExceptionHandler;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * The Intro Screen to the app
 */

public class IntroNewWalletFragment extends BaseFragment {
    public static String TAG = IntroNewWalletFragment.class.getSimpleName();
    FragmentIntroNewWalletBinding binding;
    @Inject
    Realm realm;
    @Inject
    AccountService accountService;
    private String seed;
    private Runnable mRunnable;
    private Handler mHandler;
    private boolean showAllSteps;

    /**
     * Create new instance of the fragment (handy pattern if any data needs to be passed to it)
     *
     * @return IntroNewWalletFragment instance
     */
    public static IntroNewWalletFragment newInstance(boolean showAllSteps) {
        Bundle args = new Bundle();
        args.putBoolean("showSteps", showAllSteps);
        IntroNewWalletFragment fragment = new IntroNewWalletFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // Disable screen capture
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Get arguments
        showAllSteps = getArguments().getBoolean("showSteps");

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_intro_new_wallet, container, false);
        view = binding.getRoot();

        // subscribe to bus
        RxBus.get().register(this);

        // get seed from storage
        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            seed = credentials.getSeed();
            binding.setSeed(seed);
        } else {
            ExceptionHandler.handle(new Exception("Problem accessing generated seed"));
        }

        // bind data to view
        binding.setHandlers(new ClickHandlers());

        accountService.open();

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

        // Set runnable to reset seed text
        mHandler = new Handler();
        mRunnable = () -> {
            binding.introNewWalletSeed.setTextColor(getResources().getColor(R.color.ltblue));
            binding.newWalletSeedCopied.setVisibility(View.INVISIBLE);
        };

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
        // Cancel seed copy callback
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        // Screenshots again
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void goToNext() {
        // go to next screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    IntroNewWalletBackupFragment.newInstance(),
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    IntroNewWalletBackupFragment.TAG
            );
        }
    }

    private void goToWelcome() {
        RxBus.get().post(new Logout());

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

    public class ClickHandlers {
        public void onClickBack(View v) {
            goToWelcome();
        }

        public void onClickNext(View v) {
            goToNext();
        }

        public void onClickSeed(View v) {
            if (binding != null && binding.introNewWalletSeed != null) {
                // copy seed to clipboard
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(ClipboardAlarmReceiver.CLIPBOARD_NAME, seed);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }

                binding.introNewWalletSeed.setTextColor(getResources().getColor(R.color.green_light));
                binding.newWalletSeedCopied.setVisibility(View.VISIBLE);

                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 1500);
                }


                setClearClipboardAlarm();
            }
        }
    }
}
