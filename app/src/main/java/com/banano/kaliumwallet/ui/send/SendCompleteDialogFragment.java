package com.banano.kaliumwallet.ui.send;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.databinding.FragmentSendCompleteBinding;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.SwipeDismissTouchListener;
import com.banano.kaliumwallet.ui.common.UIUtil;

/**
 * Send complete screen
 */
public class SendCompleteDialogFragment extends BaseDialogFragment {
    private FragmentSendCompleteBinding binding;
    public static String TAG = SendCompleteDialogFragment.class.getSimpleName();

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return SendConfirmDialogFragment instance
     */
    public static SendCompleteDialogFragment newInstance(String destination, String amount) {
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putString("amount", amount);
        SendCompleteDialogFragment fragment = new SendCompleteDialogFragment();
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

        String destination = getArguments().getString("destination");
        String amount = getArguments().getString("amount");

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_send_complete, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // Restrict height
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Point size = new Point();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = metrics.heightPixels;
        double heightPercent = UIUtil.SMALL_DEVICE_DIALOG_HEIGHT;
        if (metrics.heightPixels > 1500) {
            heightPercent = UIUtil.LARGE_DEVICE_DIALOG_HEIGHT;
        }
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, (int) (height * heightPercent));
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
                dismiss();
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // colorize address text
        if (binding != null &&
                destination != null) {
            binding.sentDestination.setText(UIUtil.getColorizedSpannableGreen(destination, getContext()));
        }

        binding.sentAmount.setText(String.format("%s BAN", amount));

        return view;
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }
    }
}
