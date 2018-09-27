package co.banano.natriumwallet.ui.scan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.TextView;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.model.Address;
import com.google.zxing.Result;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class ScanActivity extends BaseScannerActivity implements ZXingScannerView.ResultHandler {
    public static final String QR_CODE_RESULT = "QRCodeResult";
    public static final String EXTRA_TITLE = "ScanActivityTitle";
    private ZXingScannerView mScannerView;
    private Runnable mRunnable;
    private Handler mHandler;
    private TextView mInstructionsText;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_scan);
        setupToolbar();

        ViewGroup contentFrame = findViewById(R.id.scan_content_frame);
        mScannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new KaliumViewFinderView(context);
            }
        };
        mScannerView.setAutoFocus(true);
        contentFrame.addView(mScannerView);

        // get title
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            mInstructionsText = findViewById(R.id.scan_instruction_label);
            if (mInstructionsText != null) {
                mInstructionsText.setText(title);
            }
        }

        mHandler = new Handler();
        mRunnable = () -> {
            if (mInstructionsText != null) {
                mInstructionsText.setText(title);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        String address = Address.findAddress(rawResult.getText());
        if (address != null && !address.isEmpty()) {
            Bundle conData = new Bundle();
            conData.putString(QR_CODE_RESULT, rawResult.getText());
            Intent intent = new Intent();
            intent.putExtras(conData);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            mScannerView.resumeCameraPreview(this);
            mScannerView.setAutoFocus(true);
            if (mInstructionsText != null) {
                mInstructionsText.setText(getString(R.string.send_invalid_address));
                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 800);
                }
            }
        }
    }
}
