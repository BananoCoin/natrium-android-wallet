package co.banano.natriumwallet.ui.home;

import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.bus.ContactAdded;
import co.banano.natriumwallet.bus.ContactRemoved;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.bus.SocketError;
import co.banano.natriumwallet.bus.TransactionItemClicked;
import co.banano.natriumwallet.bus.WalletHistoryUpdate;
import co.banano.natriumwallet.bus.WalletPriceUpdate;
import co.banano.natriumwallet.bus.WalletSubscribeUpdate;
import co.banano.natriumwallet.databinding.FragmentHomeBinding;
import co.banano.natriumwallet.model.Contact;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.model.PriceConversion;
import co.banano.natriumwallet.network.AccountService;
import co.banano.natriumwallet.network.model.response.AccountCheckResponse;
import co.banano.natriumwallet.network.model.response.AccountHistoryResponseItem;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseFragment;
import co.banano.natriumwallet.ui.common.FragmentUtility;
import co.banano.natriumwallet.ui.common.KeyboardUtil;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.ui.common.WindowControl;
import co.banano.natriumwallet.ui.contact.ContactOverviewFragment;
import co.banano.natriumwallet.ui.receive.ReceiveDialogFragment;
import co.banano.natriumwallet.ui.send.SendDialogFragment;
import co.banano.natriumwallet.ui.settings.SettingsFragment;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * Home Wallet Screen
 */

@BindingMethods({
        @BindingMethod(type = androidx.appcompat.widget.AppCompatImageView.class,
                attribute = "srcCompat",
                method = "setImageDrawable")
})
public class HomeFragment extends BaseFragment {
    public static String TAG = HomeFragment.class.getSimpleName();
    public boolean retrying = false;
    @Inject
    AccountService accountService;
    @Inject
    KaliumWallet wallet;
    @Inject
    Realm realm;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    private FragmentHomeBinding binding;
    private Handler mHandler;
    private Runnable mRunnable;
    private HashMap<String, String> mContactCache = new HashMap<>();
    private AccountHistoryAdapter mAdapter;

    /**
     * Create new instance of the fragment (handy pattern if any data needs to be passed to it)
     *
     * @return HomeFragment
     */
    public static HomeFragment newInstance() {
        Bundle args = new Bundle();
        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.menu_home, menu);
        //super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }
        retrying = false;

        // subscribe to bus
        RxBus.get().register(this);

