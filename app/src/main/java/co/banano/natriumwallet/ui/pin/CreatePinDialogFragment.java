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
import co.banano.natriumwallet.bus.CreatePin;
import co.banano.natriumwallet.bus.PinChange;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentCreatePinBinding;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;

import timber.log.Timber;

/**
 * Settings main screen
 */
public class CreatePinDialogFragment extends BaseDialogFragment {
    private static final int PIN_LENGTH = 4;
    public static String TAG = CreatePinDialogFragment.class.getSimpleName();
    private FragmentCreatePinBinding binding;
    private String firstPin = null;
    private PinLockListener pinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            if (firstPin == null) {
                Timber.d("Create first pin complete: %s", pin);

                firstPin = pin;
                binding.pinTitle.setText(R.string.pin_confirm_title);
                binding.pinLockView.resetPinLockView();
            } else {
                if (pin.equals(firstPin)) {
                    Timber.d("Create second pin complete: %s", pin);

                    // Pins match
                    RxBus.get().post(new CreatePin(pin));
                    dismiss();
                } else {
                    Timber.d("Pins don't match: %s", pin);

                    binding.pinTitle.setText(R.string.pin_confirm_error);
                    firstPin = null;
                    binding.pinLockView.resetPinLockView();
                }
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
    public static CreatePinDialogFragment newInstance() {
        Bundle args = new Bundle();
        CreatePinDialogFragment fragment = new CreatePinDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Modal_Window);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }
        firstPin = null;

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_create_pin, container, false);
        view = binding.getRoot();

        binding.setHandlers(new ClickHandlers());
        binding.pinLockView.attachIndicatorDots(binding.pinIndicatorDots);
        binding.pinLockView.setPinLockListener(pinLockListener);
        binding.pinLockView.setPinLength(PIN_LENGTH);
        binding.pinSubtitle.setText((""));

        // set the listener for Navigation
        Toolbar toolbar = view.findViewById(R.id.dialog_appbar);
        if (toolbar != null) {
            final CreatePinDialogFragment window = this;
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
