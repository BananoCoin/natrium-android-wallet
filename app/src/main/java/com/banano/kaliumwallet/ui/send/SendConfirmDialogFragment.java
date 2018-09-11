package com.banano.kaliumwallet.ui.send;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.CreatePin;
import com.banano.kaliumwallet.bus.PinComplete;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.bus.SendInvalidAmount;
import com.banano.kaliumwallet.databinding.FragmentSendConfirmBinding;
import com.banano.kaliumwallet.model.Address;
import com.banano.kaliumwallet.model.AuthMethod;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.model.Credentials;
import com.banano.kaliumwallet.model.KaliumWallet;
import com.banano.kaliumwallet.network.AccountService;
import com.banano.kaliumwallet.network.model.response.ErrorResponse;
import com.banano.kaliumwallet.network.model.response.ProcessResponse;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.util.NumberUtil;
import com.banano.kaliumwallet.util.SharedPreferencesUtil;
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

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return SendConfirmDialogFragment instance
     */
    public static SendConfirmDialogFragment newInstance(String destination, String amount) {
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putString("amount", amount);
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
        mActivity = getActivity();
        mTargetFragment = getTargetFragment();
        if (mActivity instanceof ActivityWithComponent) {
            ((ActivityWithComponent) mActivity).getActivityComponent().inject(this);
        }

        String destination = getArguments().getString("destination");
        String amount = String.format(Locale.ENGLISH, "%.2f", Float.parseFloat(getArguments().getString("amount")));

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
        wallet.setSendBananoAmount(amount);

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

        binding.sendAmount.setText(String.format("%s BAN", amount));

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
                !wallet.getSendBananoAmountFormatted().isEmpty() ? wallet.getSendBananoAmountFormatted() : "0"));
        builder.setView(view);
        SpannableString negativeText = new SpannableString(getString(android.R.string.cancel));
        negativeText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.yellow)), 0, negativeText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        showLoadingOverlay();
        BigInteger sendAmount = NumberUtil.getAmountAsRawBigInteger(wallet.getSendBananoAmount());

        accountService.requestSend(wallet.getFrontierBlock(), address, sendAmount);
    }

    /**
     * Event that occurs if an amount entered is invalid
     *
     * @param sendInvalidAmount Send Invalid Amount event
     */
    @Subscribe
    public void receiveInvalidAmount(SendInvalidAmount sendInvalidAmount) {
        hideLoadingOverlay();
        if (mTargetFragment != null) {
            mTargetFragment.onActivityResult(getTargetRequestCode(), SEND_FAILED_AMOUNT, mActivity.getIntent());
        }
        dismiss();
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
                showPinScreen(getString(R.string.send_pin_description, wallet.getSendBananoAmount()));
            } else if (credentials != null && credentials.getPin() == null) {
                showCreatePinScreen();
            }
        }
    }
}