        // set status bar color
        setStatusBarGray();

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_home, container, false);
        view = binding.getRoot();

        // Currency bindings
        binding.setWallet(wallet);
        binding.setLocalCurrency(wallet.getLocalCurrency());
        binding.setHandlers(new ClickHandlers());

        // hide keyboard
        KeyboardUtil.hideKeyboard(getActivity());

        // initialize recyclerview (list of wallet transactions)
        binding.homeRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new AccountHistoryAdapter(new ArrayList<>());
        binding.homeRecyclerview.setAdapter(mAdapter);
        mRunnable = () -> {
            retrying = false;
            binding.homeSwiperefresh.setRefreshing(false);
        };
        mHandler = new Handler();
        binding.homeSwiperefresh.setOnRefreshListener(() -> {
            if (!retrying) {
                accountService.requestUpdate();
                if (mHandler != null && mRunnable != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 5000);
                }
            }
        });

        // Initialize transaction history
        if (wallet != null && wallet.getAccountHistory() != null) {
            if (wallet.getAccountHistory().size() > 0) {
                binding.loadingAnimation.setVisibility(View.GONE);
            }
            updateAccountHistory();
        }

        Credentials credentials = realm.where(Credentials.class).findFirst();

        // Hack for easier settings access https://stackoverflow.com/questions/17942223/drawerlayout-modify-sensitivity
        // assuming mDrawerLayout is an instance of android.support.v4.widget.DrawerLayout
        try {

            // get dragger responsible for the dragging of the left drawer
            Field draggerField = DrawerLayout.class.getDeclaredField("mLeftDragger");
            draggerField.setAccessible(true);
            ViewDragHelper vdh = (ViewDragHelper) draggerField.get(binding.drawerLayout);

            // get access to the private field which defines
            // how far from the edge dragging can start
            Field edgeSizeField = ViewDragHelper.class.getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);

            // increase the edge size - while x2 should be good enough,
            // try bigger values to easily see the difference
            int origEdgeSize = (int) edgeSizeField.get(vdh);
            int newEdgeSize = origEdgeSize * 5;
            edgeSizeField.setInt(vdh, newEdgeSize);

        } catch (Exception e) {
            // we unexpectedly failed - e.g. if internal implementation of
            // either ViewDragHelper or DrawerLayout changed
        }

        // Change status bar color when drawer open
        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {
                if (v > 0.5) {
                    setStatusBarDarkGray();
                } else {
                    setStatusBarGray();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                setStatusBarDarkGray();
            }

            @Override
            public void onDrawerClosed(@NonNull View view) {
                setStatusBarGray();
                // Close contacts if open
                Fragment contactOverviewFragment = ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().findFragmentByTag(ContactOverviewFragment.TAG);
                if (contactOverviewFragment != null) {
                    FragmentUtility.disableFragmentAnimations = true;
                    ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().popBackStackImmediate();
                    ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
                    FragmentUtility.disableFragmentAnimations = false;
                }
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });

        // Add sttings fragment to drawer container
        FragmentTransaction ft = ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().beginTransaction();
        ft.replace(R.id.settings_frag_container, SettingsFragment.newInstance()).commit();

        return view;
    }

    private String getContactName(String address) {
        if (mContactCache.containsKey(address)) {
            return mContactCache.get(address);
        }
        RealmQuery realmQuery = realm.where(Contact.class);
        realmQuery.equalTo("address", address);
        if (realmQuery.count() > 0) {
            Contact c = (Contact) realmQuery.findFirst();
            mContactCache.put(address, c.getDisplayName());
            return c.getDisplayName();
        }
        return null;
    }

    private void updateAccountHistory() {
        List<AccountHistoryResponseItem> historyList = wallet.getAccountHistory();
        for (AccountHistoryResponseItem item : historyList) {
            String name = getContactName(item.getAccount());
            if (name != null) {
                item.setContactName(name);
            } else {
                item.setContactName(null);
            }
        }
        mAdapter.updateList(historyList);
        binding.homeRecyclerview.getLayoutManager().scrollToPosition(0);
    }

    @Subscribe
    public void receiveContactAdded(ContactAdded contactAdded) {
        if (mAdapter == null || getContext() == null) {
            return;
        }
        updateAccountHistory();
        getActivity().runOnUiThread(() -> {
            mAdapter.notifyDataSetChanged();
        });
        UIUtil.showToast(getString(R.string.contact_added, contactAdded.getName()), getContext());
    }

    @Subscribe
    public void receiveContactRemoved(ContactRemoved contactRemoved) {
        if (mContactCache == null || mAdapter == null || getContext() == null) {
            return;
        }
        if (mContactCache.containsValue(contactRemoved.getName())) {
            mContactCache.remove(contactRemoved.getAddress());
        }
        updateAccountHistory();
        getActivity().runOnUiThread(() -> {
            mAdapter.notifyDataSetChanged();
        });
        UIUtil.showToast(getString(R.string.contact_removed, contactRemoved.getName()), getContext());
    }

    @Subscribe
    public void receiveHistory(WalletHistoryUpdate walletHistoryUpdate) {
        if (wallet == null || binding == null || mAdapter == null || getContext() == null) {
            return;
        }
        binding.loadingAnimation.setVisibility(View.GONE);
        if (wallet.getAccountHistory().size() > 0) {
            binding.exampleCards.setVisibility(View.GONE);
        }
        updateAccountHistory();
        binding.homeSwiperefresh.setRefreshing(false);
    }

    @Subscribe
    public void receivePrice(WalletPriceUpdate walletPriceUpdate) {
        updateAmounts();
    }

    @Subscribe
    public void receiveSubscribe(WalletSubscribeUpdate walletSubscribeUpdate) {
        updateAmounts();
        if (wallet.getOpenBlock() == null) {
            binding.introText.exampleIntroText.setText(UIUtil.colorizeNano(binding.introText.exampleIntroText.getText().toString(), getContext()));
            binding.loadingAnimation.setVisibility(View.GONE);
            binding.exampleCards.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe
    public void receiveAccountCheck(AccountCheckResponse accountCheckResponse) {
        if (accountCheckResponse.getReady()) {
            // account is on the network, so send a pending request
            accountService.requestPending();
        }
    }

    @Subscribe
    public void receiveError(SocketError error) {
        binding.loadingAnimation.setVisibility(View.GONE);
        binding.homeSwiperefresh.setRefreshing(false);
        // Retry refresh
        if (!retrying) {
            retrying = true;
            accountService.requestUpdate();
            if (mHandler != null && mRunnable != null) {
                mHandler.removeCallbacks(mRunnable);
                mHandler.postDelayed(mRunnable, 5000);
            }
        } else {
            retrying = false;
        }
    }

    @Subscribe
    public void receiveTranItemClick(TransactionItemClicked tran) {
        // show details dialog
        if (getActivity() instanceof WindowControl) {
            TranDetailsFragment dialog = TranDetailsFragment.newInstance(tran.getHash(), tran.getAccount());
            dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                    TranDetailsFragment.TAG);

            ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        }
    }

    private void updateAmounts() {
        if (wallet != null) {
            binding.setWallet(wallet);
            if (wallet.getAccountBalanceBananoRaw() != null &&
                    wallet.getAccountBalanceBananoRaw().compareTo(new BigDecimal(0)) == 1) {
                // if balance > 0, enable send button
                binding.homeSendButton.setEnabled(true);
                binding.homeSendButton.setBackground(getResources().getDrawable(R.drawable.bg_solid_button));
                // Hide placeholder
                binding.bananoPlaceholder.setVisibility(View.GONE);
                binding.amountBananoSymbol.setVisibility(View.VISIBLE);
                binding.amountBananoTitle.setVisibility(View.VISIBLE);
                // Set default price
                switch (sharedPreferencesUtil.getPriceConversion()) {
                    case BTC:
                        binding.btcPrice.setVisibility(View.VISIBLE);
                        binding.amountLocalCurrencyTitle.setVisibility(View.VISIBLE);
                        break;
                    default:
                        binding.btcPrice.setVisibility(View.GONE);
                        binding.amountLocalCurrencyTitle.setVisibility(View.GONE);
                        break;
                }
                // Tweak sizing based on how big amount is
                String balBanano = wallet.getAccountBalanceBanano();
                binding.amountBananoTitle.setText(balBanano);
                if (balBanano.length() >= 9 && balBanano.length() < 12) {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.main_balance_md_text));
                } else if (balBanano.length() >= 12) {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.main_balance_sm_text));
                } else {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.main_balance_lg_text));
                }
            } else {
                binding.homeSendButton.setEnabled(false);
                binding.homeSendButton.setBackground(getResources().getDrawable(R.drawable.bg_solid_button_disabled));
            }
        }
    }

    public class ClickHandlers {
        public void onClickReceive(View view) {
            if (getActivity() instanceof WindowControl) {
                // show receive dialog
                ReceiveDialogFragment dialog = ReceiveDialogFragment.newInstance();
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        ReceiveDialogFragment.TAG);
                executePendingTransactions();
            }
        }

        public void onClickSend(View view) {
            if (getActivity() instanceof WindowControl) {
                // show send dialog
                SendDialogFragment dialog = SendDialogFragment.newInstance(null);
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        SendDialogFragment.TAG);

                executePendingTransactions();
            }
        }

        public void onClickSettings(View view) {
            binding.drawerLayout.openDrawer(Gravity.START);
        }

        public void onClickPrice(View view) {
            if (sharedPreferencesUtil.getPriceConversion() == PriceConversion.BTC) {
                // Switch to NONE
                binding.btcPrice.setVisibility(View.GONE);
                binding.amountLocalCurrencyTitle.setVisibility(View.GONE);
                sharedPreferencesUtil.setPriceConversion(PriceConversion.NONE);
            } else {
                // Switch to BTC
                binding.btcPrice.setVisibility(View.VISIBLE);
                binding.amountLocalCurrencyTitle.setVisibility(View.VISIBLE);
                sharedPreferencesUtil.setPriceConversion(PriceConversion.BTC);
            }
        }

        /**
         * Execute all pending transactions
         */
        private void executePendingTransactions() {
            ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        }
    }
}
