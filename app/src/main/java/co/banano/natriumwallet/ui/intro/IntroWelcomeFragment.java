package co.banano.natriumwallet.ui.intro;

import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.appcompat.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.databinding.FragmentIntroWelcomeBinding;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.FragmentUtility;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.util.SharedPreferencesUtil;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * The Intro Screen to the app
 */

public class IntroWelcomeFragment extends BaseFragment {
    public static String TAG = IntroWelcomeFragment.class.getSimpleName();
    @Inject
    Realm realm;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    private FragmentIntroWelcomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_intro_welcome, container, false);
        view = binding.getRoot();

        binding.welcomeAnimation.setScale(1.1f);

        // bind data to view
        binding.setHandlers(new ClickHandlers());

        // Set drawable left (programatically for compat)
        Drawable startPlusDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_plus_icon);
        binding.introWelcomeButtonNewWallet.setCompoundDrawablesRelativeWithIntrinsicBounds(startPlusDrawable, null, null, null);
        binding.introWelcomeButtonNewWallet.setCompoundDrawablePadding((int) (UIUtil.convertDpToPixel(34, getContext()) * -1));
        Drawable startImportDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_import_wallet);
        binding.introWelcomeButtonHaveWallet.setCompoundDrawablesRelativeWithIntrinsicBounds(startImportDrawable, null, null, null);
        binding.introWelcomeButtonHaveWallet.setCompoundDrawablePadding((int) (UIUtil.convertDpToPixel(34, getContext()) * -1));

        return view;
    }

    public class ClickHandlers {
        public void onClickNewWallet(View view) {
            // go to interstitial
            if (getActivity() instanceof WindowControl) {
                // create wallet seed
                realm.executeTransaction(realm -> {
                    Credentials credentials = realm.createObject(Credentials.class);
                    credentials.setSeed(KaliumUtil.generateSeed());
                });

                sharedPreferencesUtil.setFromNewWallet(true);
                ((WindowControl) getActivity()).getFragmentUtility().replace(
                        IntroNewWalletFragment.newInstance(true),
                        FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                        FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                        IntroNewWalletFragment.TAG
                );
            }
        }

        public void onClickHaveWallet(View view) {
            // let user input their existing wallet
            if (getActivity() instanceof WindowControl) {
                ((WindowControl) getActivity()).getFragmentUtility().add(
                        new IntroSeedFragment(),
                        FragmentUtility.Animation.CROSSFADE,
                        FragmentUtility.Animation.CROSSFADE,
                        IntroSeedFragment.TAG
                );
            }
        }
    }

}
