package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;

public class DeviceRequest {
    public static final String REQUEST_PARAMETER_DIVISOR = "::";
    public static final String REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM = "SWITCH_OFF_IRRIGATION_SYSTEM";
    public static final String REQUEST_SWITCH_ON_IRRIGATION_SYSTEM = "SWITCH_ON_IRRIGATION_SYSTEM";
    public static final String REQUEST_ENABLE_AUTOMATE_SYSTEM = "ENABLE_AUTOMATE_SYSTEM";
    public static final String REQUEST_DISABLE_AUTOMATE_SYSTEM = "DISABLE_AUTOMATE_SYSTEM";
    public static final String REQUEST_START_DATA_OBJECT_REFRESHING = "START_DATA_OBJECT_REFRESHING";
    public static final String REQUEST_STOP_DATA_OBJECT_REFRESHING = "STOP_DATA_OBJECT_REFRESHING";
    public static final String REQUEST_SCHEDULE_IRR_SYS = "REQUEST_SCHEDULE_IRR_SYS";
    public static final String STARTING_DATE_PARAMETER = "startingDate";
    public static final String STARTING_TIME_PARAMETER = "startingTime";
    public static final String IRRIGATION_DURATION_PARAMETER = "irrigationDuration";
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
                            jsonObject.getString(SqlDbHelper.TABLE_DEVICE_REQUEST_ID_COLUMN_NAME),
                            jsonObject.getString(SqlDbHelper.TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME),
                            jsonObject.getString(SqlDbHelper.TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME).replace("\"", "\\\""),
                            jsonObject.getString(SqlDbHelper.TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME)
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

    public static void getDeviceRequestFromServer(@NonNull Context context, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        SqlDbHelper.getDeviceRequestFromServer(contentValues, (callback));
    }

    public static void sendRequest(@NonNull Context context, String hubID, String request) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, hubID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.REQUEST_PARAMETER, request);
        SqlDbHelper.sendRequest(contentValues);
    }

    public void delete() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, this.id);
        contentValues.put(SqlDbHelper.TABLE_DEVICE_REQUEST_CALLER_COLUMN_NAME, this.caller);
        contentValues.put(SqlDbHelper.TABLE_DEVICE_REQUEST_REQUEST_COLUMN_NAME, this.request);
        contentValues.put(SqlDbHelper.TABLE_DEVICE_REQUEST_DATE_COLUMN_NAME, this.date);
        SqlDbHelper.deleteDeviceRequest(contentValues);
    }

    public String getCaller() {
        return this.caller;
    }
    public String getRequest() {
        return this.request;
    }

    public boolean isValidDeviceRequest() {
        String[] tmpDateArray = this.date.split("T");
        String[] dateArray = tmpDateArray[0].split("-");
        String[] timeArray = tmpDateArray[1].split(":");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.parseInt(dateArray[0]));
        calendar.set(Calendar.MONTH, Integer.parseInt(dateArray[1]));
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateArray[2]));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeArray[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeArray[1]));
        boolean returnValue = (((System.currentTimeMillis() - calendar.getTimeInMillis()) < (15 * 1000)));
        Log.i(Common.LOG_SERVICE, "-----> inputDate: " + returnValue);
        return returnValue;
    }
}
