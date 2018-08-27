package com.banano.kaliumwallet.ui.home;

import android.databinding.BindingMethod;
import android.databinding.BindingMethods;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.banano.kaliumwallet.task.DownloadOrRetreiveFileTask;
import com.banano.kaliumwallet.ui.common.UIUtil;
import com.banano.kaliumwallet.ui.send.SendDialogFragment;
import com.banano.kaliumwallet.util.svg.SvgDecoder;
import com.banano.kaliumwallet.util.svg.SvgDrawableTranscoder;
import com.banano.kaliumwallet.util.svg.SvgSoftwareLayerSetter;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.inject.Inject;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.bus.RxBus;
import com.banano.kaliumwallet.bus.SocketError;
import com.banano.kaliumwallet.bus.WalletHistoryUpdate;
import com.banano.kaliumwallet.bus.WalletPriceUpdate;
import com.banano.kaliumwallet.bus.WalletSubscribeUpdate;
import com.banano.kaliumwallet.databinding.FragmentHomeBinding;
import com.banano.kaliumwallet.model.Credentials;
import com.banano.kaliumwallet.model.KaliumWallet;
import com.banano.kaliumwallet.network.AccountService;
import com.banano.kaliumwallet.network.model.response.AccountCheckResponse;
import com.banano.kaliumwallet.network.model.response.AccountHistoryResponseItem;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseFragment;
import com.banano.kaliumwallet.ui.common.KeyboardUtil;
import com.banano.kaliumwallet.ui.common.WindowControl;
import com.banano.kaliumwallet.ui.receive.ReceiveDialogFragment;
import com.banano.kaliumwallet.ui.settings.SettingsDialogFragment;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Home Wallet Screen
 */

