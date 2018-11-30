package co.banano.natriumwallet.ui.pin;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrognito.pinlockview.PinLockListener;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.PinChange;
import co.banano.natriumwallet.bus.PinComplete;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentPinBinding;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Settings main screen
 */
public class PinDialogFragment extends BaseDialogFragment {
    private static final int PIN_LENGTH = 4;
    private static final String SUBTITLE_KEY = "PinDialogSubtitleKey";
    public static String TAG = PinDialogFragment.class.getSimpleName();
    @Inject
    Realm realm;
    private FragmentPinBinding binding;
    private String subtitle;
    private PinLockListener pinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            Timber.d("Pin complete: %s", pin);

            Credentials credentials = realm.where(Credentials.class).findFirst();

            if (credentials != null && credentials.getPin() != null && credentials.getPin().equals(pin)) {
                RxBus.get().post(new PinComplete(pin));
                dismiss();
            } else {
                binding.pinTitle.setText(R.string.pin_error);
                binding.pinLockView.resetPinLockView();
            }
        }

        @Override
        public void onEmpty() {
            Timber.d("Pin empty");
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            Timber.d("Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
            RxBus.get().post(new PinChange(pinLength, intermediatePin));
        }
    };

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return ReceiveDialogFragment instance
     */
    public static PinDialogFragment newInstance(String subtitle) {
        Bundle args = new Bundle();
        args.putString(SUBTITLE_KEY, subtitle);
        PinDialogFragment fragment = new PinDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Modal_Window);
        subtitle = getArguments().getString(SUBTITLE_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pin, container, false);
        view = binding.getRoot();

        binding.setHandlers(new ClickHandlers());
        binding.pinLockView.attachIndicatorDots(binding.pinIndicatorDots);
        binding.pinLockView.setPinLockListener(pinLockListener);
        binding.pinLockView.setPinLength(PIN_LENGTH);
        if (subtitle != null) {
            binding.pinSubtitle.setText(subtitle);
        } else {
            binding.pinSubtitle.setText((""));
        }

        // set the listener for Navigation
        Toolbar toolbar = view.findViewById(R.id.dialog_appbar);
        if (toolbar != null) {
            final PinDialogFragment window = this;
            toolbar.setNavigationOnClickListener(v1 -> window.dismiss());
        }

        return view;
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }
    }
}
