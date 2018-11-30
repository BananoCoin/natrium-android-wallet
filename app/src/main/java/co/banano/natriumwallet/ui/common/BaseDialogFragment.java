package co.banano.natriumwallet.ui.common;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import co.banano.natriumwallet.R;
import co.banano.natriumwallet.broadcastreceiver.ClipboardAlarmReceiver;
import co.banano.natriumwallet.ui.pin.CreatePinDialogFragment;
import co.banano.natriumwallet.ui.pin.PinDialogFragment;
import co.banano.natriumwallet.ui.scan.ScanActivity;
import co.banano.natriumwallet.util.ExceptionHandler;

/**
 * Base class for dialog fragments
 */

public class BaseDialogFragment extends DialogFragment {
    protected static final int SCAN_RESULT = 2;
    protected static final int SEND_COMPLETE = 3;
    protected static final int SEND_FAILED = 4;
    protected static final int SEND_FAILED_AMOUNT = 5;
    protected static final int SEND_CANCELED = 6;
    protected static final int SEND_RESULT = 7;
    protected static final int CHANGE_FAILED = 8;
    protected static final int CHANGE_COMPLETE = 9;
    protected static final int CHANGE_RESULT = 10;
    private static final int ZXING_CAMERA_PERMISSION = 1;
    protected View view;
    private String scanActivityTitle;

    /**
     * Animate appearance of a view
     *
     * @param view         View to animate
     * @param toVisibility Visibility at the end of animation
     * @param toAlpha      Alpha at the end of animation
     * @param duration     Animation duration in ms
     */
    public static void animateView(final View view, final int toVisibility, float toAlpha, int duration) {
        boolean show = toVisibility == View.VISIBLE;
        if (show) {
            view.setAlpha(0);
        }
        view.setVisibility(View.VISIBLE);
        view.animate()
                .setDuration(duration)
                .alpha(show ? toAlpha : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(toVisibility);
                    }
                });
    }

    @Override
    public void show(FragmentManager manager, String tag) {

        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commit();
        } catch (IllegalStateException e) {
            ExceptionHandler.handle(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().setOnKeyListener((dialog, keyCode, event) -> {

            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                //This is the filter
                if (event.getAction() != KeyEvent.ACTION_DOWN)
                    return true;
                else {
                    dismiss();
                    return true; // pretend we've processed it
                }
            } else
                return false; // pass on to be processed as normal
        });
    }

    /**
     * Set status bar color to gray
     */
    protected void setStatusBarGray() {
        setStatusBarColor(R.color.gray);
    }

    private void setStatusBarColor(int color) {
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).setStatusBarColor(color);
        }
    }

    /**
     * Set the navigation bar color
     */
    protected void setNavigationBarColor() {
        setNavigationBarColor(R.color.gray);
    }

    private void setNavigationBarColor(int color) {
        if (getActivity() instanceof  WindowControl && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((WindowControl) getActivity()).setNavigationBarColor(color);
        }
    }

    /**
     * Set alarm for 2 minutes to clear the clipboard
     */
    protected void setClearClipboardAlarm() {
        // create pending intent
        AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), ClipboardAlarmReceiver.class);
        intent.setAction("co.banano.natriumwallet.alarm");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

        // set a two minute alarm to start the pending intent
        if (alarmMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 120 * 1000, alarmIntent);
            } else {
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 120 * 1000, alarmIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case ZXING_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Intent intent = new Intent(getActivity(), ScanActivity.class);
                    intent.putExtra(ScanActivity.EXTRA_TITLE, scanActivityTitle);
                    startActivityForResult(intent, SCAN_RESULT);
                }
            }
        }
    }

    /**
     * Start the scanner activity
     *
     * @param title Title that should be displayed above the viewfinder
     */
    protected void startScanActivity(String title, boolean isSeedScanner) {
        this.scanActivityTitle = title;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // check first to see if camera permission has been granted
            requestPermissions(new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(getActivity(), ScanActivity.class);
            intent.putExtra(ScanActivity.EXTRA_TITLE, this.scanActivityTitle);
            startActivityForResult(intent, SCAN_RESULT);
        }
    }

    protected void showCreatePinScreen() {
        CreatePinDialogFragment dialog = CreatePinDialogFragment.newInstance();
        dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                CreatePinDialogFragment.TAG);

        // make sure that dialog is not null
        ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
        // reset status bar to blue when dialog is closed
        if (dialog.getDialog() != null) {
            dialog.getDialog().setOnDismissListener(dialogInterface -> {
                // close keyboard
                KeyboardUtil.hideKeyboard(getActivity());
            });
        }
    }

    protected void showPinScreen(String subtitle) {
        PinDialogFragment dialog = PinDialogFragment.newInstance(subtitle);
        dialog.show(((WindowControl) getActivity()).getFragmentUtility().getFragmentManager(),
                PinDialogFragment.TAG);

        // make sure that dialog is not null
        ((WindowControl) getActivity()).getFragmentUtility().getFragmentManager().executePendingTransactions();
    }

    /**
     * Create a link from the text on the view
     *
     * @param v    TextView
     * @param text id of text to add to the field
     */
    protected void createLink(TextView v, int text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.setText(Html.fromHtml(getString(text), Html.FROM_HTML_MODE_LEGACY));
        } else {
            v.setText(Html.fromHtml(getString(text)));
        }
        v.setTransformationMethod(new LinkTransformationMethod());
        v.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
