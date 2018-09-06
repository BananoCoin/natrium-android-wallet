package com.banano.kaliumwallet.ui.home;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import com.banano.kaliumwallet.databinding.FragmentTransactionDetailsBinding;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.SwipeDismissTouchListener;
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.ui.common.WindowControl;
import com.banano.kaliumwallet.ui.contact.AddContactDialogFragment;
import com.banano.kaliumwallet.ui.webview.WebViewDialogFragment;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * Tran Details Dialog
 */
public class TranDetailsFragment extends BaseDialogFragment {
    private FragmentTransactionDetailsBinding binding;
    public static String TAG = TranDetailsFragment.class.getSimpleName();
    private Runnable mRunnable;
    private Handler mHandler;
    private String mAddress;
    private String mBlockHash;
    private boolean copyRunning = false;
    private boolean isContact = false;

    @Inject
    Realm realm;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return ReceiveDialogFragment instance
     */
    public static TranDetailsFragment newInstance(String blockHash, String address) {
        Bundle args = new Bundle();
        args.putString("blockHash", blockHash);
        args.putString("address", address);
        TranDetailsFragment fragment = new TranDetailsFragment();
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
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
        }
        isContact = false;
        copyRunning = false;

        // Get args
        mAddress = getArguments().getString("address");
        mBlockHash = getArguments().getString("blockHash");

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_transaction_details, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // Anchor to bottom
        Window window = getDialog().getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, (int)UIUtil.convertDpToPixel(240, getContext()));
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
            public void onTap(View view) { }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Set runnable to reset address copied text, contact added text
        mHandler = new Handler();
        mRunnable = () -> {
            copyRunning = false;
            binding.tranDetailsCopy.setBackground(getResources().getDrawable(R.drawable.bg_solid_button_normal));
            binding.tranDetailsCopy.setTextColor(getResources().getColor(R.color.gray));
            binding.tranDetailsCopy.setText(getString(R.string.receive_copy_cta));
            if (!isContact) {
                binding.addContactBtn.setVisibility(View.VISIBLE);
            }
        };

        // Show add contact if applicable
        RealmQuery realmQuery = realm.where(Contact.class);
        realmQuery.equalTo("address", mAddress);
        if (realmQuery.count() > 0) {
            isContact = true;
            binding.addContactBtn.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHandler != null  && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }


    public class ClickHandlers {
        public void onClickCopy(View view) {
            if (!copyRunning) {
                copyRunning = true;
                // copy address to clipboard
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(ClipboardAlarmReceiver.CLIPBOARD_NAME, mAddress);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }

                binding.tranDetailsCopy.setBackground(getResources().getDrawable(R.drawable.bg_green_button_normal));
                binding.tranDetailsCopy.setTextColor(getResources().getColor(R.color.green_dark));
                binding.tranDetailsCopy.setText(getString(R.string.receive_copied));
                if (!isContact) {
                    binding.addContactBtn.setVisibility(View.GONE);
                }

                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 900);
                }
            }
        }

        public void onClickDetails(View view) {
            if (getActivity() instanceof WindowControl) {
                // show webview dialog
                WebViewDialogFragment dialog = WebViewDialogFragment.newInstance(getString(R.string.home_explore_url, mBlockHash), "");
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        WebViewDialogFragment.TAG);

                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
            }
        }

        public void onClickAddContact(View view) {
            if (getActivity() instanceof WindowControl) {
                // show receive dialog
                AddContactDialogFragment dialog = AddContactDialogFragment.newInstance(mAddress);
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        AddContactDialogFragment.TAG);
                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
                dismiss();
            }
        }

        public void onClickClose(View view) {
            dismiss();
        }
    }
}
