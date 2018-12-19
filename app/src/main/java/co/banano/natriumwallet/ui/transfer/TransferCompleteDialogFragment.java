package co.banano.natriumwallet.ui.transfer;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import co.banano.natriumwallet.R;
import co.banano.natriumwallet.databinding.FragmentTransferCompleteBinding;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.ui.common.BaseDialogFragment;
import co.banano.natriumwallet.ui.common.SwipeDismissTouchListener;
import co.banano.natriumwallet.ui.common.UIUtil;

public class TransferCompleteDialogFragment extends BaseDialogFragment {
    public static String TAG = TransferCompleteDialogFragment.class.getSimpleName();

    private FragmentTransferCompleteBinding binding;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return FragmentTransferCompleteBinding instance
     */
    public static TransferCompleteDialogFragment newInstance(String amount) {
        Bundle args = new Bundle();
        args.putString("amount", amount);
        TransferCompleteDialogFragment fragment = new TransferCompleteDialogFragment();
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

        String amount = getArguments().getString("amount");

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_transfer_complete, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

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
                dismiss();
            }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // Set amount text
        binding.transferDescription.setText(getString(R.string.transfer_complete_text, amount));

        return view;
    }

    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }
    }
}