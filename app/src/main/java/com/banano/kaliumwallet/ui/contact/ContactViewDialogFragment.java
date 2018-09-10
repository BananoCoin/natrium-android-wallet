package com.banano.kaliumwallet.ui.contact;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.ContactRemoved;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.databinding.FragmentContactViewBinding;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.SwipeDismissTouchListener;
import com.banano.kaliumwallet.ui.common.UIUtil;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContactViewDialogFragment extends BaseDialogFragment {
    private FragmentContactViewBinding binding;
    public static final String TAG = ContactViewDialogFragment.class.getSimpleName();
    private boolean removeClicked = false;

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
        removeClicked = false;
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

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    private void showConfirmation() {
        removeClicked = true;
        binding.contactViewHeader.setText(R.string.contact_remove_header);
        binding.viewContactBtn.setText(getString(R.string.dialog_confirm).toUpperCase());
        binding.viewContactClose.setText(getString(R.string.dialog_cancel).toUpperCase());
    }

    private void hideConfirmation() {
        removeClicked = false;
        binding.contactViewHeader.setText(R.string.contact_view_header);
        binding.viewContactBtn.setText(R.string.contact_remove_btn);
        binding.viewContactClose.setText(R.string.dialog_close);
    }

    public class ClickHandlers {
        public void onClickClose(View v) {
            if (!removeClicked) {
                dismiss();
            } else {
                removeClicked = false;
                hideConfirmation();
            }
        }

        public void onClickRemove(View v) {
            if (!removeClicked) {
                showConfirmation();
            } else {
                realm.executeTransaction(realm -> {
                    RealmResults<Contact> contact = realm.where(Contact.class).equalTo("name", binding.contactName.getText().toString()).findAll();
                    contact.deleteAllFromRealm();
                });
                RxBus.get().post(new ContactRemoved(binding.contactName.getText().toString(), binding.contactAddress.getText().toString()));
                dismiss();
            }
        }
    }
}
