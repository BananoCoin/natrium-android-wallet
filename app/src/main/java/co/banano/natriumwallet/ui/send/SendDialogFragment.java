package co.banano.natriumwallet.ui.send;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.ContactSelected;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.databinding.FragmentSendBinding;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.DigitsInputFilter;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.ui.contact.ContactSelectionAdapter;
import co.banano.natriumwallet.ui.scan.ScanActivity;
import co.banano.natriumwallet.util.NumberUtil;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmQuery;

import static android.app.Activity.RESULT_OK;
import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Send main screen
 */
public class SendDialogFragment extends BaseDialogFragment {
    public static String TAG = SendDialogFragment.class.getSimpleName();
    @Inject
    KaliumWallet wallet;
    @Inject
    AccountService accountService;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    Realm realm;
    private FragmentSendBinding binding;
    private Address address;
    private Activity mActivity;
    private ContactSelectionAdapter mAdapter;
    private boolean useLocalCurrency = false;
    private String lastNanoAmount;
    private String lastLocalCurrencyAmount;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return SendDialogFragment instance
     */
    public static SendDialogFragment newInstance(String contactName) {
        Bundle args = new Bundle();
        args.putString("contact_name", contactName);
        SendDialogFragment fragment = new SendDialogFragment();
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
        useLocalCurrency = false;
        mActivity = getActivity();
        if (mActivity instanceof ActivityWithComponent) {
            ((ActivityWithComponent) mActivity).getActivityComponent().inject(this);
        }

        // get data
        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            address = new Address(credentials.getAddressString());
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_send, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // subscribe to bus
        RxBus.get().register(this);

        // Set balance hint
        binding.sendBalance.setText(getString(R.string.send_balance, wallet.getAccountBalanceBanano()));

        // Reset send amount
        wallet.setSendNanoAmount("");
        wallet.setLocalCurrencyAmount("");

        // Restrict height
        Window window = getDialog().getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, UIUtil.getDialogHeight(false, getContext()));
        window.setGravity(Gravity.BOTTOM);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

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
                binding.sendContainer.requestFocus();
                hideKeyboard(view);
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // colorize address text
        if (binding != null &&
                binding.sendFromAddress != null &&
                address != null &&
                address.getAddress() != null) {
            binding.sendFromAddress.setText(UIUtil.getColorizedSpannable(address.getAddress(), getContext()));
        }


