package it.uniba.dib.sms2324.ecowateringcommon.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class SharedPreferencesHelper {
    public static final String FIRST_START_FLAG_FILE_NAME = "IS_FIRST_START_CHECK";
    public static final String FIRST_START_FLAG_VALUE_KEY = "IS_FIRST_START";
    public static final String FIRST_START_VALUE_FLAG = "true";
    public static final String LOCATION_PERMISSION_FIRST_DIALOG_FILE_NAME = "LOCATION_PERMISSION_FIRST_DIALOG";
    public static final String LOCATION_PERMISSION_FIRST_DIALOG_VALUE_KEY = "LOCATION_PERMISSION_FIRST_DIALOG_VALUE_KEY";
    public static final String IS_USER_RETURNED_FROM_SETTING_FILE_NAME = "IS_USER_RETURNED_FROM_SETTING";
    public static final String IS_USER_RETURNED_FROM_SETTING_VALUE_KEY = "IS_USER_RETURNED_FROM_SETTING_VALUE_KEY";
    public static final String IRR_SYS_START_DELAYED_FILENAME = "IRR_SYS_START_DELAYED_FILENAME";
    public static final String IRR_SYS_START_DELAYED_KEY = "IRR_SYS_START_DELAYED_KEY";
    public static final String SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME = "SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_FILENAME";
    public static final String SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY = "SENSOR_CONFIGURATION_FRAGMENT_IS_REFRESHING_KEY";
    public static final String BT_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME = "BT_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME";
    public static final String BT_CONNECTION_FRAGMENT_IS_REFRESHING_KEY = "BT_CONNECTION_FRAGMENT_IS_REFRESHING_KEY";
    public static final String WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME = "WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_FILENAME";
    public static final String WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_KEY = "WIFI_CONNECTION_FRAGMENT_IS_REFRESHING_KEY";
    public static final String CONNECTION_CHOOSER_FRAGMENT_IS_REFRESHING_FILENAME = "CONNECTION_CHOOSER_FRAGMENT_IS_REFRESHING_FILENAME";
    public static final String CONNECTION_CHOOSER_FRAGMENT_IS_REFRESHING_KEY = "CONNECTION_CHOOSER_FRAGMENT_IS_REFRESHING_KEY";
    @SuppressLint("ApplySharedPref")
    public static void writeStringOnSharedPreferences(@NonNull Context context, String fileName, String keyValue, String stringValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(keyValue, stringValue);
        editor.commit();
        Log.i(Common.LOG_SHARED_PREFERENCES, "write String on " + fileName + " - VALUE: " + stringValue);
    }

    @SuppressLint("ApplySharedPref")
    public static void writeBooleanOnSharedPreferences(@NonNull Context context, String fileName, String keyValue, boolean booleanValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(keyValue, booleanValue);
        editor.commit();
        Log.i(Common.LOG_SHARED_PREFERENCES, "write Boolean on " + fileName + " - VALUE: " + booleanValue);
    }

    public static String readStringFromSharedPreferences(@NonNull Context context, String fileName, String keyValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        String returnString = sharedPreferences.getString(keyValue, Common.NULL_STRING_VALUE);
        Log.i(Common.LOG_SHARED_PREFERENCES, "read String from " + fileName + " - VALUE: " + returnString);
        return returnString;
    }

    public static boolean readBooleanFromSharedPreferences(@NonNull Context context, String fileName, String keyValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        boolean returnValue = sharedPreferences.getBoolean(keyValue, false);
        Log.i(Common.LOG_SHARED_PREFERENCES, "read String from " + fileName + " - VALUE: " + returnValue);
        return returnValue;
    }
}
