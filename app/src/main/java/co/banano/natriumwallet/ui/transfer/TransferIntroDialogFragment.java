package co.banano.natriumwallet.ui.transfer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.hwangjr.rxbus.annotation.Subscribe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentTransferBinding;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.network.model.response.AccountBalanceItem;
import co.banano.natriumwallet.network.model.response.AccountsBalancesResponse;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.scan.ScanActivity;

import static android.app.Activity.RESULT_OK;

/**
 * Initial Transfer Screen
 */
public class TransferIntroDialogFragment extends BaseDialogFragment {
    public static String TAG = TransferIntroDialogFragment.class.getSimpleName();
    private static final int NUM_SWEEP = 15; // Number of accounts to derive/sweep from a seed

    @Inject
    AccountService accountService;
    @Inject
    KaliumWallet wallet;

    private FragmentTransferBinding binding;

    private HashMap<String, AccountBalanceItem> accountPrivkeyMap = new HashMap<>();

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of ChangeRepDialogFragment
     */
    public static TransferIntroDialogFragment newInstance() {
        Bundle args = new Bundle();
        TransferIntroDialogFragment fragment = new TransferIntroDialogFragment();
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
        // inject
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_transfer, container, false);
        view = binding.getRoot();


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
            public void onTap(View v) {
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Set values
        binding.setHandlers(new ClickHandlers());

        binding.transferDescription.setText(getString(R.string.transfer_intro, getString(R.string.send_scan_qr)));

        // subscribe to bus
        RxBus.get().register(this);

        // Lottie hardware acceleration
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.animationView.useHardwareAcceleration(true);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    private void showLoadingOverlay() {
        if (binding != null && binding.progressOverlay != null) {
            binding.transferScanQr.setEnabled(false);
            animateView(binding.progressOverlay, View.VISIBLE, 1, 200);
            // Darken window further
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.90f;
            window.setAttributes(windowParams);
        }
    }

    private void hideLoadingOverlay() {
        if (binding != null && binding.progressOverlay != null) {
            binding.transferScanQr.setEnabled(true);
            animateView(binding.progressOverlay, View.GONE, 0, 200);
            // Lighten window again
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.60f;
            window.setAttributes(windowParams);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCAN_RESULT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle res = data.getExtras();
                if (res != null) {
                    String result = res.getString(ScanActivity.QR_CODE_RESULT);
                    showLoadingOverlay();

                    String privKey;
                    String pubKey;
                    String account;
                    if (KaliumUtil.isValidSeed(result)) {
                        List<String> accountsToRequest = new ArrayList<>();
                        for (int i = 0; i < NUM_SWEEP; i++) {
                            privKey = KaliumUtil.seedToPrivate(result, i);
                            pubKey = KaliumUtil.privateToPublic(privKey);
                            account = KaliumUtil.publicToAddress(pubKey).replace("nano_", "xrb_");
                            // don't let them transfer from their own account
                            if (pubKey.equals(wallet.getPublicKey())) {
                                continue;
                            }
                            accountPrivkeyMap.put(account, new AccountBalanceItem(privKey));
                            accountsToRequest.add(account);
                        }
                        // Also put the seed itself as a private key, in case thats the intention
                        pubKey = KaliumUtil.privateToPublic(result);
                        account =  KaliumUtil.publicToAddress(pubKey).replace("nano_", "xrb_");
                        // don't let them transfer from their own account
                        if (!pubKey.equals(wallet.getPublicKey())) {
                            accountPrivkeyMap.put(account, new AccountBalanceItem(result));
                            accountsToRequest.add(account);
                        }
                        // Make account balances request
                        accountService.requestAccountsBalances(accountsToRequest);
                    }
                }
            }
        }
    }

    private void showConfirmDialog() {
        TransferConfirmDialogFragment dialog = TransferConfirmDialogFragment.newInstance(accountPrivkeyMap);
        dialog.show(getFragmentManager(), TransferConfirmDialogFragment.TAG);
        getFragmentManager().executePendingTransactions();
    }

    @Subscribe
    public void onAccountBalancesResponse(AccountsBalancesResponse accountsBalancesResponse) {
        HashMap<String, AccountBalanceItem> accountBalances = accountsBalancesResponse.getBalances();
        hideLoadingOverlay();
        for (Map.Entry<String, AccountBalanceItem> item : accountBalances.entrySet()) {
            AccountBalanceItem balances = item.getValue();
            String account = item.getKey();
            BigInteger balance = new BigInteger(balances.getBalance());
            BigInteger pending = new BigInteger(balances.getPending());
            if (balance.add(pending).equals(BigInteger.ZERO)) {
                accountPrivkeyMap.remove(account.replace("nano_", "xrb_"));
            } else {
                AccountBalanceItem balanceItem = accountPrivkeyMap.get(account);
                balanceItem.setBalance(balances.getBalance());
                balanceItem.setPending(balances.getPending());
                accountPrivkeyMap.put(account.replace("nano_", "xrb_"), balanceItem);
            }
        }
        if (accountPrivkeyMap.size() == 0) {
            UIUtil.showToast(getString(R.string.transfer_no_funds_toast), getContext());
            return;
        }
        showConfirmDialog();
        dismiss();
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }

        public void onClickScan(View view) {
            startScanActivity(getString(R.string.transfer_qr_scan_hint), true);
        }
    }
}
