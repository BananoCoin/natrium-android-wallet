package com.banano.kaliumwallet.ui.receive;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.banano.kaliumwallet.ui.common.SwipeDismissTouchListener;
import com.github.sumimakito.awesomeqr.AwesomeQRCode;

import java.io.File;
import java.io.FileOutputStream;

import javax.inject.Inject;

import com.banano.kaliumwallet.R;
import com.banano.kaliumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import com.banano.kaliumwallet.databinding.FragmentReceiveBinding;
import com.banano.kaliumwallet.model.Address;
import com.banano.kaliumwallet.model.Credentials;
import com.banano.kaliumwallet.ui.common.ActivityWithComponent;
import com.banano.kaliumwallet.ui.common.BaseDialogFragment;
import com.banano.kaliumwallet.ui.common.UIUtil;

import io.realm.Realm;

/**
 * Receive main screen
 */
public class ReceiveDialogFragment extends BaseDialogFragment {
    private FragmentReceiveBinding binding;
    public static String TAG = ReceiveDialogFragment.class.getSimpleName();
    private static final int QRCODE_SIZE = 240;
    private static final String TEMP_FILE_NAME = "bananoreceive.png";
    private static final String ADDRESS_KEY = "com.banano.kaliumwallet.ui.receive.ReceiveDialogFragment.Address";
    private Address address;
    private String fileName;
    private Runnable mRunnable;
    private Handler mHandler;

    @Inject
    Realm realm;

    /**
     * Create new instance of the dialog fragment (handy pattern if any data needs to be passed to it)
     *
     * @return ReceiveDialogFragment instance
     */
    public static ReceiveDialogFragment newInstance() {
        Bundle args = new Bundle();
        ReceiveDialogFragment fragment = new ReceiveDialogFragment();
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

        // get data
        Credentials credentials = realm.where(Credentials.class).findFirst();
        if (credentials != null) {
            address = new Address(credentials.getAddressString());
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_receive, container, false);
        view = binding.getRoot();
        binding.setHandlers(new ClickHandlers());

        // Restrict height
        Window window = getDialog().getWindow();
        Point size = new Point();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = metrics.heightPixels;
        double heightPercent = 0.95;
        if (metrics.heightPixels > 1500) {
            heightPercent = 0.88;
        } else {
            ViewGroup.MarginLayoutParams qrMargin = (ViewGroup.MarginLayoutParams)binding.qrContainer.getLayoutParams();
            qrMargin.bottomMargin = (int)UIUtil.convertDpToPixel(5, getContext());
            binding.qrContainer.setLayoutParams(qrMargin);
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
            public void onTap(View view) { }
        }, SwipeDismissTouchListener.TOP_TO_BOTTOM));

        // colorize address text
        if (binding != null &&
                binding.receiveAddress != null &&
                binding.receiveCard != null &&
                binding.receiveCard.cardAddress != null &&
                address != null &&
                address.getAddress() != null) {
            binding.receiveAddress.setText(UIUtil.getColorizedSpannable(address.getAddress(), getContext()));
            binding.receiveCard.cardAddress.setText(UIUtil.getColorizedSpannableBright(address.getAddress(), getContext()));
        }

        // Tweak layout for shorter devices
        float ratio = ((float)metrics.heightPixels / (float)metrics.widthPixels);
        if (ratio < 1.8) {
            binding.receiveOuter.getLayoutParams().height = (int)UIUtil.convertDpToPixel(260, getContext());
            binding.receiveOuter.getLayoutParams().width = (int)UIUtil.convertDpToPixel(260, getContext());
            binding.receiveBarcode.getLayoutParams().height = (int)UIUtil.convertDpToPixel(125, getContext());
            binding.receiveBarcode.getLayoutParams().width = (int)UIUtil.convertDpToPixel(125, getContext());
            ViewGroup.MarginLayoutParams barcodeMargin = (ViewGroup.MarginLayoutParams)binding.receiveBarcode.getLayoutParams();
            barcodeMargin.topMargin = (int)UIUtil.convertDpToPixel(33, getContext());
            binding.receiveBarcode.setLayoutParams(barcodeMargin);
        }

        // Set QR bg for compat
        Drawable qrBackground = VectorDrawableCompat.create(getResources(), R.drawable.qr_border, getContext().getTheme());
        binding.receiveOuter.setBackground(qrBackground);

        // generate QR code
        new AwesomeQRCode.Renderer()
                .contents(address.getAddress())
                .size((int) UIUtil.convertDpToPixel(QRCODE_SIZE, getContext()))
                .whiteMargin(false)
                .margin(0)
                .dotScale(1.0f)
                .renderAsync(new AwesomeQRCode.Callback() {
                    @Override
                    public void onRendered(AwesomeQRCode.Renderer renderer, final Bitmap bitmap) {
                        getActivity().runOnUiThread(() -> {
                            binding.receiveBarcode.setImageBitmap(bitmap);
                            binding.receiveCard.receiveCardQrBg.setBackground(qrBackground);
                            binding.receiveCard.cardBarcodeImg.setImageBitmap(bitmap);
                        });
                    }

                    @Override
                    public void onError(AwesomeQRCode.Renderer renderer, Exception e) {
                        e.printStackTrace();
                    }
                });

        // Set runnable to reset address copied text
        mHandler = new Handler();
        mRunnable = () -> {
            binding.receiveButtonCopy.setBackground(getResources().getDrawable(R.drawable.bg_solid_button));
            binding.receiveButtonCopy.setTextColor(getResources().getColor(R.color.gray));
            binding.receiveButtonCopy.setText(getString(R.string.receive_copy_cta));
        };

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHandler != null  && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    public Bitmap setViewToBitmapImage(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        }
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    public void saveImage(Bitmap finalBitmap) {
        try {
            File cachePath = new File(getContext().getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream outputStream;
            fileName = System.currentTimeMillis() + "_" + TEMP_FILE_NAME;
            File file = new File(cachePath + "/" + fileName);
            outputStream = new FileOutputStream(file, true);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class ClickHandlers {
        public void onClickClose(View view) {
            dismiss();
        }

        public void onClickShare(View view) {
            binding.receiveCard.cardLayout.setVisibility(View.VISIBLE);
            saveImage(setViewToBitmapImage(binding.receiveCard.cardLayout));
            File imagePath = new File(getContext().getCacheDir(), "images");
            File newFile = new File(imagePath, fileName);
            Uri imageUri = FileProvider.getUriForFile(getContext(), "com.banano.kaliumwallet.fileprovider", newFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_TEXT, address.getAddress());
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.setDataAndType(imageUri, getActivity().getContentResolver().getType(imageUri));
            shareIntent.setType("image/*");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.receive_share_title)));
            binding.receiveCard.cardLayout.setVisibility(View.INVISIBLE);
        }

        public void onClickCopy(View view) {
            // copy address to clipboard
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(ClipboardAlarmReceiver.CLIPBOARD_NAME, address.getAddress());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }

            binding.receiveButtonCopy.setBackground(getResources().getDrawable(R.drawable.bg_green_button));
            binding.receiveButtonCopy.setTextColor(getResources().getColor(R.color.green_dark));
            binding.receiveButtonCopy.setText(getString(R.string.receive_copied));

            if (mHandler != null) {
                mHandler.postDelayed(mRunnable, 700);
            }
        }
    }
}
