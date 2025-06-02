package it.uniba.dib.sms2324.ecowateringcommon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Common {

    // CONSTANTS

    public static final int ACTION_BACK_PRESSED = 1023;
    public static final int BT_PERMISSION_REQUEST = 2001;
    public static final String CALLED_FROM_HUB = "CALLED_FROM_HUB";
    public static final String CALLED_FROM_DEVICE = "CALLED_FROM_DEVICE";
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";
    public static final int GPS_WIFI_ENABLE_REQUEST = 2003;
    public static final int GPS_ENABLE_REQUEST = 2004;
    public static final int GPS_BT_ENABLE_REQUEST = 2006;
    public static final int GPS_ENABLED_RESULT = 1007;
    public static final int LOCATION_PERMISSION_REQUEST = 2002;
    public static final String MANAGE_EWH_INTENT_OBJ = "MANAGE_EWH_OBJ";
    public static final int NULL_INT_VALUE = 7001;
    public static final String NULL_STRING_VALUE = "null";
    public static final int REFRESH_FRAGMENT = 1030;
    public static final String REMOVE_REMOTE_DEVICE_RESPONSE = "remoteDeviceRemoved";
    public static final String LOG_NORMAL = "NORMAL_LOG";
    public static final String LOG_SERVICE = "SERVICE_LOG";
    public static final String LOG_SHARED_PREFERENCES = "SHARED_PREFERENCES_LOG";
    private static final String THIS_URL = "https://theall.altervista.org/sms2324";
    private static final UUID THIS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String VOID_STRING_VALUE = "";
    public static final int WIFI_PERMISSION_REQUEST = 2005;
    private static final String URI_SCHEME_PACKAGE = "package";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_ST = "st";
    public static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_ND = "nd";
    public static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_RD = "rd";
    public static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_TH = "th";
    public static final String DATE_SPLITTER = "-";

    // INTERFACES

    public interface OnStringResponseGivenCallback {
        void getResponse(String response);
    }

    public interface OnIntegerResultGivenCallback {
        void getResult(int result);
    }

    public interface OnMethodFinishCallback {
        void canContinue();
    }

    // METHODS

    public static String getThisUrl() { return THIS_URL; }
    public static UUID getThisUUID() { return THIS_UUID; }

    /**
     * {@code @param:}
     *  {@code @NonNull} Context context;
     * Return the unique device Id.
     */
    @SuppressLint("HardwareIds")
    public static String getThisDeviceID(@NonNull Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void openAppDetailsSetting(Context context) {
        Intent openAppSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts(URI_SCHEME_PACKAGE, context.getPackageName(), null);
        openAppSettingIntent.setData(uri);
        context.startActivity(openAppSettingIntent);
    }

    public static void showLoadingFragment(@NonNull View view, int mainFragmentId, int loadingFragmentId) {
        view.findViewById(loadingFragmentId).setVisibility(View.VISIBLE);
        if(mainFragmentId != NULL_INT_VALUE) {
            view.findViewById(mainFragmentId).setVisibility(View.GONE);
        }
    }

    public static void hideLoadingFragment(@NonNull View view, int mainFragmentId, int loadingFragmentId) {
        if(mainFragmentId != NULL_INT_VALUE) {
            view.findViewById(mainFragmentId).setVisibility(View.VISIBLE);
        }
        view.findViewById(loadingFragmentId).setVisibility(View.GONE);
    }

    public static void restartApp(@NonNull Context context) {
        Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if(restartIntent != null) {
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(restartIntent);
            Runtime.getRuntime().exit(0);
        }
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Activity activity;
     * To lock the layout.
     */
    public static void lockLayout(@NonNull Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} Activity activity;
     * To unlock the layout.
     */
    public static void unlockLayout(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    public static float dpToPx(@NonNull Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int getDaysAgo(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDate pastDate = LocalDateTime.parse(dateString, formatter).toLocalDate();  //  PARSE PARAMETER DATE
        LocalDate today = LocalDate.now();  //  PARSE CURRENT DATE
        return (int) ChronoUnit.DAYS.between(pastDate, today);
    }

    public static int getSpecificDay(String date) {   // FOR DATE FORMAT "YYYY-DD-MM"
        return Integer.parseInt(date.split(Common.DATE_SPLITTER)[2]);
    }

    public static int getSpecificMonth(String date) {   // FOR DATE FORMAT "YYYY-DD-MM"
        return Integer.parseInt(date.split(Common.DATE_SPLITTER)[1]);
    }

    public static int getSpecificYear(String date) {   // FOR DATE FORMAT "YYYY-DD-MM"
        return Integer.parseInt(date.split(Common.DATE_SPLITTER)[0]);
    }

    public static String concatDayEnglishLanguage(int intDay, String day) {
        switch (intDay) {
            case 1:
            case 21:
            case 31:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_ST);

            case 2:
            case 22:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_ND);

            case 3:
            case 23:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_RD);

            default:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_TH);
        }
    }

    public static void expandViewGradually(final @NonNull View view, long durationMillis) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        final int targetHeight = view.getMeasuredHeight();

        final int startHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                view.getContext().getResources().getDisplayMetrics()
        );

        view.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = startHeight;
        view.setLayoutParams(layoutParams);

        Animation expandAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                layoutParams.height = (interpolatedTime == 1)
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : startHeight + (int) ((targetHeight - startHeight) * interpolatedTime);
                view.setLayoutParams(layoutParams);
            }
        };
        expandAnimation.setDuration(durationMillis); // es. 500ms
        expandAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(expandAnimation);
    }

    public static void collapseViewGradually(final View view, long durationMillis) {
        final int initialHeight = view.getHeight(); //

        final int endHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                view.getContext().getResources().getDisplayMetrics()
        );
        final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        Animation collapseAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                layoutParams.height = (interpolatedTime == 1)
                        ? endHeight
                        : initialHeight - (int) ((initialHeight - endHeight) * interpolatedTime);
                view.setLayoutParams(layoutParams);
            }
        };
        collapseAnimation.setDuration(durationMillis); // es. 500ms
        collapseAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(collapseAnimation);
    }
}
