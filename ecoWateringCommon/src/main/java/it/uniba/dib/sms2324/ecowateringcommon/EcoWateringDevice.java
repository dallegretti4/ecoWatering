package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class EcoWateringDevice {
    public static final String TABLE_DEVICE_DEVICE_ID_COLUMN_NAME = "deviceID";
    public static final String TABLE_DEVICE_NAME_COLUMN_NAME = "name";
    public static final String DEVICE_NAME_CHANGED_RESPONSE = "deviceNameSuccessfulChanged";
    public static final String DEVICE_DELETE_ACCOUNT_RESPONSE = "deviceAccountSuccessfulDeleted";
    private String deviceID;
    private String name;
    private List<String> ecoWateringHubList;
    public interface OnResponseGivenCallback {
        void getResponse(String response);
    }
    public interface EcoWateringDeviceFromJsonStringCallback {
        void convert(String response);
    }
    public interface DeviceExistsCallback {
        void getResponse(String response);
    }
    public interface AddNewEcoWateringDeviceCallback {
        void canRestartApp();
    }

    public EcoWateringDevice(String jsonString) {
        try{
            JSONObject jsonOBJ = new JSONObject(jsonString);
            this.deviceID = jsonOBJ.getString(TABLE_DEVICE_DEVICE_ID_COLUMN_NAME);
            this.name = jsonOBJ.getString(TABLE_DEVICE_NAME_COLUMN_NAME);
            // REMOTE DEVICE LIST RECOVERING
            this.ecoWateringHubList = new ArrayList<>();
            String stringEcoWateringHubList = jsonOBJ.getString(Common.BO_DEVICE_HUB_LIST_COLUMN_NAME);
            if(!stringEcoWateringHubList.equals("") && !stringEcoWateringHubList.equals("null")) {
                JSONArray jsonRemoteDeviceList = new JSONArray(stringEcoWateringHubList);
                for(int i=0; i<jsonRemoteDeviceList.length(); i++) {
                    this.ecoWateringHubList.add(jsonRemoteDeviceList.getString(i));
                }
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  DeviceExistsCallback callback -> to avoid the caller to manage the response.
     * Get the json response from database server about specific EcoWateringDevice instance existence.
     */
    public static void exists(@NonNull String deviceID, DeviceExistsCallback callback) {
        String jsonString = "{\"" + TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"DEVICE_EXISTS\"}";
        Thread deviceExistsThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "deviceExists response: " + response);
            callback.getResponse(response);
        });
        deviceExistsThread.start();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  {@code @NonNull} String name;
     *  AddNewEcoWateringDeviceCallback callback -> to notify the caller, who can restart the app.
     * To add new EcoWateringDevice into the database server
     */
    public static void addNewEcoWateringDevice(@NonNull String deviceID, @NonNull String name, AddNewEcoWateringDeviceCallback callback) {
        String jsonString = "{\"" + TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"ADD_NEW_DEVICE\",\"" + TABLE_DEVICE_NAME_COLUMN_NAME + "\":\"" + name + "\"}";
        Thread addNewDeviceThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "addNewDevice response: " + response);
            callback.canRestartApp();
        });
        addNewDeviceThread.start();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  EcoWateringDeviceFromJsonStringCallback callback -> to avoid the caller to manage the response.
     * Get the json response from database server about specific EcoWateringDevice instance.
     */
    public static void getEcoWateringDeviceJsonString(@NonNull String deviceID, EcoWateringDeviceFromJsonStringCallback callback) {
        String jsonString = "{\"" + TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"GET_DEVICE_OBJ\"}";
        Thread getDeviceObjThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "getEWDeviceObj response: " + response);
            callback.convert(response);
        });
        getDeviceObjThread.start();
    }

    public void disconnectFromEWHub(@NonNull Context context, String hubID, OnResponseGivenCallback callback) {
        new Thread(() -> {
            String jsonString = "{\"deviceID\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"DISCONNECT_FROM_EWH\",\"HUB\":\"" + hubID + "\"}";
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            callback.getResponse(response);
        }).start();
    }

    public static void setName(String deviceID, String newName, OnResponseGivenCallback callback) {
        String jsonString = "{\"" + TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"SET_DEVICE_NAME\",\"NEW_NAME\":\"" + newName + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "setDeviceName response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void deleteAccount(String deviceID, OnResponseGivenCallback callback) {
        String jsonString = "{\"" + TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"DELETE_DEVICE_ACCOUNT\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "deleteDeviceAccount response: " + response);
            callback.getResponse(response);
        }).start();
    }

    /**
     * Return device's ID associated to the instance of the EcoWateringHub.
     */
    public String getDeviceID() {
        return this.deviceID;
    }

    /**
     * return the EcoWateringDevice name from the instance.
     */
    public String getName() {
        return this.name;
    }

    /**
     * return the EcoWateringHub list from the instance.
     */
    public List<String> getEcoWateringHubList() {
        return this.ecoWateringHubList;
    }

    @NonNull
    @Override
    public String toString() {
        return this.name + " - " + this.deviceID;
    }
}