        // Set color on destination  when valid
        binding.sendAddress.addTextChangedListener(new TextWatcher() {
            String lastText = "";
            boolean toContact = false;
            boolean isColorized = false;
            boolean fromColorization = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String curText = binding.sendAddress.getText().toString().trim();
                if (curText.length() > 0) {
                    binding.sendAddressContact.setVisibility(View.GONE);
                } else {
                    binding.sendAddressContact.setVisibility(View.VISIBLE);
                }
                if (curText.startsWith("@")) {
                    if (toContact) {
                        toContact = false;
                        updateContactSearch();
                        binding.sendAddress.clearFocus();
                        return;
                    }
                    binding.sendAddress.setGravity(Gravity.START);
                    binding.sendAddress.setBackground(getResources().getDrawable(R.drawable.bg_edittext_bottom_round));
                    updateContactSearch();
                    return;
                } else {
                    if (!fromColorization) {
                        binding.sendAddress.setTextColor(getResources().getColor(R.color.white_60));
                    }
                    binding.contactRecyclerview.setVisibility(View.GONE);
                    binding.sendAddress.setGravity(Gravity.CENTER);
                    binding.sendAddress.setBackground(getResources().getDrawable(R.drawable.bg_edittext));
                }
                if (curText.equals(lastText)) {
                    if (fromColorization) {
                        fromColorization = false;
                    } else {
                        Address address = new Address(curText);
                        if (address.isValidAddress()) {
                            hideAddressError();
                            isColorized = true;
                            fromColorization = true;
                            binding.sendAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address.getAddress(), getContext()));
                            binding.sendAddress.setSelection(address.getAddress().length());
                        }
                    }
                    return;
                } else if (curText.length() > 0 && lastText.length() == 0) {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/overpass_mono_light.ttf");
                    binding.sendAddress.setPadding(binding.sendAddress.getPaddingLeft(), binding.sendAddress.getPaddingTop(), (int) UIUtil.convertDpToPixel(55, getContext()), binding.sendAddress.getPaddingBottom());
                    binding.sendAddress.setTypeface(tf);
                } else if (curText.length() == 0 && lastText.length() > 0) {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/nunitosans_extralight.ttf");
                    binding.sendAddress.setPadding(binding.sendAddress.getPaddingLeft(), binding.sendAddress.getPaddingTop(), (int) UIUtil.convertDpToPixel(20, getContext()), binding.sendAddress.getPaddingBottom());
                    binding.sendAddress.setTypeface(tf);
                }
                if (!curText.equals(lastText)) {
                    lastText = curText;
                    Address address = new Address(curText);
                    if (address.isValidAddress()) {
                        hideAddressError();
                        Contact c = realm.where(Contact.class).equalTo("address", address.getAddress()).findFirst();
                        if (c != null) {
                            lastText = c.getName();
                            toContact = true;
                            binding.sendAddress.setText(c.getName());
                        } else {
                            isColorized = true;
                            fromColorization = true;
                            binding.sendAddress.setText(UIUtil.getColorizedSpannableBrightWhite(address.getAddress(), getContext()));
                            binding.sendAddress.setSelection(address.getAddress().length());
                        }
                    } else {
                        if (isColorized) {
                            fromColorization = false;
                            binding.sendAddress.setText(new SpannableString(curText));
                            binding.sendAddress.setSelection(curText.length());
                            isColorized = false;
                        }
                    }
                }
            }

        });

        // Amount validation
        binding.sendAmount.setFilters(new InputFilter[]{new DigitsInputFilter(Integer.MAX_VALUE, 6, Integer.MAX_VALUE)});
        binding.sendAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable e) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().length() > 0) {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/nunitosans_bold.ttf");
                    binding.sendAmount.setTypeface(tf);
                } else {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/nunitosans_extralight.ttf");
                    binding.sendAmount.setTypeface(tf);
                }
                if (!useLocalCurrency) {
                    wallet.setSendNanoAmount(charSequence.toString().trim());
                    binding.setWallet(wallet);
                } else if (charSequence.length() > 0) {
                    String original = charSequence.toString();
                    String symbol = NumberFormat.getCurrencyInstance(wallet.getLocalCurrency().getLocale()).getCurrency().getSymbol();
                    if (!original.startsWith(symbol)) {
                        binding.sendAmount.removeTextChangedListener(this);
                        binding.sendAmount.setText(symbol + original);
                        binding.sendAmount.setSelection((symbol + original).length());
                        binding.sendAmount.addTextChangedListener(this);
                    } else if (original.trim().equals(symbol)) {
                        binding.sendAmount.removeTextChangedListener(this);
                        binding.sendAmount.setText("");
                        original = "";
                        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "font/nunitosans_extralight.ttf");
                        binding.sendAmount.setTypeface(tf);
                        binding.sendAmount.addTextChangedListener(this);
                    }

                    original = original.replace(symbol, "");
                    if (original.equals(wallet.getAccountBalanceLocalCurrencyNoSymbol())) {
                        String amount = String.format(Locale.ENGLISH, "%.6f", wallet.getUsableAccountBalanceBanano().floatValue());
                        amount = amount.indexOf(".") < 0 ? amount : amount.replaceAll("0*$", "").replaceAll("\\.$", "");
                        wallet.setSendNanoAmount(amount);
                    }
                    wallet.setLocalCurrencyAmount(original);

                }
                hideAmountError();
            }
        });
        binding.sendAmount.setKeyListener(DigitsKeyListener.getInstance("0123456789."));

        // Hide keyboard in amount field when return is pushed
        binding.sendAddress.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.sendAddress.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Remove hint when focused
        binding.sendAmount.setOnFocusChangeListener((View view, boolean isFocused) -> {
            if (isFocused) {
                binding.sendAmount.setHint("");
            } else {
                binding.sendAmount.setHint(R.string.send_amount_hint);
            }
        });
        binding.sendAddress.setOnFocusChangeListener((View view, boolean isFocused) -> {
            if (isFocused) {
                binding.sendAddress.setHint("");
                if (binding.sendAddress.getText().toString().startsWith("@")) {
                    binding.sendAddress.setGravity(Gravity.START);
                }
            } else {
                binding.sendAddress.setGravity(Gravity.CENTER);
                binding.contactRecyclerview.setVisibility(View.GONE);
                binding.sendAddress.setBackground(getResources().getDrawable(R.drawable.bg_edittext));
                binding.sendAddress.setHint(R.string.send_address_hint);
                if (binding.sendAddress.getText().toString().startsWith("@")) {
                    RealmQuery realmQuery = realm.where(Contact.class);
                    realmQuery.equalTo("name", binding.sendAddress.getText().toString());
                    if (realmQuery.count() == 0) {
                        binding.sendAddress.setText("");
                    }
                }
            }
        });

        // Prepare contacts info
        List<Contact> contacts = realm.where(Contact.class).findAll().sort("name");
        binding.contactRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ContactSelectionAdapter(contacts);
        binding.contactRecyclerview.setItemAnimator(null);
        binding.contactRecyclerview.setAdapter(mAdapter);

        // Prefill address if applicable
        String contactName = getArguments().getString("contact_name", null);
        if (contactName != null) {
            binding.sendAddress.requestFocus();
            binding.sendAddress.setText(contactName);
            binding.sendAddress.clearFocus();
        }

        // Show contract trigger button if applicable
        if (binding.sendAddress.getText().length() > 0) {
            binding.sendAddressContact.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    @Subscribe
    public void receiveContactSelected(ContactSelected contactSelected) {
        binding.sendAddress.setText(contactSelected.getName());
        hideKeyboard(binding.sendAddress);
        binding.sendContainer.requestFocus();
    }

    private void updateContactSearch() {
        String searchTerm = binding.sendAddress.getText().toString();
        if (!searchTerm.startsWith("@")) {
            return;
        }
        searchTerm = searchTerm.substring(1, searchTerm.length());
        List<Contact> contacts = realm.where(Contact.class).contains("name", searchTerm, Case.INSENSITIVE).findAll().sort("name");
        mAdapter.updateList(contacts);
        // Colorize name if a valid contact
        if (contacts.size() > 0) {
            binding.contactRecyclerview.setVisibility(View.VISIBLE);
            binding.sendAddress.setBackground(getResources().getDrawable(R.drawable.bg_edittext_bottom_round));
            String name = binding.sendAddress.getText().toString().trim();
            boolean foundMatch = false;
            for (Contact c : contacts) {
                if (c.getName().equals(name)) {
                    binding.sendAddress.setTextColor(getResources().getColor(R.color.ltblue));
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                binding.sendAddress.setTextColor(getResources().getColor(R.color.white_60));
            }
        } else {
            binding.contactRecyclerview.setVisibility(View.GONE);
            binding.sendAddress.setBackground(getResources().getDrawable(R.drawable.bg_edittext));
            binding.sendAddress.setTextColor(getResources().getColor(R.color.white_60));
        }
    }

    private void showAddressError(int str_id) {
        binding.sendAddressValidation.setVisibility(View.VISIBLE);
        binding.sendAddressValidation.setText(getString(str_id));
    }

    private void hideAddressError() {
        binding.sendAddressValidation.setVisibility(View.INVISIBLE);
    }

    private boolean validateAddress() {
        String address = binding.sendAddress.getText().toString().trim();
        if (address.startsWith("@")) {
            Contact c = realm.where(Contact.class).equalTo("name", address).findFirst();
            if (c != null) {
                address = c.getAddress();
            } else {
                showAddressError(R.string.contact_invalid_name);
                return false;
            }
        }
        // check for valid address
        if (address.isEmpty()) {
            showAddressError(R.string.send_enter_address);
            return false;
        }
        Address destination = new Address(address);
        if (!destination.isValidAddress()) {
            showAddressError(R.string.send_invalid_address);
            return false;
        }
        hideAddressError();
        return true;
    }

    private void showAmountError(int str_id) {
        binding.sendAmountValidation.setVisibility(View.VISIBLE);
        binding.sendAmountValidation.setText(getString(str_id));
    }

    private void hideAmountError() {
        binding.sendAmountValidation.setVisibility(View.INVISIBLE);
    }

    private boolean validateAmount() {
        BigInteger sendAmount = NumberUtil.getAmountAsRawBigInteger(wallet.getSendNanoAmount());
        // check that amount being sent is less than or equal to account balance
        if (wallet.getSendNanoAmount().isEmpty()) {
            showAmountError(R.string.send_enter_amount);
            return false;
        } else if (sendAmount.compareTo(new BigInteger("0")) <= -1 || sendAmount.compareTo(new BigInteger("0")) == 0) {
            showAmountError(R.string.send_amount_error);
            return false;
        } else if (sendAmount.compareTo(wallet.getAccountBalanceBananoRaw().toBigInteger()) > 0) {
            showAmountError(R.string.send_insufficient_balance);
            return false;
        }

        // check that we have a frontier block
        if (wallet.getFrontierBlock() == null) {
            showAmountError(R.string.send_no_frontier);
            return false;
        }
        hideAmountError();
        return true;
    }

    private boolean validateRequest() {
        boolean amountValid = validateAmount();
        boolean addressValid = validateAddress();
        if (!amountValid || !addressValid) {
            return false;
        } else {
            hideAmountError();
            hideAddressError();
            return true;
        }
    }

    private void showSendCompleteDialog() {
        // show complete dialog
        SendCompleteDialogFragment dialog = SendCompleteDialogFragment.newInstance(binding.sendAddress.getText().toString(), wallet.getSendNanoAmount(), useLocalCurrency);
        dialog.show(((WindowControl) mActivity).getFragmentUtility().getFragmentManager(),
                SendCompleteDialogFragment.TAG);
        executePendingTransactions();
    }

    private void showSendConfirmDialog() {
        // show send dialog
        SendConfirmDialogFragment dialog = SendConfirmDialogFragment.newInstance(binding.sendAddress.getText().toString(), wallet.getSendNanoAmount(), useLocalCurrency);
        dialog.setTargetFragment(this, SEND_RESULT);
        dialog.show(((WindowControl) mActivity).getFragmentUtility().getFragmentManager(),
                SendConfirmDialogFragment.TAG);
        executePendingTransactions();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SEND_RESULT) {
            if (resultCode == SEND_COMPLETE) {
                showSendCompleteDialog();
                dismiss();
            } else if (resultCode == SEND_FAILED) {
                UIUtil.showToast(getString(R.string.send_generic_error), getContext());
            } else if (resultCode == SEND_FAILED_AMOUNT) {
                wallet.setSendNanoAmount(wallet.getUsableAccountBalanceBanano().toString());
                binding.setWallet(wallet);
                showAmountError(R.string.send_amount_error);
            }
        } else if (requestCode == SCAN_RESULT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle res = data.getExtras();
                if (res != null) {
                    // parse address
                    Address address = new Address(res.getString(ScanActivity.QR_CODE_RESULT));

                    // set to scanned value
                    if (address.getAddress() != null) {
                        binding.sendAddress.setText(address.getAddress());
                    }

                    if (address.getAmount() != null) {
                        wallet.setSendNanoAmount(address.getAmount());
                        binding.setWallet(wallet);
                    }
                }
            }
        }
    }

    /**
     * Handle switching to local currency
     */
    private void switchCurrency() {
        // Keep a cache of previous amounts because, it's kinda nice to see approx what nano is worth
        // this way you can tap button and tap back and not end up with X.9993451 NANO
        if (useLocalCurrency) {
            // Switch back to NANO
            binding.sendAmount.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            if (lastLocalCurrencyAmount != null && lastNanoAmount != null && wallet.getLocalCurrencyAmountNoSymbol().equals(lastLocalCurrencyAmount)) {
                lastLocalCurrencyAmount = wallet.getLocalCurrencyAmountNoSymbol();
                wallet.setSendNanoAmount(lastNanoAmount);
            } else {
                lastLocalCurrencyAmount = wallet.getLocalCurrencyAmountNoSymbol();
            }
            useLocalCurrency = false;
            binding.sendAmount.setText(wallet.getSendNanoAmountFormatted());
            lastNanoAmount = wallet.getSendNanoAmount();
            binding.sendBalance.setText(getString(R.string.send_balance, wallet.getAccountBalanceBanano()));
            binding.sendAmount.setFilters(new InputFilter[]{new DigitsInputFilter(Integer.MAX_VALUE, 6, Integer.MAX_VALUE)});
        } else {
            // Switch to local currency);
            NumberFormat nf = NumberFormat.getCurrencyInstance(wallet.getLocalCurrency().getLocale());
            String allowedChars = "0123456789.";
            if (nf instanceof DecimalFormat) {
                DecimalFormatSymbols sym = ((DecimalFormat)nf).getDecimalFormatSymbols();
                allowedChars = String.format("0123456789%s", sym.getDecimalSeparator());
            }
            binding.sendAmount.setKeyListener(DigitsKeyListener.getInstance(allowedChars));
            if (lastNanoAmount != null && lastLocalCurrencyAmount != null && wallet.getSendNanoAmount().equals(lastNanoAmount)) {
                lastNanoAmount = wallet.getSendNanoAmount();
                wallet.setLocalCurrencyAmount(lastLocalCurrencyAmount);
            } else {
                lastNanoAmount = wallet.getSendNanoAmount();
            }
            useLocalCurrency = true;
            binding.sendAmount.setText(wallet.getLocalCurrencyAmountNoSymbol());
            lastLocalCurrencyAmount = wallet.getLocalCurrencyAmountNoSymbol();
            binding.sendBalance.setText(String.format("(%s)", wallet.getAccountBalanceLocalCurrency()));
            binding.sendAmount.setFilters(new InputFilter[]{new DigitsInputFilter(Integer.MAX_VALUE, 2, Integer.MAX_VALUE)});
        }
        binding.sendAmount.setSelection(binding.sendAmount.getText().length());
    }

    /**
     * Execute all pending transactions
     */
    private void executePendingTransactions() {
        ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }

        public void onClickQR(View view) {
            startScanActivity(getString(R.string.scan_send_instruction_label), false);
        }

        public void onClickSend(View view) {
            if (!validateRequest()) {
                return;
            } else if (mActivity instanceof WindowControl) {
                // show send dialog
                showSendConfirmDialog();
            }
        }

        public void onClickMax(View view) {
            if (!useLocalCurrency) {
                String amount = String.format(Locale.ENGLISH, "%.6f", wallet.getUsableAccountBalanceBanano().floatValue());
                amount = amount.indexOf(".") < 0 ? amount : amount.replaceAll("0*$", "").replaceAll("\\.$", "");
                binding.sendAmount.setText(amount);
            } else {
                binding.sendAmount.setText(wallet.getAccountBalanceLocalCurrencyNoSymbol());
            }
        }

        public void onClickPaste(View view) {
            // copy address to clipboard
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                Address address = new Address(clipboard.getPrimaryClip().getItemAt(0).getText().toString());
                binding.sendAddress.setText(address.getAddress());
            }
        }

        public void onClickCurrencyChange(View view) {
            switchCurrency();
        }

        public void onClickContactTrigger(View view) {
            binding.sendAddress.setText("@");
            binding.sendAddress.requestFocus();
            binding.sendAddress.setSelection(1);
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.sendAddress, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
