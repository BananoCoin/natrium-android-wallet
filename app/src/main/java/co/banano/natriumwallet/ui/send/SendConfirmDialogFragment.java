package co.banano.natriumwallet.ui.send;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.CreatePin;
import co.banano.natriumwallet.bus.PinComplete;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.bus.SocketError;
import co.banano.natriumwallet.databinding.FragmentSendConfirmBinding;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.AuthMethod;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.network.model.response.ErrorResponse;
import co.banano.natriumwallet.network.model.response.ProcessResponse;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.util.NumberUtil;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.github.ajalt.reprint.core.AuthenticationFailureReason;
import com.github.ajalt.reprint.core.Reprint;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * Send confirm screen
 */
public class SendConfirmDialogFragment extends BaseDialogFragment {
    public static String TAG = SendConfirmDialogFragment.class.getSimpleName();
    @Inject
    KaliumWallet wallet;
    @Inject
    AccountService accountService;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    Realm realm;
    private FragmentSendConfirmBinding binding;
    private Address address;
    private AlertDialog fingerprintDialog;
    private Activity mActivity;
    private Fragment mTargetFragment;
    private int retryCount = 0;
    private boolean maxSend = false;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return SendConfirmDialogFragment instance
     */
    public static SendConfirmDialogFragment newInstance(String destination, String amount, boolean localCurrency) {
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putString("amount", amount);
        args.putBoolean("useLocalCurrency", localCurrency);
        SendConfirmDialogFragment fragment = new SendConfirmDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_Modal);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // init dependency injection
        retryCount = 0;
        mActivity = getActivity();
        mTargetFragment = getTargetFragment();
        if (mActivity instanceof ActivityWithComponent) {
            ((ActivityWithComponent) mActivity).getActivityComponent().inject(this);
        }

        String destination = getArguments().getString("destination");
        float sendAmountF =  Float.parseFloat(getArguments().getString("amount"));
        String amount;
        if (sendAmountF != 0) {
            maxSend = false;
            amount = String.format(Locale.ENGLISH, "%.6f", sendAmountF);
        } else {
            maxSend = true;
            amount = wallet.getAccountBalanceBananoNoComma();
        }
        boolean useLocalCurrency = getArguments().getBoolean("useLocalCurrency", false);

        // subscribe to bus
        RxBus.get().register(this);

        // get address
        Contact contact = null;
        if (destination.startsWith("@")) {
            contact = realm.where(Contact.class).equalTo("name", destination).findFirst();
            if (contact == null) {
                if (mTargetFragment != null) {
                    mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_FAILED, mActivity.getIntent());
                }
                dismiss();
            }
        }
        if (contact != null) {
            address = new Address(contact.getAddress());
        } else {
            address = new Address(destination);
        }

        // Set send amount
        wallet.setSendNanoAmount(amount);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_send_confirm, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // Restrict height
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, UIUtil.getDialogHeight(false, getContext()));
        window.setGravity(Gravity.BOTTOM);

        // colorize address text
        if (binding != null &&
                binding.sendDestination != null &&
                address != null &&
                address.getAddress() != null) {
            if (contact != null) {
                String prependString = contact.getName() + "\n";
                binding.sendDestination.setText(UIUtil.getColorizedSpannableBrightPrepend(prependString, address.getAddress(), getContext()));
            } else {
                binding.sendDestination.setText(UIUtil.getColorizedSpannableBright(address.getAddress(), getContext()));
            }
        }

        if (!useLocalCurrency) {
            binding.sendAmount.setText(String.format("%s NANO", amount));
        } else {
            binding.sendAmount.setText(String.format("%s NANO (%s)", amount, wallet.getLocalCurrencyAmount()));
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        if (fingerprintDialog != null) {
            fingerprintDialog.dismiss();
        }
        RxBus.get().unregister(this);
    }

    private void showLoadingOverlay() {
        if (binding != null && binding.progressOverlay != null) {
            binding.sendCancel.setEnabled(false);
            binding.sendConfirm.setEnabled(false);
            animateView(binding.progressOverlay, View.VISIBLE, 1, 200);
        }
    }

    private void hideLoadingOverlay() {
        if (binding != null && binding.progressOverlay != null) {
            animateView(binding.progressOverlay, View.GONE, 0, 200);
        }
    }

    private void showFingerprintDialog(View view) {
        int style = android.os.Build.VERSION.SDK_INT >= 21 ? R.style.AlertDialogCustom : android.R.style.Theme_Holo_Dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), style);
        builder.setMessage(getString(R.string.send_fingerprint_description,
                !wallet.getSendNanoAmountFormatted().isEmpty() ? wallet.getSendNanoAmountFormatted() : "0"));
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
            if (getContext() != null) {
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.error));
            }
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_error, 0, 0, 0);
        }
    }

    private void executeSend() {
        retryCount++;
        showLoadingOverlay();
        BigInteger sendAmount;
        if (maxSend) {
            sendAmount = new BigInteger("0");
        } else {
            sendAmount = NumberUtil.getAmountAsRawBigInteger(wallet.getSendNanoAmount());
        }

        accountService.requestSend(wallet.getFrontierBlock(), address, sendAmount);
    }

    /**
     * Catch errors from the service
     *
     * @param errorResponse Error Response event
     */
    @Subscribe
    public void receiveServiceError(ErrorResponse errorResponse) {
        hideLoadingOverlay();
        if (mTargetFragment != null) {
            mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_FAILED, mActivity.getIntent());
        }
        dismiss();
    }

    /**
     * Catch service error
     *
     * @param socketError Socket error event
     */
    @Subscribe
    public void receiveSocketError(SocketError socketError) {
        if (retryCount > 1) {
            hideLoadingOverlay();
            if (mTargetFragment != null) {
                mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_FAILED, mActivity.getIntent());
            }
            dismiss();
        } else {
            executeSend();
        }
    }

    /**
     * Received a successful send response so go back
     *
     * @param processResponse Process Response
     */
    @Subscribe
    public void receiveProcessResponse(ProcessResponse processResponse) {
        hideLoadingOverlay();
        if (mTargetFragment != null) {
            mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_COMPLETE, mActivity.getIntent());
        }
        dismiss();
        accountService.requestUpdate();
    }

    /**
     * Pin entered correctly
     *
     * @param pinComplete PinComplete object
     */
    @Subscribe
    public void receivePinComplete(PinComplete pinComplete) {
        executeSend();
    }

    @Subscribe
    public void receiveCreatePin(CreatePin pinComplete) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                credentials.setPin(pinComplete.getPin());
            }
        });
        executeSend();
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            if (mTargetFragment != null) {
                mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_CANCELED, mActivity.getIntent());
            }
            dismiss();
        }

        public void onClickConfirm(View view) {
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
                                    executeSend();
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
                showPinScreen(getString(R.string.send_pin_description, wallet.getSendNanoAmount()));
            } else if (credentials != null && credentials.getPin() == null) {
                showCreatePinScreen();
            }
        }
    }
}
