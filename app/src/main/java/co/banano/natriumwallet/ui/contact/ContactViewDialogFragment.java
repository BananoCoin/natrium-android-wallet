package co.banano.natriumwallet.ui.contact;

import android.app.AlertDialog;
import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import co.banano.natriumwallet.bus.ContactRemoved;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentContactViewBinding;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.ui.send.SendDialogFragment;

import javax.inject.Inject;

import co.banano.natriumwallet.ui.webview.WebViewDialogFragment;
import io.realm.Realm;
import io.realm.RealmResults;

public class ContactViewDialogFragment extends BaseDialogFragment {
    private FragmentContactViewBinding binding;
    public static final String TAG = ContactViewDialogFragment.class.getSimpleName();
    private Handler mHandler;
    private Runnable mRunnable;

    @Inject
    Realm realm;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return ContactViewDialogFragment instance
     */
    public static ContactViewDialogFragment newInstance(String name, String address) {
        Bundle args = new Bundle();
        args.putString("c_name", name);
        args.putString("c_address", address);
        ContactViewDialogFragment fragment = new ContactViewDialogFragment();
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

        String name = getArguments().getString("c_name", "");
        String address = getArguments().getString("c_address", "");

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_view, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // subscribe to bus
        RxBus.get().register(this);

        // Restrict height
        Window window = getDialog().getWindow();
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
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Fill data
        binding.contactName.setText(name);
        binding.contactAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address, getContext()));

        // Get contact
        Contact c = realm.where(Contact.class).equalTo("address", address).findFirst();
        if (c == null) {
            return view;
        }

        // Reset address copy text
        // Set runnable to reset seed text
        mHandler = new Handler();
        mRunnable = () -> {
            binding.contactAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address, getContext()));
            binding.contactAddressCopied.setVisibility(View.INVISIBLE);
        };

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
        // Cancel address copy callback
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    public class ClickHandlers {
        public void onClickClose(View v) {
            dismiss();
        }

        public void onClickRemove(View v) {
            int style = android.os.Build.VERSION.SDK_INT >= 21 ? R.style.AlertDialogCustom : android.R.style.Theme_Holo_Dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), style);
            SpannableString title = new SpannableString(getString(R.string.contact_remove_btn));
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            SpannableString positive = new SpannableString(getString(R.string.intro_new_wallet_backup_yes));
            positive.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, positive.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            SpannableString negative = new SpannableString(getString(R.string.intro_new_wallet_backup_no));
            negative.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ltblue)), 0, negative.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setTitle(title)
                    .setMessage(getString(R.string.contact_remove_sure, binding.contactName.getText().toString()))
                    .setPositiveButton(positive, (dialog, which) -> {
                        realm.executeTransaction(realm -> {
                            RealmResults<Contact> contact = realm.where(Contact.class).equalTo("name", binding.contactName.getText().toString()).findAll();
                            contact.deleteAllFromRealm();
                        });
                        RxBus.get().post(new ContactRemoved(binding.contactName.getText().toString(), binding.contactAddress.getText().toString()));
                        dismiss();
                    })
                    .setNegativeButton(negative, (dialog, which) -> {
                        // do nothing which dismisses the dialog
                    })
                    .show();
        }

        public void onClickAddress(View v) {
            if (binding != null && binding.contactAddress != null) {
                // copy seed to clipboard
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(ClipboardAlarmReceiver.CLIPBOARD_NAME, binding.contactAddress.getText().toString());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }

                binding.contactAddress.setText(binding.contactAddress.getText().toString());
                binding.contactAddress.setTextColor(getResources().getColor(R.color.green_light));
                binding.contactAddressCopied.setVisibility(View.VISIBLE);

                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 1500);
                }
            }
        }

        public void onClickSend(View view) {
            if (getActivity() instanceof WindowControl) {
                // show send dialog
                SendDialogFragment dialog = SendDialogFragment.newInstance(binding.contactName.getText().toString());
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        SendDialogFragment.TAG);

                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
                dismiss();
            }
        }

        public void onClickSearch(View v) {
            if (getActivity() instanceof WindowControl) {
                // show webview dialog
                WebViewDialogFragment dialog = WebViewDialogFragment.newInstance(getString(R.string.account_explore_url, binding.contactAddress.getText()), "");
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        WebViewDialogFragment.TAG);
                ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
            }
        }
    }
}