@BindingMethods({
        @BindingMethod(type = android.support.v7.widget.AppCompatImageView.class,
                attribute = "srcCompat",
                method = "setImageDrawable")
})
public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private WalletController controller;
    public static String TAG = HomeFragment.class.getSimpleName();
    private boolean logoutClicked = false;
    private DownloadOrRetreiveFileTask downloadMonkeyTask;

    @Inject
    AccountService accountService;

    @Inject
    KaliumWallet wallet;

    @Inject
    Realm realm;

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
        // Prevent leak after fragment destroy
        if (downloadMonkeyTask != null) {
            downloadMonkeyTask.setListener(null);
        }
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

        // subscribe to bus
        RxBus.get().register(this);

        // set status bar to blue
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
        controller = new WalletController();
        binding.homeRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.homeRecyclerview.setAdapter(controller.getAdapter());
        binding.homeSwiperefresh.setOnRefreshListener(() -> {
            accountService.requestUpdate();
            new Handler().postDelayed(() -> binding.homeSwiperefresh.setRefreshing(false), 5000);
        });

        // Initialize transaction history
        if (wallet != null && wallet.getAccountHistory() != null) {
            controller.setData(wallet.getAccountHistory(), new ClickHandlers());
        }

        updateAmounts();

        Credentials credentials = realm.where(Credentials.class).findFirst();

        // Retrieve/populate monKey
        if (credentials != null) {
            // Get monKey
            if (credentials.getAddressString() != null) {
                // Download monKey if doesn't exist
                String url = getString(R.string.monkey_api_url, credentials.getAddressString());
                downloadMonkeyTask = new DownloadOrRetreiveFileTask(getContext().getFilesDir(), String.format("%s.svg", credentials.getAddressString()));
                downloadMonkeyTask.setListener((File monkey) -> {
                    if (monkey == null) {
                        return;
                    }
                    try {
                        binding.homeMonkey.setVisibility(View.VISIBLE);
                        Uri svgUri = Uri.fromFile(monkey);
                        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(getContext())
                                .using(Glide.buildStreamModelLoader(Uri.class, getContext()), InputStream.class)
                                .from(Uri.class)
                                .as(SVG.class)
                                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                                .sourceEncoder(new StreamEncoder())
                                .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
                                .decoder(new SvgDecoder())
                                .listener(new SvgSoftwareLayerSetter<>());
                        requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE).load(svgUri).into(binding.homeMonkey);
                        requestBuilder.diskCacheStrategy(DiskCacheStrategy.NONE).load(svgUri).into(binding.monkeyOverlayImg);
                    } catch (Exception e) {
                        Timber.e("Failed to load monKey file");
                        e.printStackTrace();
                    }
                });
                downloadMonkeyTask.execute(url);
            }
        }

        // Override back button press
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (binding.monkeyOverlay.getVisibility() == View.VISIBLE) {
                    hideMonkeyOverlay();
                    return true;
                }
            }
            return false;
        });

        return view;
    }

    @Subscribe
    public void receiveHistory(WalletHistoryUpdate walletHistoryUpdate) {
        controller.setData(wallet.getAccountHistory(), new ClickHandlers());
        binding.homeSwiperefresh.setRefreshing(false);
        binding.homeRecyclerview.getLayoutManager().scrollToPosition(0);
    }

    @Subscribe
    public void receivePrice(WalletPriceUpdate walletPriceUpdate) {
        updateAmounts();
    }

    @Subscribe
    public void receiveSubscribe(WalletSubscribeUpdate walletSubscribeUpdate) {
        updateAmounts();
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
        binding.homeSwiperefresh.setRefreshing(false);
        Toast.makeText(getContext(),
                getString(R.string.error_message),
                Toast.LENGTH_SHORT)
                .show();
    }

    private void updateAmounts() {
        if (wallet != null) {
            binding.setWallet(wallet);
            if (wallet.getAccountBalanceBananoRaw() != null &&
                    wallet.getAccountBalanceBananoRaw().compareTo(new BigDecimal(0)) == 1) {
                // if balance > 0, enable send button
                binding.homeSendButton.setEnabled(true);
                // Tweak sizing based on how big amount is
                String balBanano = wallet.getAccountBalanceBanano();
                binding.amountBananoTitle.setText(balBanano);
                if (balBanano.length() == 12) {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                } else if (balBanano.length() > 12) {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                } else {
                    binding.amountBananoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
                }
            } else {
                binding.homeSendButton.setEnabled(false);
            }
        }
    }

    private void showMonkeyOverlay() {
        animateView(binding.homeMonkey, View.GONE, 0, 200);
        animateView(binding.monkeyOverlay, View.VISIBLE, 1.0f, 200);
    }

    private void hideMonkeyOverlay() {
        animateView(binding.monkeyOverlay, View.GONE, 0, 200);
        animateView(binding.homeMonkey, View.VISIBLE, 1.0f, 200);
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
                SendDialogFragment dialog = SendDialogFragment.newInstance();
                dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                        SendDialogFragment.TAG);

                executePendingTransactions();
            }
        }

        public void onClickTransaction(View view) {
            if (getActivity() instanceof WindowControl) {
                AccountHistoryResponseItem accountHistoryItem = (AccountHistoryResponseItem) view.getTag();
                if (accountHistoryItem != null) {
                    // show details dialog
                    TranDetailsFragment dialog = TranDetailsFragment.newInstance(accountHistoryItem.getHash(), accountHistoryItem.getAccount());
                    dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                            TranDetailsFragment.TAG);

                    executePendingTransactions();
                }
            }
        }

        public void onClickSettings(View view) {
            SettingsDialogFragment dialog = SettingsDialogFragment.newInstance();
            dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                    SettingsDialogFragment.TAG);

            executePendingTransactions();

            dialog.getDialog().setOnDismissListener(dialogInterface -> {
                if (binding != null) {
                    updateAmounts();
                }
            });
        }

        public void onClickMonkey(View view) {
            showMonkeyOverlay();
        }

        public void onClickMonkeyFrame(View view) {
            hideMonkeyOverlay();
        }

        /**
         * Execute all pending transactions
         */
        private void executePendingTransactions() {
            ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        }
    }
}
