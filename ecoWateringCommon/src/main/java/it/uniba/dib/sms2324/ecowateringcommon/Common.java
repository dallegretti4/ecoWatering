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
    /**
     * max value -> 1031
     * max permission value -> 2007
     */
    public static final int ACTION_ADD_REMOTE_DEVICE = 1027;
    public static final int ACTION_BACK_PRESSED = 1023;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_RESTART_FRAGMENT = 1024;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_ADDED = 1026;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_IT_SELF_REMOVED = 1031;
    public static final int ACTION_REMOTE_DEVICES_CONNECTED_SUCCESS_REMOVED = 1025;
    public static final String BO_DEVICE_HUB_LIST_COLUMN_NAME = "ecoWateringHubList";
    public static final String BO_IRRIGATION_SYSTEM_COLUMN_NAME = "irrigationSystem";
    public static final String BO_HUB_CONFIGURATION_COLUMN_NAME = "ecoWateringHubConfiguration";
    public static final String BT_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    public static final String BT_ERROR_RESPONSE = "error";
    public static final int BT_ERROR_RESULT = 1010;
    public static final String BT_SERVER_SOCKET_REQUEST_NAME = "ecoWateringBtRequest";
    public static final int BT_ALREADY_CONNECTED_RESULT = 1005;
    public static final String BT_CONNECTED_RESPONSE = "remoteDeviceAdded";
    public static final int BT_CONNECTED_RESULT = 1004;
    public static final int BT_ENABLED_RESULT = 1009;
    public static final int BT_NOT_CONNECTED_RESULT = 1006;
    public static final int BT_NOT_ENABLED_RESULT = 1002;
    public static final int BT_NOT_SUPPORTED_RESULT = 1001;
    public static final int BT_RESTART_FRAGMENT_RESULT = 1003;
    public static final int BT_DISCOVERABLE_DURATION = 180;
    public static final int BT_PERMISSION_REQUEST = 2001;
    public static final String BUNDLE_EWH_LIST_KEY = "EWH_LIST";
    public static final String CALLED_FROM_HUB = "CALLED_FROM_HUB";
    public static final String CALLED_FROM_DEVICE = "CALLED_FROM_DEVICE";
    public static final String CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE = "ambient temperature sensor";
    public static final String CONFIGURE_SENSOR_TYPE_LIGHT = "light sensor";
    public static final String CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY = "relative humidity sensor";
    public static final String EWH_UPDATED_SUCCESS_RESPONSE = "ecoWateringHubUpdated";
    public static final String EW_SENSOR_ID_SEPARATOR = " - ";
    public static final int FIRST_BT_CONNECT_PERMISSION_REQUEST = 1016;
    public static final int FIRST_LOCATION_PERMISSION_REQUEST = 1015;
    public static final int FIRST_WIFI_PERMISSION_REQUEST = 1017;
    public static final String FIRST_START_FILENAME_FLAG = "isFirstStartCheck";
    public static final String FIRST_START_KEY_FLAG = "IS_FIRST_START";
    public static final String FIRST_START_VALUE_FLAG = "true";
    public static final String FIRST_START_KEY_NOT_FOUND_FLAG = "keyNotFound";
    public static final int GPS_WIFI_ENABLE_REQUEST = 2003;
    public static final int GPS_ENABLE_REQUEST = 2004;
    public static final int GPS_BT_ENABLE_REQUEST = 2006;
    public static final int GPS_ENABLED_RESULT = 1007;
    public static final String HTTP_RESPONSE_ERROR = "error";
    public static final String HTTP_RESPONSE_EXISTS_TRUE = "0";
    public static final String IRRIGATION_SYSTEM_STATE_ON_RESPONSE = "irrigationSystemSwitchedOn";
    public static final String IRRIGATION_SYSTEM_STATE_OFF_RESPONSE = "irrigationSystemSwitchedOff";
    public static final int LOCATION_PERMISSION_REQUEST = 2002;
    public static final String MANAGE_EWH_INTENT_OBJ = "MANAGE_EWH_OBJ";
    public static final int MAIN_FRAGMENT_RESTART_RESULT = 1021;
    public static final int MENU_ITEM_ADD_NEW_EWH = 1022;
    public static final int NULL_INT_VALUE = 7001;
    public static final int REFRESH_FRAGMENT = 1030;
    public static final int REFRESH_FRAGMENT_FROM_DEVICE_INTERVAL = 5 * 1000;
    public static final int REFRESH_FRAGMENT_FROM_HUB_INTERVAL = 5 * 1000;
    public static final String REMOVE_REMOTE_DEVICE_RESPONSE = "remoteDeviceRemoved";
    public static final String SENSOR_ADDED_SUCCESSFULLY_RESPONSE = "sensorAddedSuccessfully";
    public static final String TABLE_CONFIGURATION_IS_AUTOMATED_COLUMN_NAME = "isAutomated";
    public static final String TABLE_CONFIGURATION_IS_CONFIGURED_COLUMN_NAME = "isConfigured";
    public static final String TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME = "ambientTemperatureSensor";
    public static final String TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME = "lightSensor";
    public static final String TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME = "relativeHumiditySensor";
    public static final String THIS_LOG = "MY_LOG";
    private static final String THIS_URL = "https://theall.altervista.org/sms2324";
    private static final UUID THIS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    public static final int WIFI_ALREADY_CONNECTED_DEVICE_RESULT = 1018;
    public static final String WIFI_CONNECTED_RESPONSE = "remoteDeviceAdded";
    public static final int WIFI_CONNECTED_RESULT = 1019;
    public static final String WIFI_ERROR_RESPONSE = "error";
    public static final int WIFI_ERROR_RESULT = 1020;
    public static final int WIFI_GROUP_CREATED_SUCCESS_RESULT = 1013;
    public static final int WIFI_GROUP_CREATED_FAILURE_RESULT = 1014;
    public static final int WIFI_NOT_SUPPORTED_RESULT = 1011;
    public static final int WIFI_PERMISSION_REQUEST = 2005;
    public static final int WIFI_RESTART_DISCOVERY_RESULT = 2007;
    public static final int WIFI_RESTART_FRAGMENT_RESULT = 1012;
    public static final String WIFI_SOCKET_REQUEST_NAME = "ecoWateringWiFiRequest";

    /**
        METHODS
     */
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
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
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
