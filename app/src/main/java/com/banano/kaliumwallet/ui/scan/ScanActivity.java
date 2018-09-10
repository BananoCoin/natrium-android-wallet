package com.banano.kaliumwallet.ui.scan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.banano.kaliumwallet.R;
import com.google.zxing.Result;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class ScanActivity extends BaseScannerActivity implements ZXingScannerView.ResultHandler {
    public static final String QR_CODE_RESULT = "QRCodeResult";
    public static final String EXTRA_TITLE = "ScanActivityTitle";
    private ZXingScannerView mScannerView;

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
        contentFrame.addView(mScannerView);

        // get title
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            TextView instructions = findViewById(R.id.scan_instruction_label);
            if (instructions != null) {
                instructions.setText(title);
            }
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
        Bundle conData = new Bundle();
        conData.putString(QR_CODE_RESULT, rawResult.getText());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finish();
    }
}
