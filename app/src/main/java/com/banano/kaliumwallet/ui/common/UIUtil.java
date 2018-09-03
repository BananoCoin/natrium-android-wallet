package com.banano.kaliumwallet.ui.common;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.banano.kaliumwallet.R;

/**
 * UI Utility Functions
 */

public class UIUtil {
    public static final double SMALL_DEVICE_DIALOG_HEIGHT = 0.88;
    public static final double LARGE_DEVICE_DIALOG_HEIGHT = 0.85;
    public static final double LARGE_DEVICE_DIALOG_HEIGHT_SMALLER = 0.83;

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow transparent
     * Last 6 characters are yellow transparent
     *
     * @param s       Spannable
     * @param context Context
     */
    public static void colorizeSpannable(Spannable s, Context context) {
        if (context == null) {
            return;
        }
        if (s.length() > 0) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.yellow_dark_transparent)), 0, s.length() > 10 ? 11 : s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s.length() > 58) {
                s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.yellow_dark_transparent)), 58, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Colorize a string in the following manner:
     * Characters 12-57 are white
     *
     * @param s       Spannable
     * @param context Context
     */
    public static void whitenMiddleSpannable(Spannable s, Context context) {
        if (context == null) {
            return;
        }
        if (s.length() > 58) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white_90)),  11, 58, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow transparent
     * Last 6 characters are yellow transparent
     *
     * @param s       Spannable
     * @param context Context
     */
    public static void colorizeSpannableBright(Spannable s, Context context) {
        if (context == null) {
            return;
        }
        if (s.length() > 0) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.yellow)), 0, s.length() > 10 ? 11 : s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s.length() > 58) {
                s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.yellow)), 58, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow transparent
     * Last 6 characters are yellow transparent
     *
     * @param s       Spannable
     * @param context Context
     */
    public static void colorizeSpannableGreen(Spannable s, Context context) {
        if (context == null) {
            return;
        }
        if (s.length() > 0) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.green_light)), 0, s.length() > 10 ? 11 : s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (s.length() > 58) {
                s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white_90)),  11, 58, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.green_light)), 58, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Replace occurences of BANANO with yellow text
     *
     * @param s     Spannable
     * @param context Context
     */
    public static Spannable colorizeBanano(String s, Context context) {
        Spannable sp = new SpannableString(s);
        if (context == null || !s.toUpperCase().contains("BANANO")) {
            return sp;
        }
        int indexStart = s.toUpperCase().indexOf("BANANO");
        int indexEnd = 6;
        if (indexStart < 0) {
            return sp;
        }
        sp.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.yellow)), indexStart, indexStart+indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow
     * Last 6 characters are yellow
     *
     * @param s       String
     * @param context Context
     * @return Spannable string
     */
    public static Spannable getColorizedSpannable(String s, Context context) {
        Spannable sp = new SpannableString(s);
        colorizeSpannable(sp, context);
        return sp;
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow
     * Last 6 characters are yellow
     *
     * @param s       String
     * @param context Context
     * @return Spannable string
     */
    public static Spannable getColorizedSpannableBright(String s, Context context) {
        Spannable sp = new SpannableString(s);
        colorizeSpannableBright(sp, context);
        return sp;
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are green
     * Middle characters are white
     * Last 6 characters are yellow
     *
     * @param s       String
     * @param context Context
     * @return Spannable string
     */
    public static Spannable getColorizedSpannableGreen(String s, Context context) {
        Spannable sp = new SpannableString(s);
        colorizeSpannableGreen(sp, context);
        return sp;
    }

    /**
     * Colorize a string in the following manner:
     * First 11 characters are yellow
     * Middle characters are white
     * Last 6 characters are yellow
     *
     * @param s       String
     * @param context Context
     * @return Spannable string
     */
    public static Spannable getColorizedSpannableBrightWhite(String s, Context context) {
        Spannable sp = new SpannableString(s);
        colorizeSpannableBright(sp, context);
        whitenMiddleSpannable(sp, context);
        return sp;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into dp
     * @param context Context to get resources and device specific display metrics
     * @return An int value to represent dp equivalent to px value
     */
    public static int convertPixelsToDp(int px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Get what dialog height should be based on device size
     *
     * @param shortDialog Whether or not to return smaller dialog height
     * @param context Context to get device display metrics
     * @return Height in pixels
     */
    public static int getDialogHeight(boolean shortDialog, Context context) {
        Display d = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        d.getRealMetrics(metrics);

        int height = UIUtil.convertPixelsToDp(metrics.heightPixels, context);
        double heightPercent = UIUtil.SMALL_DEVICE_DIALOG_HEIGHT;
        if (height > 700 && shortDialog) {
            heightPercent = UIUtil.LARGE_DEVICE_DIALOG_HEIGHT_SMALLER;
        } else if (height > 700) {
            heightPercent = UIUtil.LARGE_DEVICE_DIALOG_HEIGHT;
        }
        return (int)(metrics.heightPixels * heightPercent);
    }
}