package com.banano.kaliumwallet.ui.contact;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.databinding.FragmentContactAddBinding;
import com.banano.kaliumwallet.model.Address;
import com.banano.kaliumwallet.model.Contact;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.SwipeDismissTouchListener;
import com.banano.kaliumwallet.ui.common.UIUtil;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmQuery;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Dialog for creating new contacts
 */
public class AddContactDialogFragment extends BaseDialogFragment {
    private FragmentContactAddBinding binding;
    public static final String TAG = AddContactDialogFragment.class.getSimpleName();
    private boolean showPaste = false;
    private boolean contactAdded = false;


    @Inject
    Realm realm;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return AddContactDialogFragment instance
     */
    public static AddContactDialogFragment newInstance(String address) {
        Bundle args = new Bundle();
        args.putString("c_address", address);
        AddContactDialogFragment fragment = new AddContactDialogFragment();
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
        showPaste = false;
        contactAdded = false;
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        String address = getArguments().getString("c_address", null);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_add, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

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
                binding.addContactContainer.requestFocus();
                hideKeyboard(view);
                if (contactAdded) {
                    dismiss();
                }
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Pre-fill address and handle text changes
        if (address != null) {
            showPaste = false;
            binding.contactAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address, getContext()));
            hidePaste();
        }
        // Set color on contact address  when valid
        binding.contactAddress.addTextChangedListener(new TextWatcher() {
            String lastText = "";
            boolean isColorized = false;
            boolean fromColorization = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {  }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (contactAdded) {
                    return;
                }
                String curText = binding.contactAddress.getText().toString().trim();
                if (curText.equals(lastText)) {
                    if (fromColorization) {
                        fromColorization = false;
                    } else {
                        Address address = new Address(curText);
                        if (address.isValidAddress()) {
                            hideAddressError();
                            isColorized = true;
                            fromColorization = true;
                            binding.contactAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address.getAddress(),  getContext()));
                            binding.contactAddress.setSelection(address.getAddress().length());
                        }
                    }
                    return;
                } else if (curText.length() > 0 && lastText.length() == 0) {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/overpass_mono_light.ttf");
                    binding.contactAddress.setTypeface(tf);
                    if (showPaste) {
                        binding.contactAddress.setPadding(binding.contactAddress.getPaddingLeft(), binding.contactAddress.getPaddingTop(), (int)UIUtil.convertDpToPixel(55, getContext()), binding.contactAddress.getPaddingBottom());
                    }
                } else if (curText.length() == 0 && lastText.length() > 0) {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/nunitosans_extralight.ttf");
                    binding.contactAddress.setTypeface(tf);
                    if (showPaste) {
                        binding.contactAddress.setPadding(binding.contactAddress.getPaddingLeft(), binding.contactAddress.getPaddingTop(), (int)UIUtil.convertDpToPixel(20, getContext()), binding.contactAddress.getPaddingBottom());
                    }
                }
                if (!curText.equals(lastText)) {
                    lastText = curText;
                    Address address = new Address(curText);
                    if (address.isValidAddress()) {
                        hideAddressError();
                        isColorized = true;
                        fromColorization = true;
                        binding.contactAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address.getAddress(),  getContext()));
                        binding.contactAddress.setSelection(address.getAddress().length());
                    } else {
                        if (isColorized) {
                            fromColorization = false;
                            binding.contactAddress.setText(new SpannableString(curText));
                            binding.contactAddress.setSelection(curText.length());
                            isColorized = false;
                        }
                    }
                }
            }
        });
        // prepend @ to contact name
        binding.contactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String name = editable.toString();
                if (!name.startsWith("@") && !name.isEmpty()) {
                    binding.contactName.setText("@" + name);
                    binding.contactName.setSelection(binding.contactName.getText().length());
                }
                hideNameError();
            }
        });

        // Hide keyboard in amount field when return is pushed
        binding.contactName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.contactAddress.setRawInputType(InputType.TYPE_CLASS_TEXT);

        // Remove hint when focused
        binding.contactName.setOnFocusChangeListener((View view, boolean isFocused) -> {
            if (isFocused) {
                binding.contactName.setHint("");
            } else {
                binding.contactName.setHint(R.string.contact_name_hint);
            }
        });
        binding.contactAddress.setOnFocusChangeListener((View view, boolean isFocused) -> {
            if (isFocused) {
                binding.contactAddress.setHint("");
            } else {
                binding.contactAddress.setHint(R.string.contact_address_hint);
            }
        });

        return view;
    }

    private void hidePaste() {
        binding.contactAddress.setPadding((int)UIUtil.convertDpToPixel(40, getContext()), binding.contactAddress.getPaddingTop(), (int)UIUtil.convertDpToPixel(40, getContext()), binding.contactAddress.getPaddingBottom());
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/overpass_mono_light.ttf");
        binding.contactAddress.setTypeface(tf);
        binding.contactAddressPaste.setVisibility(View.GONE);
        binding.contactAddress.setEnabled(false);
    }

    private void showAddressError(int str_id) {
        binding.contactAddressValidation.setVisibility(View.VISIBLE);
        binding.contactAddressValidation.setText(getString(str_id));
    }

    private void hideAddressError() {
        binding.contactAddressValidation.setVisibility(View.INVISIBLE);
    }

    private boolean validateAddress() {
        // check for valid address
        if (binding.contactAddress.getText().toString().trim().isEmpty()) {
            showAddressError(R.string.send_enter_address);
            return false;
        }
        Address address = new Address(binding.contactAddress.getText().toString());
        if (!address.isValidAddress()) {
            showAddressError(R.string.send_invalid_address);
            return false;
        }
        RealmQuery realmQuery = realm.where(Contact.class);
        realmQuery.equalTo("address", address.getAddress());
        if (realmQuery.count() > 0) {
            showAddressError(R.string.contact_address_exists);
        }
        hideAddressError();
        return true;
    }

    private void showNameError(int str_id) {
        binding.contactNameValidation.setVisibility(View.VISIBLE);
        binding.contactNameValidation.setText(getString(str_id));
    }

    private void hideNameError() {
        binding.contactNameValidation.setVisibility(View.INVISIBLE);
    }

    private boolean validateName() {
        String name = binding.contactName.getText().toString().trim();
        if (!name.startsWith("@")) {
            name = "@" + name;
        }
        if (name.isEmpty()) {
            showNameError(R.string.contact_name_missing);
            return false;
        } else {
            RealmQuery realmQuery = realm.where(Contact.class);
            realmQuery.equalTo("name", name);
            if (realmQuery.count() > 0) {
                showNameError(R.string.contact_name_exists);
                return false;
            }
        }
        hideNameError();
        return true;
    }

    private boolean validateRequest() {
        boolean nameValid = validateName();
        boolean addressValid = validateAddress();
        if (!nameValid || !addressValid) {
            return false;
        } else {
            hideNameError();
            hideAddressError();
            return true;
        }
    }

    private void contactAdded() {
        contactAdded = true;
        binding.contactAddClose.setVisibility(View.GONE);
        binding.contactAddHeader.setText(R.string.contact_added_header);
        binding.contactAddHeader.setTextColor(getResources().getColor(R.color.green_light));
        binding.contactName.setTextColor(getResources().getColor(R.color.green_light));
        binding.contactAddress.setText(UIUtil.getColorizedSpannableGreen(binding.contactAddress.getText().toString(), getContext()));
        binding.addContactBtnOverlay.setVisibility(View.GONE);
        binding.addContactBtn.setText(R.string.contact_added);
        binding.addContactBtn.setTextColor(getResources().getColor(R.color.green_dark));
        binding.addContactBtn.setBackground(getResources().getDrawable(R.drawable.bg_green_button_normal));
        binding.addContactClose.setTextColor(getResources().getColor(R.color.green_light));
        binding.addContactClose.setBackground(getResources().getDrawable(R.drawable.bg_green_button_outline));
    }

    public class ClickHandlers {
        public void onClickClose(View v) {
            dismiss();
        }

        public void onClickPaste(View v) {
            // get a valid address from clipboard
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                Address address = new Address(clipboard.getPrimaryClip().getItemAt(0).getText().toString());
                binding.contactAddress.setText(address.getAddress());
            }
        }

        public void onClickAdd(View v) {
            if (!validateRequest() || contactAdded) {
                return;
            }
            realm.executeTransaction(realm -> {
                Contact newContact = realm.createObject(Contact.class);
                newContact.setAddress(binding.contactAddress.getText().toString());
                newContact.setName(binding.contactName.getText().toString());
            });
            contactAdded();
        }
    }
}
