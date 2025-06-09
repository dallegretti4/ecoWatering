package it.uniba.dib.sms2324.ecowateringcommon.helpers;

import android.content.ContentValues;
import android.util.Log;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class SqlDbHelper {
    private static final String LOG_DB = "DATABASE_LOG";

    // ECO WATERING HUB
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

    // IRRIGATION SYSTEM
    public static final String TABLE_IRR_SYS_ID_COLUMN_NAME = "id";
    public static final String TABLE_IRR_SYS_MODEL_COLUMN_NAME = "model";
    public static final String TABLE_IRR_SYS_STATE_COLUMN_NAME = "state";
    public static final String TABLE_IRR_SYS_ACTIVITY_LOG_COLUMN_NAME = "activityLog";

    // SENSORS INFO
    public static final String TABLE_SENSORS_INFO_ID_COLUMN_NAME = "id";
    public static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_LIST_COLUMN_NAME = "ambientTemperatureSensorList";
    public static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_CHOSEN_SENSOR_COLUMN_NAME = "ambientTemperatureChosenSensor";
    public static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_VALUE_COLUMN_NAME = "ambientTemperatureSensorValue";
    public static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME = "ambientTemperatureLastUpdate";
    public static final String TABLE_SENSORS_INFO_LIGHT_SENSOR_LIST_COLUMN_NAME = "lightSensorList";
    public static final String TABLE_SENSORS_INFO_LIGHT_CHOSEN_SENSOR_COLUMN_NAME = "lightChosenSensor";
    public static final String TABLE_SENSORS_INFO_LIGHT_SENSOR_VALUE_COLUMN_NAME = "lightSensorValue";
    public static final String TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME = "lightLastUpdate";
    public static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_LIST_COLUMN_NAME = "relativeHumiditySensorList";
    public static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_CHOSEN_SENSOR_COLUMN_NAME = "relativeHumidityChosenSensor";
    public static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_VALUE_COLUMN_NAME = "relativeHumiditySensorValue";
    public static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME = "relativeHumidityLastUpdate";
    // DEVICE REQUEST
    public static final String TABLE_DEVICE_REQUEST_ID_COLUMN_NAME = "id";
    public static final String TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME = "caller";
    public static final String TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME = "request";
    public static final String TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME = "date";


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
                .append(TABLE_IRR_SYS_MODEL_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_IRR_SYS_MODEL_COLUMN_NAME)).append("\"}");
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

    // IRRIGATION SYSTEM METHODS

    public static void setState(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SET_IRRIGATION_SYSTEM_STATE).append("\",\"")
                .append(TABLE_IRR_SYS_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_IRR_SYS_ID_COLUMN_NAME)).append("\",\"")
                .append(TABLE_IRR_SYS_STATE_COLUMN_NAME).append("\":").append(contentValues.get(TABLE_IRR_SYS_STATE_COLUMN_NAME)).append("}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "setIrrSysState response: " + response);
            callback.getResponse(response);
        }).start();
    }

    // SENSORS INFO METHODS
    public static void addNewSensor(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_SENSORS_INFO_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_SENSORS_INFO_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_ADD_NEW_SENSOR).append("\",\"")
                .append(HttpHelper.REMOTE_DEVICE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER)).append("\",\"")
                .append(HttpHelper.SENSOR_TYPE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.SENSOR_TYPE_PARAMETER)).append("\",\"")
                .append(HttpHelper.SENSOR_ID_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.SENSOR_ID_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "addNewSensor response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void updateSensorList(@NonNull ContentValues contentValues) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_SENSORS_INFO_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_SENSORS_INFO_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_UPDATE_SENSOR_LIST).append("\",\"")
                .append(HttpHelper.REMOTE_DEVICE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER)).append("\",\"")
                .append(HttpHelper.SENSOR_TYPE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.SENSOR_TYPE_PARAMETER)).append("\",\"")
                .append(HttpHelper.VALUE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.VALUE_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "updateSensorList response: " + response);
        }).start();
    }

    public static void detachSelectedSensor(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_DETACH_SENSOR).append("\",\"")
                .append(HttpHelper.REMOTE_DEVICE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER)).append("\",\"")
                .append(HttpHelper.SENSOR_TYPE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.SENSOR_TYPE_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "detachSelectedSensor response: " + response);
            callback.getResponse(response);
        }).start();
    }

    // WEATHER INFO METHODS
    public static void updateWeatherInfo(@NonNull ContentValues contentValues) {
        String string = "{\"" + TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_UPDATE_WEATHER_INFO + "\",\"" +
                TABLE_HUB_LATITUDE_COLUMN_NAME + "\":" + contentValues.get(TABLE_HUB_LATITUDE_COLUMN_NAME) + ",\"" +
                TABLE_HUB_LONGITUDE_COLUMN_NAME + "\":" + contentValues.get(TABLE_HUB_LONGITUDE_COLUMN_NAME) + "}";
        String response = HttpHelper.sendHttpPostRequest(string);    // BLOCKER
        Log.i(LOG_DB, "updateWeatherInfo response: " + response);
    }

    // DEVICE REQUEST METHODS
    public static void getDeviceRequestFromServer(@NonNull ContentValues contentValues, Common.OnStringResponseGivenCallback callback) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_GET_DEVICE_REQUEST).append("\"}");
        Log.i(LOG_DB, stringBuilder.toString());
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "getDeviceRequestFromServer response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void sendRequest(@NonNull ContentValues contentValues) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_SEND_REQUEST).append("\",\"")
                .append(HttpHelper.REMOTE_DEVICE_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REMOTE_DEVICE_PARAMETER)).append("\",\"")
                .append(HttpHelper.REQUEST_PARAMETER).append("\":\"").append(contentValues.get(HttpHelper.REQUEST_PARAMETER)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "sendRequest response: " + response);
        }).start();
    }

    public static void deleteDeviceRequest(@NonNull ContentValues contentValues) {
        StringBuilder stringBuilder = new StringBuilder("{\"");
        stringBuilder.append(TABLE_HUB_DEVICE_ID_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_HUB_DEVICE_ID_COLUMN_NAME)).append("\",\"")
                .append(HttpHelper.MODE_PARAMETER).append("\":\"").append(HttpHelper.MODE_DELETE_DEVICE_REQUEST).append("\",\"")
                .append(TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME)).append("\",\"")
                .append(TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME)).append("\",\"")
                .append(TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME).append("\":\"").append(contentValues.get(TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME)).append("\"}");
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(stringBuilder.toString());
            Log.i(LOG_DB, "deleteDeviceRequest response: " + response);
        }).start();
    }
}
