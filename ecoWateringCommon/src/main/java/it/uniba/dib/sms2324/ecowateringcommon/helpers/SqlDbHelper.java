package it.uniba.dib.sms2324.ecowateringcommon.helpers;

import android.content.ContentValues;
import android.util.Log;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class SqlDbHelper {
    private static final String LOG_DB = "DATABASE_LOG";
    public static final String TABLE_HUB_TABLE_NAME = "EcoWateringHub";
    public static final String TABLE_HUB_DEVICE_ID_COLUMN_NAME = "deviceID";
    public static final String TABLE_HUB_NAME_COLUMN_NAME = "name";
    public static final String TABLE_HUB_ADDRESS_COLUMN_NAME = "address";
    public static final String TABLE_HUB_CITY_COLUMN_NAME = "city";
    public static final String TABLE_HUB_COUNTRY_COLUMN_NAME = "country";
    public static final String TABLE_HUB_LATITUDE_COLUMN_NAME = "latitude";
    public static final String TABLE_HUB_LONGITUDE_COLUMN_NAME = "longitude";
    public static final String TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME = "remoteDeviceList";
    public static final String TABLE_HUB_IS_AUTOMATED_COLUMN_NAME = "isAutomated";
    public static final String TABLE_HUB_IS_DATA_OBJECT_REFRESHING_COLUMN_NAME = "isDataObjectRefreshing";
    public static final String TABLE_HUB_BATTERY_PERCENT_COLUMN_NAME = "batteryPercent";

    public static final String TABLE_IRR_SYS_TABLE_NAME = "IrrigationSystem";
    public static final String TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME = "model";


    // HUB METHODS

    public static void addNewEcoWateringHub(@NonNull ContentValues contentValues, Common.OnMethodFinishCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(TABLE_HUB_NAME_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_NAME_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_ADD_NEW_HUB).append("\",\"")
                .append(TABLE_HUB_ADDRESS_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_ADDRESS_COLUMN_NAME)).append("\",\"")
                .append(TABLE_HUB_CITY_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_CITY_COLUMN_NAME)).append("\",\"")
                .append(TABLE_HUB_COUNTRY_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_COUNTRY_COLUMN_NAME)).append("\",\"")
                .append(TABLE_HUB_LATITUDE_COLUMN_NAME).append("\":").append(contentValues.get(TABLE_HUB_LATITUDE_COLUMN_NAME)).append(",\"")
                .append(TABLE_HUB_LONGITUDE_COLUMN_NAME).append("\":").append(contentValues.get(TABLE_HUB_LONGITUDE_COLUMN_NAME)).append(",\"")
                .append(TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            callback.canContinue();
            Log.i(LOG_DB, "addNewEcoWateringHub response: " + response);
        }).start();
    }

    public static void getEcoWateringHub(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_GET_HUB_OBJ).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "getEcoWateringHub response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static String addNewRemoteDevice(@NonNull ContentValues contentValues) {
        String stringBuilder = "{\"" + TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_ADD_REMOTE_DEVICE + "\",\"" +
                HttpHelper.REMOTE_DEVICE_PARAMETER + "\":\"" + contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER) + "\"}";
        String response = HttpHelper.sendHttpPostRequest(stringBuilder);
        Log.i(LOG_DB, "addNewRemoteDevice response: " + response);
        return response;
    }

    public static void removeRemoteDevice(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_REMOVE_REMOTE_DEVICE).append("\",\"")
                .append(HttpHelper.REMOTE_DEVICE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "removeRemoteDevice response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void setName(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SET_HUB_NAME).append("\",\"")
                .append(HttpHelper.NEW_NAME_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.NEW_NAME_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "setHubName response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void setIsAutomated(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SET_IS_AUTOMATED).append("\",\"")
                .append(HttpHelper.VALUE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.VALUE_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "setIsAutomated response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void setIsDataObjectRefreshing(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SET_IS_DATA_OBJECT_REFRESHING).append("\",\"")
                .append(HttpHelper.VALUE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.VALUE_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "setIsDataObjectRefreshing response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void setBatteryPercent(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SET_BATTERY_PERCENT).append("\",\"")
                .append(HttpHelper.VALUE_PARAMETER).append("\":").append(contentValues.get(HttpHelper.VALUE_PARAMETER)).append("}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "setBatteryPercent response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void deleteAccount(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_DELETE_HUB_ACCOUNT).append("\"}");
        Log.i(LOG_DB, stringBuilder.toString());
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "deleteAccount response: " + response);
            callback.getResponse(response);
        }).start();
    }
}
