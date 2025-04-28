package it.uniba.dib.sms2324.ecowateringcommon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.UUID;

public class Common {
    public static final int ACTION_BACK_PRESSED = 1023;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_RESTART_FRAGMENT = 1024;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED = 1025;
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

    public static final String THIS_LOG = "MY_LOG";
    private static final String THIS_URL = "https://theall.altervista.org/sms2324";
    private static final UUID THIS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String VOID_STRING_VALUE = "";
    public static final int WIFI_PERMISSION_REQUEST = 2005;
    public static final String WIFI_SOCKET_REQUEST_NAME = "ecoWateringWiFiRequest";
    private static final String URI_SCHEME_PACKAGE = "package";

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

    /**
     * {@code @param:}
     *  {@code @NonNull} Activity activity;
     * To lock the layout.
     */
    public static void lockLayout(@NonNull Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
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
     * To unlock the layout.
     */
    public static void unlockLayout(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }
}
