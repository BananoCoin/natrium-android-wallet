package co.banano.natriumwallet.ui.settings;

import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import co.banano.natriumwallet.databinding.FragmentBackupSeedBinding;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;
import co.banano.natriumwallet.util.ExceptionHandler;

import javax.inject.Inject;

import io.realm.Realm;

public class BackupSeedDialogFragment extends BaseDialogFragment {
    public static String TAG = BackupSeedDialogFragment.class.getSimpleName();
    @Inject
    Realm realm;
    private FragmentBackupSeedBinding binding;
    private Handler mHandler;
    private Runnable mRunnable;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return New instance of BackupSeedDialogFragment
     */
    public static BackupSeedDialogFragment newInstance() {
        Bundle args = new Bundle();
        BackupSeedDialogFragment fragment = new BackupSeedDialogFragment();
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

        // No screenshots :smug:
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_backup_seed, container, false);
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
            public void onTap(View view) {
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Set values
        binding.setHandlers(new ClickHandlers());

        // get seed from storage
        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            binding.setSeed(credentials.getSeed());
        } else {
            ExceptionHandler.handle(new Exception("Problem accessing generated seed"));
        }

        // Set runnable to reset seed text
        mHandler = new Handler();
        mRunnable = () -> {
            binding.backupSeedSeed.setTextColor(getResources().getColor(R.color.ltblue));
            binding.backupSeedCopied.setVisibility(View.INVISIBLE);
        };

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        // Cancel seed copy callback
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }

        public void onClickSeed(View v) {
            if (binding != null && binding.backupSeedSeed != null) {
                // copy seed to clipboard
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(ClipboardAlarmReceiver.CLIPBOARD_NAME, binding.backupSeedSeed.getText().toString());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }

                binding.backupSeedSeed.setTextColor(getResources().getColor(R.color.green_light));
                binding.backupSeedCopied.setVisibility(View.VISIBLE);

                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 1500);
                }

                setClearClipboardAlarm();
            }
        }
    }
}
