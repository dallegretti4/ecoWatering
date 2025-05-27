package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class DeviceRequest {
    public static final String TABLE_DEVICE_REQUEST_ID_COLUMN_NAME = "id";
    public static final String TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME = "caller";
    public static final String TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME = "request";
    public static final String TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME = "date";
    public static final String REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM = "SWITCH_OFF_IRRIGATION_SYSTEM";
    public static final String REQUEST_SWITCH_ON_IRRIGATION_SYSTEM = "SWITCH_ON_IRRIGATION_SYSTEM";
    public static final String REQUEST_FORCE_SENSORS_UPDATE = "FORCE_SENSORS_UPDATE";
    public static final String SUCCESS_RESPONSE_FORCE_SENSORS_UPDATE = "sensorsUpdateForced";
    public static final String REQUEST_START_DATA_OBJECT_REFRESHING = "START_DATA_OBJECT_REFRESHING";
    public static final String SUCCESS_RESPONSE_START_BACKGROUND_REFRESHING = "backgroundRefreshingStarted";
    public static final String REQUEST_STOP_DATA_OBJECT_REFRESHING = "STOP_DATA_OBJECT_REFRESHING";
    public static final String SUCCESS_RESPONSE_STOP_BACKGROUND_REFRESHING = "backgroundRefreshingStopped";
    private final String id;
    private final String caller;
    private final String request;
    private final String date;
    private DeviceRequest(String id, String caller, String request, String date) {
        this.id = id;
        this.caller = caller;
        this.request = request;
        this.date = date;
    }

    public static ArrayList<DeviceRequest> getDeviceRequestList(String jsonString) {
        if((jsonString != null) && (!jsonString.equals(Common.NULL_STRING_VALUE)) && (!jsonString.equals(HttpHelper.HTTP_RESPONSE_ERROR))) {
            ArrayList<DeviceRequest> returnArray = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for(int i=0; i<jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    returnArray.add(new DeviceRequest(
                            jsonObject.getString(TABLE_DEVICE_REQUEST_ID_COLUMN_NAME),
                            jsonObject.getString(TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME),
                            jsonObject.getString(TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME),
                            jsonObject.getString(TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME)
                    ));
                }
                return returnArray;
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void getDeviceRequestFromServer(String hubID, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_GET_DEVICE_REQUEST + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "getDeviceRequest response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void sendRequest(String hubID, String caller, String request) {
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubID + "\",\"" +
                HttpHelper.REMOTE_DEVICE_PARAMETER + "\":\"" + caller + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_SEND_REQUEST + "\",\"" +
                HttpHelper.REQUEST_PARAMETER + "\":\"" + request + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "sendRequest response: " + response);
        }).start();
    }

    public void delete() {
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + this.id + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_DELETE_DEVICE_REQUEST + "\",\"" +
                TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME + "\":\"" + this.caller + "\",\"" +
                TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME + "\":\"" + this.request + "\",\"" +
                TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME + "\":\"" + this.date + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "deleteDeviceRequest response: " + response);
        }).start();
    }

    public String getCaller() {
        return this.caller;
    }
    public String getRequest() {
        return this.request;
    }

    public boolean isValidDeviceRequest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Common.DATE_FORMAT_STRING); // DATE PARSER SETUP
        LocalDateTime inputDateTime = LocalDateTime.parse(this.date, formatter);
        LocalDateTime currentDate = LocalDateTime.now(ZoneId.systemDefault());
        return ((Duration.between(inputDateTime, currentDate).getSeconds() >= 0) && (Duration.between(inputDateTime, currentDate).getSeconds() <= 30));
    }
}
