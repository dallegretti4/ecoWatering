package it.uniba.dib.sms2324.ecowateringcommon.helpers;

import android.content.Context;
import android.content.SharedPreferences;

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
    public static void writeStringOnSharedPreferences(@NonNull Context context, String fileName, String keyValue, String stringValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(keyValue, stringValue);
        editor.apply();
    }

    public static void writeBooleanOnSharedPreferences(@NonNull Context context, String fileName, String keyValue, boolean booleanValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(keyValue, booleanValue);
        editor.apply();
    }

    public static String readStringOnSharedPreferences(@NonNull Context context, String fileName, String keyValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sharedPreferences.getString(keyValue, Common.NULL_STRING_VALUE);
    }

    public static boolean readBooleanOnSharedPreferences(@NonNull Context context, String fileName, String keyValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(keyValue, false);
    }
}
