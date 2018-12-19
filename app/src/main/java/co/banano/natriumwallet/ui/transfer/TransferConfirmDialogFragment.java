package co.banano.natriumwallet.ui.transfer;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.hwangjr.rxbus.annotation.Subscribe;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.bus.TransferHistoryResponse;
import co.banano.natriumwallet.bus.TransferProcessResponse;
import co.banano.natriumwallet.databinding.FragmentTransferConfirmBinding;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.network.model.response.AccountBalanceItem;
import co.banano.natriumwallet.network.model.response.AccountHistoryResponse;
import co.banano.natriumwallet.network.model.response.PendingTransactionResponse;
import co.banano.natriumwallet.network.model.response.PendingTransactionResponseItem;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.util.NumberUtil;
import io.realm.Realm;
import timber.log.Timber;

/**
 * Transfer confirmation screen - the majority of the work happens here
 *
 * Process:
 *  1) Take the input HashMap, which contains address->info mapping.
 *     For accounts with no pending funds, move these to a second internal HashMap for "readyToSend"
 *  2) For remaining accounts (with pending balances), the following algorithm is applied:
 *      a) Request account_history, this gives us the frontier of the account or an empty string
 *         if the account has no blocks (is not open)
 *      b) Request pending, to get a list of all pending blocks this account has
 *      c) If closed, request open. When process response received, create additional receive
 *         blocks as necessary in a synchronized fashion. Updating the frontier along the way.
 *      d) When no pending blocks remain (or we've reached the limit we're willing to process),
 *         then move this address->info map into the "readyToSend" map. And process the next
 *         pending account
 *  3) For the "readyToSend" accounts, create a send for the entire balance of each. One at a time.
 */
public class TransferConfirmDialogFragment extends BaseDialogFragment {
    public static String TAG = TransferConfirmDialogFragment.class.getSimpleName();

    private FragmentTransferConfirmBinding binding;
    private BigInteger totalTransfered = new BigInteger("0");
    private boolean finished = false;

    @Inject
    AccountService accountService;
    @Inject
    Realm realm;
    @Inject
    KaliumWallet wallet;

    // Stores accounts with pending blocks
    HashMap<String, AccountBalanceItem> rawInMap = new HashMap<>();
    // Stores accounts with no more pending blocks
    HashMap<String, AccountBalanceItem> readyToSendMap = new HashMap<>();
    // Need to be received by our own account
    PendingTransactionResponse accountPending;

    private Runnable mRunnable;
    private Handler mHandler;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of ChangeRepDialogFragment
     */
    public static TransferConfirmDialogFragment newInstance(HashMap<String, AccountBalanceItem> privKeyMap) {
        Bundle args = new Bundle();
        args.putSerializable("PRIVKEYMAP", privKeyMap);
        TransferConfirmDialogFragment fragment = new TransferConfirmDialogFragment();
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
                inflater, R.layout.fragment_transfer_confirm, container, false);
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

        // subscribe to bus
        RxBus.get().register(this);

        // Determine how much is here and sum it up
        if (getArguments().getSerializable("PRIVKEYMAP") != null) {
            rawInMap = (HashMap<String, AccountBalanceItem>) getArguments().getSerializable("PRIVKEYMAP");
        } else {
            Timber.d("Input map is null");
            exitWithError();
        }
        BigInteger totalSum = new BigInteger("0");
        for (Map.Entry<String, AccountBalanceItem> item : rawInMap.entrySet()) {
            AccountBalanceItem balances = item.getValue();
            BigInteger balance = new BigInteger(balances.getBalance());
            BigInteger pending = new BigInteger(balances.getPending());
            totalSum = totalSum.add(balance).add(pending);
            // If there's no pending here then we don't need to run a pocket/open routine
            if (pending.equals(BigInteger.ZERO) && balance.compareTo(BigInteger.ZERO) > 0) {
                readyToSendMap.put(item.getKey(), balances);
                rawInMap.remove(item.getKey());
            } else if (pending.equals(BigInteger.ZERO) && balance.equals(BigInteger.ZERO)) {
                rawInMap.remove(item.getKey());
            }
        }
        String totalAsReadable = NumberUtil.getRawAsUsableString(totalSum.toString());

        binding.transferConfirmOne.setText(getString(R.string.transfer_confirm_info_first, totalAsReadable));

