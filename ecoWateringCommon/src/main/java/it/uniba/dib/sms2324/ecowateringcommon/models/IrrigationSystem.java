package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;

public class IrrigationSystem implements Parcelable {
    private static final String IRRIGATION_SYSTEM_SIMULATION_MODEL_NAME = "DALL - Irrigation System - SIMULATED";
    private static final String IRRIGATION_SYSTEM_STATE_ON_RESPONSE = "irrigationSystemSwitchedOn";
    private static final String STATE_TRUE_VALUE = "1";
    private static final String TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME = "id";
    public static final String TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME = "model";
    private static final String TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME = "state";
    private String model;
    private boolean state;

    public interface OnChangeStateCallback {
        void onStateChanged(boolean requestedState, boolean outcome);
    }

    // CONSTRUCTOR
    public IrrigationSystem(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            // MODEL RECOVERING
            this.model = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME);
            // STATE RECOVERING
            String tmpString = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME) ;
            this.state = tmpString.equals(STATE_TRUE_VALUE);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    // ONLY FOR SIMULATION
    private IrrigationSystem(String model, boolean isSimulation) {
        if(isSimulation) {
            this.model = IRRIGATION_SYSTEM_SIMULATION_MODEL_NAME;
        }
        else {
            this.model = model;
        }
    }
    public String getModel() {
        return this.model;
    }

    public boolean getState() {
        return this.state;
    }

    public static IrrigationSystem discoverIrrigationSystem(boolean isSimulatedMode) {
        if(isSimulatedMode) {
            return new IrrigationSystem(Common.VOID_STRING_VALUE, true);
        }
        else {
            // TO-DO edit ecoWateringHub.setup.StartSecondFragment when method is implemented
            return null;
        }
    }

    /**
     *
     * @param deviceID from the caller device;
     * @param irrigationSystemID from the irrigation System;
     * @param state true to switch on the irrigation system, false to switch off;
     * @param callback to manage the HTTP response.
     */
    public void setState(String deviceID, String irrigationSystemID, boolean state, OnChangeStateCallback callback) {
        // CONVERT STATE
        int stateInt = 0;
        if(state) {
            stateInt = 1;
        }
        String jsonString = "{\"" +
                EcoWateringDevice.TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_SET_IRRIGATION_SYSTEM_STATE + "\",\"" +
                TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME + "\":\"" + irrigationSystemID + "\",\"" +
                TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME + "\":\"" + stateInt + "\"}";
        Thread changeStateThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "irrSysChangeState response: " + response);
            if(response != null) {
                if(state && response.equals(IRRIGATION_SYSTEM_STATE_ON_RESPONSE)) {
                    this.state = true;
                    callback.onStateChanged(true, true);
                }
                else if(!state && response.equals(HttpHelper.IRRIGATION_SYSTEM_STATE_OFF_RESPONSE)) {
                    this.state = false;
                    callback.onStateChanged(false, true);
                }
                else {
                    callback.onStateChanged(false, false);
                }
            }
        });
        changeStateThread.start();
    }

    @NonNull
    @Override
    public String toString() {
        return TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME + ": " + this.model + ", " + TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME  + ": " + this.state;
    }

    // PARCELABLE IMPLEMENTATION
    protected IrrigationSystem(Parcel in) {
        model = in.readString();
        state = in.readByte() == 1;
    }

    public static final Creator<IrrigationSystem> CREATOR = new Creator<IrrigationSystem>() {
        @Override
        public IrrigationSystem createFromParcel(Parcel in) {
            return new IrrigationSystem(in);
        }

        @Override
        public IrrigationSystem[] newArray(int size) {
            return new IrrigationSystem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(model);
        parcel.writeByte((byte) (state ? 1 : 0));
    }
}
