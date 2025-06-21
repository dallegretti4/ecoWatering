package it.uniba.dib.sms2324.ecowateringcommon.models.device;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;

public class EcoWateringDevice implements Parcelable {
    private static final String BO_DEVICE_HUB_LIST_COLUMN_NAME = "ecoWateringHubList";
    private static final String TABLE_DEVICE_DEVICE_ID_COLUMN_NAME = "deviceID";
    private static final String TABLE_DEVICE_NAME_COLUMN_NAME = "name";
    public static final String DEVICE_NAME_CHANGED_RESPONSE = "deviceNameSuccessfulChanged";
    public static final String DEVICE_DELETE_ACCOUNT_RESPONSE = "deviceAccountSuccessfulDeleted";
    private String deviceID;
    private String name;
    private List<String> ecoWateringHubList;

    public EcoWateringDevice(String jsonString) {
        try{
            JSONObject jsonOBJ = new JSONObject(jsonString);
            this.deviceID = jsonOBJ.getString(TABLE_DEVICE_DEVICE_ID_COLUMN_NAME);
            this.name = jsonOBJ.getString(TABLE_DEVICE_NAME_COLUMN_NAME);
            // REMOTE DEVICE LIST RECOVERING
            this.ecoWateringHubList = new ArrayList<>();
            String stringEcoWateringHubList = jsonOBJ.getString(BO_DEVICE_HUB_LIST_COLUMN_NAME);
            if(!stringEcoWateringHubList.equals(Common.VOID_STRING_VALUE) && !stringEcoWateringHubList.equals(Common.NULL_STRING_VALUE)) {
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

    public static void exists(@NonNull String deviceID, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_DEVICE_EXISTS + "\"}";
        Thread deviceExistsThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(Common.LOG_NORMAL, "deviceExists response: " + response);
            callback.getResponse(response);
        });
        deviceExistsThread.start();
    }

    public static void addNewEcoWateringDevice(@NonNull Context context, @NonNull String name, Common.OnMethodFinishCallback callback) {
        String jsonString = "{\"" +
                TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_ADD_NEW_DEVICE + "\",\"" +
                TABLE_DEVICE_NAME_COLUMN_NAME + "\":\"" + name + "\"}";
        Thread addNewDeviceThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(Common.LOG_NORMAL, "addNewDevice response: " + response);
            callback.canContinue();
        });
        addNewDeviceThread.start();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  EcoWateringDeviceFromJsonStringCallback callback -> to avoid the caller to manage the response.
     * Get the json response from database server about specific EcoWateringDevice instance.
     */
    public static void getEcoWateringDeviceJsonString(@NonNull String deviceID, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_GET_DEVICE_OBJ + "\"}";
        Thread getDeviceObjThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(Common.LOG_NORMAL, "getEWDeviceObj response: " + response);
            callback.getResponse(response);
        });
        getDeviceObjThread.start();
    }

    public void disconnectFromEWHub(@NonNull Context context, String hubID, Common.OnStringResponseGivenCallback callback) {
        new Thread(() -> {
            String jsonString = "{\"" +
                    TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                    HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_DISCONNECT_FROM_EWH + "\",\"" +
                    HttpHelper.HUB_PARAMETER + "\":\"" + hubID + "\"}";
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            callback.getResponse(response);
        }).start();
    }

    public static void setName(@NonNull Context context, String newName, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_SET_DEVICE_NAME + "\",\"" +
                HttpHelper.NEW_NAME_PARAMETER + "\":\"" + newName + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(Common.LOG_NORMAL, "setDeviceName response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public static void deleteAccount(@NonNull Context context, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_DELETE_DEVICE_ACCOUNT + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(Common.LOG_NORMAL, "deleteDeviceAccount response: " + response);
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


    // PARCELABLE IMPLEMENTATION

    protected EcoWateringDevice(Parcel in) {
        deviceID = in.readString();
        name = in.readString();
        ecoWateringHubList = in.createStringArrayList();
    }

    public static final Creator<EcoWateringDevice> CREATOR = new Creator<EcoWateringDevice>() {
        @Override
        public EcoWateringDevice createFromParcel(Parcel in) {
            return new EcoWateringDevice(in);
        }

        @Override
        public EcoWateringDevice[] newArray(int size) {
            return new EcoWateringDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(deviceID);
        dest.writeString(name);
        dest.writeStringList(ecoWateringHubList);
    }
}
