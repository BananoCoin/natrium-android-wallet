package co.banano.natriumwallet.ui.send;

import androidx.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.databinding.FragmentSendCompleteBinding;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * Send complete screen
 */
public class SendCompleteDialogFragment extends BaseDialogFragment {
    public static String TAG = SendCompleteDialogFragment.class.getSimpleName();
    @Inject
    Realm realm;
    @Inject
    KaliumWallet wallet;
    private FragmentSendCompleteBinding binding;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return SendConfirmDialogFragment instance
     */
    public static SendCompleteDialogFragment newInstance(String destination, String amount, boolean localCurrency) {
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putString("amount", amount);
        args.putBoolean("useLocalCurrency", localCurrency);
        SendCompleteDialogFragment fragment = new SendCompleteDialogFragment();
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

        String destination = getArguments().getString("destination");
        String amount = getArguments().getString("amount");
        boolean useLocalCurrency = getArguments().getBoolean("useLocalCurrency", false);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_send_complete, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // get address
        Contact contact = null;
        Address address;
        if (destination.startsWith("@")) {
            contact = realm.where(Contact.class).equalTo("name", destination).findFirst();
        }
        if (contact != null) {
            address = new Address(contact.getAddress());
        } else {
            address = new Address(destination);
        }

        // Restrict height
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, UIUtil.getDialogHeight(false, getContext()));
        window.setGravity(Gravity.BOTTOM);

        // Shadow
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.60f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(windowParams);

        // Swipe down to dismiss
        getDialog().getWindow().getDecorView().setOnTouchListener(new SwipeDismissTouchListener(getDialog().getWindow().getDecorView(),
                null, new SwipeDismissTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(Object token) {
                return true;
            }

            @Override
            public void onDismiss(View view, Object token) {
                dismiss();
            }

            @Override
            public void onTap(View view) {
                dismiss();
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // colorize address text
        if (binding != null &&
                address.getAddress() != null) {
            if (contact != null) {
                String prependString = contact.getName() + "\n";
                binding.sentDestination.setText(UIUtil.getColorizedSpannableGreenPrepend(prependString, address.getAddress(), getContext()));
            } else {
                binding.sentDestination.setText(UIUtil.getColorizedSpannableGreen(destination, getContext()));
            }
        }

        if (!useLocalCurrency) {
            binding.sentAmount.setText(String.format("%s NANO", amount));
        } else {
            binding.sentAmount.setText(String.format("%s NANO (%s)", amount, wallet.getLocalCurrencyAmount()));
        }

        return view;
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }
    }
}