        // Lottie hardware acceleration
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.animationView.useHardwareAcceleration(true);
        }

        // Lock callbacks in accountService
        accountService.setLock();

        // Timeout runnable
        mHandler = new Handler();
        mRunnable = () -> {
            startProcessing();
        };


        return view;
    }

    @Override
    public void onDestroyView() {
        accountService.unsetLock();
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    private void showLoadingOverlay() {
        if (binding != null && binding.progressOverlay != null) {
            binding.transferConfirm.setEnabled(false);
            binding.transferCancel.setEnabled(false);
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
            animateView(binding.progressOverlay, View.GONE, 0, 200);
        }
    }

    private void exitWithError() {
        UIUtil.showToast(getString(R.string.transfer_error), getContext());
        dismiss();
    }

    private void showCompleteDialog() {
        TransferCompleteDialogFragment dialog = TransferCompleteDialogFragment.newInstance(NumberUtil.getRawAsUsableString(totalTransfered.toString()));
        dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                TransferCompleteDialogFragment.TAG);
        ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        dismiss();
    }

    /**
     * onAccountHistoryResponse()
     *
     * Callback for account_history. Set the frontier of the account requested, if it's open.
     * Trigger the pending request for this account
     *
     * @param transferHistoryResponse
     */
    @Subscribe
    public void onAccountHistoryResponse(TransferHistoryResponse transferHistoryResponse) {
        if (transferHistoryResponse == null) {
            Timber.d("account_history response is null");
            exitWithError();
            return;
        }
        boolean readyToSend = false;
        String account = transferHistoryResponse.getAccount();
        AccountHistoryResponse accountHistoryResponse = transferHistoryResponse.getAccountHistoryResponse();
        AccountBalanceItem accountBalanceItem = rawInMap.get(account);
        if (accountBalanceItem == null) {
            accountBalanceItem = readyToSendMap.get(account);
            if (accountBalanceItem == null) {
                Timber.d("Couldn't find account %s in rawInMap", account);
                exitWithError();
                return;
            }
            readyToSend = true;
        }
        if (accountHistoryResponse.getHistory().size() > 0) {
            accountBalanceItem.setFrontier(accountHistoryResponse.getHistory().get(0).getHash());
            if (readyToSend) {
                readyToSendMap.put(account, accountBalanceItem);
            } else {
                rawInMap.put(account, accountBalanceItem);
            }
        } else if (readyToSend) {
            Timber.d("This account has no frontier but it should %s", account);
            exitWithError();
            return;
        }
        if (readyToSend) {
            startProcessing();
        } else {
            accountService.requestPending(account);
        }
    }

    /**
     * onPendingResponse()
     *
     * Callback for pending. Begin creating open/receive blocks for this account
     *
     * @param pendingTransactionResponse
     */
    @Subscribe
    public void onPendingResponse(PendingTransactionResponse pendingTransactionResponse) {
        // See if this was our account or a paper wallet account
        if (!pendingTransactionResponse.getAccount().equals(getAddressString())) {
            // Store response for this account
            AccountBalanceItem balanceItem = rawInMap.get(pendingTransactionResponse.getAccount());
            if (balanceItem == null) {
                Timber.d("Couldn't find account in pending response %s", pendingTransactionResponse.getAccount());
                exitWithError();
                return;
            }
            balanceItem.setPendingTransactions(pendingTransactionResponse);
            rawInMap.put(pendingTransactionResponse.getAccount(), balanceItem);

            // Begin open/receive for pendings
            processNextPending(pendingTransactionResponse.getAccount());
        } else {
            // Store result
            accountPending = pendingTransactionResponse;
            processKaliumPending();
        }
    }

    /**
     * onProcessResponse()
     *
     * Callback for process (block). Update frontier for account and move on to next pending block
     * request. If from a send, move on to next account to send to
     *
     * @param processResponse
     */
    @Subscribe
    public void onProcessResponse(TransferProcessResponse processResponse) {
        // If was processed to our own account, behave differently
        if (processResponse.getAccount().equals(getAddressString())) {
            wallet.setFrontierBlock(processResponse.getHash());
            processKaliumPending();
            return;
        }
        // Update balance and frontier of this account
        AccountBalanceItem accountBalanceItem = rawInMap.get(processResponse.getAccount());
        if (accountBalanceItem != null) {
            accountBalanceItem.setFrontier(processResponse.getHash());
            accountBalanceItem.setBalance(processResponse.getBalance());
            rawInMap.put(processResponse.getAccount(), accountBalanceItem);
            // Process next item
            processNextPending(processResponse.getAccount());
        } else {
            accountBalanceItem = readyToSendMap.get(processResponse.getAccount());
            if (accountBalanceItem == null) {
                Timber.d("Couldn't find account in readyToSend map %s", processResponse.getAccount());
                exitWithError();
                return;
            }
            totalTransfered = totalTransfered.add(new BigInteger(accountBalanceItem.getBalance()));
            readyToSendMap.remove(processResponse.getAccount());
            startProcessing();
        }
    }

    /**
     * processNextPending()
     *
     * Take the next pending block for this account and make a process request for an open/receive
     * If there are no more pendings, move the account to "readyToSend" and begin processing next
     * account.
     *
     * @param account
     */
    private void processNextPending(String account) {
        // Get next pending block, if there is one
        AccountBalanceItem accountBalanceItem = rawInMap.get(account);
        PendingTransactionResponse pendingTransactionResponse = accountBalanceItem.getPendingTransactions();
        HashMap<String, PendingTransactionResponseItem> pendingBlocks = pendingTransactionResponse.getBlocks();
        if (pendingBlocks.size() > 0) {
            Map.Entry<String, PendingTransactionResponseItem> entry = pendingBlocks.entrySet().iterator().next();
            PendingTransactionResponseItem pendingTransactionResponseItem = entry.getValue();
            pendingTransactionResponseItem.setHash(entry.getKey());
            if (accountBalanceItem.getFrontier() != null) {
                // Receive block
                accountService.requestReceive(accountBalanceItem.getFrontier(),
                        pendingTransactionResponseItem.getHash(),
                        new BigInteger(pendingTransactionResponseItem.getAmount()),
                        accountBalanceItem.getPrivKey());
            } else {
                // Open account
                accountService.requestOpen("0",
                        pendingTransactionResponseItem.getHash(),
                        new BigInteger(pendingTransactionResponseItem.getAmount()),
                        accountBalanceItem.getPrivKey());
            }
            pendingBlocks.remove(entry.getKey());
        } else {
            readyToSendMap.put(account, accountBalanceItem);
            rawInMap.remove(account);
            startProcessing(); // Move on to next account
        }
    }

    /**
     * Receive pendings for our own account
     */
    private void processKaliumPending() {
        if (accountPending == null) {
            exitWithError();
            return;
        }
        HashMap<String, PendingTransactionResponseItem> pendingBlocks = accountPending.getBlocks();
        if (pendingBlocks.size() > 0) {
            Timber.d("here_1");
            Map.Entry<String, PendingTransactionResponseItem> entry = pendingBlocks.entrySet().iterator().next();
            PendingTransactionResponseItem pendingTransactionResponseItem = entry.getValue();
            pendingTransactionResponseItem.setHash(entry.getKey());
            if (wallet.getOpenBlock() != null) {
                // Receive block
                accountService.requestReceive(wallet.getFrontierBlock(),
                        pendingTransactionResponseItem.getHash(),
                        new BigInteger(pendingTransactionResponseItem.getAmount()),
                        getPrivateKeyString());
            } else {
                // Open account
                accountService.requestOpen("0",
                        pendingTransactionResponseItem.getHash(),
                        new BigInteger(pendingTransactionResponseItem.getAmount()),
                        getPrivateKeyString());
            }
            pendingBlocks.remove(entry.getKey());
        } else {
            Timber.d("here_2");
            startProcessing(); // Finish the process
        }
    }

    /**
     * startProcessing()
     * Make the initial or next account_history request, if there's accounts with pending funds.
     * Otherwise make the initial or next send request, if there's no accounts with pending funds.
     * - This just kicks off the request, the above callbacks will continue the processing
     */
    private void startProcessing() {
        if (rawInMap.size() > 0) {
            Map.Entry<String, AccountBalanceItem> item = rawInMap.entrySet().iterator().next();
            String account = item.getKey();
            // Kick off account_history request
            accountService.requestAccountHistory(account);
        } else if (readyToSendMap.size() > 0) {
            // Start requesting sends
            Map.Entry<String, AccountBalanceItem> item = readyToSendMap.entrySet().iterator().next();
            AccountBalanceItem info = item.getValue();
            // See if we have frontier, if we don't request it
            if (info.getFrontier() == null) {
                accountService.requestAccountHistory(item.getKey());
                return;
            }
            Credentials credentials = realm.where(Credentials.class).findFirst();
            Address destination;
            if (credentials != null) {
                destination = new Address(credentials.getAddressString());
            } else {
                Timber.d("couldn't find address from realm");
                exitWithError();
                return;
            }
            accountService.requestSend(info.getFrontier(), destination, new BigInteger("0"), info.getPrivKey());
        } else if (!finished) {
            finished = true;
            accountService.requestPending(getAddressString());
            mHandler.postDelayed(mRunnable, 10000);
        } else {
            accountService.unsetLock();
            accountService.requestUpdate();
            showCompleteDialog();
        }
    }

    private String getAddressString() {
        Credentials _credentials = realm.where(Credentials.class).findFirst();
        if (_credentials == null) {
            return null;
        } else {
            Credentials credentials = realm.copyFromRealm(_credentials);
            return new Address(credentials.getAddressString()).getAddress().replace("nano_", "xrb_");
        }
    }

    private String getPrivateKeyString() {
        Credentials _credentials = realm.where(Credentials.class).findFirst();
        if (_credentials == null) {
            return null;
        } else {
            Credentials credentials = realm.copyFromRealm(_credentials);
            return credentials.getPrivateKey();
        }
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }

        public void onClickConfirm(View view) {
            showLoadingOverlay();
            startProcessing();
        }
    }
}