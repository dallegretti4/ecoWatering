package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlan;

public class IrrigationSystem implements Parcelable {
    private static final String IRRIGATION_SYSTEM_SIMULATION_MODEL_NAME = "DALL - Irrigation System - SIMULATED";
    private static final String IRRIGATION_SYSTEM_STATE_ON_RESPONSE = "irrigationSystemSwitchedOn";
    private static final String IRRIGATION_SYSTEM_STATE_OFF_RESPONSE = "irrigationSystemSwitchedOff";
    private static final String STATE_TRUE_VALUE = "1";
    private static final String TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME = "id";
    public static final String TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME = "model";
    private static final String TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME = "state";
    private static final String TABLE_IRRIGATION_SYSTEM_ACTIVITY_LOG_COLUMN_NAME = "activityLog";
    private String model;
    private boolean state;
    private ArrayList<IrrigationSystemActivityLogInstance> activityLog;

    // CONSTRUCTOR
    public IrrigationSystem(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            // MODEL RECOVERING
            this.model = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME);
            // STATE RECOVERING
            if(!jsonOBJ.isNull(TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME)) this.state = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME).equals(STATE_TRUE_VALUE);
            else this.state = false;
            // ACTIVITY LOG RECOVERING
            this.activityLog = new ArrayList<>();
            if(!jsonOBJ.isNull(TABLE_IRRIGATION_SYSTEM_ACTIVITY_LOG_COLUMN_NAME)) {
                String tmpString = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_ACTIVITY_LOG_COLUMN_NAME);
                JSONArray jsonArray = new JSONArray(tmpString);
                for(int i=0; i<jsonArray.length(); i++) {
                    this.activityLog.add(new IrrigationSystemActivityLogInstance(jsonArray.getString(i)));
                }
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    // ONLY FOR SIMULATION
    private IrrigationSystem(String model, boolean isSimulation) {
        if(isSimulation) this.model = IRRIGATION_SYSTEM_SIMULATION_MODEL_NAME;
        else this.model = model;
    }

    public String getModel() {
        return this.model;
    }

    public boolean getState() {
        return this.state;
    }

    public static IrrigationSystem discoverIrrigationSystem(boolean isSimulatedMode) {
        if(isSimulatedMode) return new IrrigationSystem(Common.VOID_STRING_VALUE, true);
        else {
            // TO-DO edit ecoWateringHub.setup.StartSecondFragment when method is implemented
            return null;
        }
    }

    public ArrayList<IrrigationSystemActivityLogInstance> getActivityLog() {
        return this.activityLog;
    }

    public int getIrrigationSavedMinutes() {
        int minutesCount = 0;
        for(IrrigationSystemActivityLogInstance log : this.activityLog) {
            minutesCount += IrrigationPlan.BASE_DAILY_IRRIGATION_MINUTES - log.getMinutes();
        }
        return Math.max(minutesCount, 0);
    }

    /**
     *
     * @param deviceID from the caller device;
     * @param irrigationSystemID from the irrigation System;
     * @param state true to switch on the irrigation system, false to switch off;
     */
    public void setState(String deviceID, String irrigationSystemID, boolean state) {
        // CONVERT STATE
        int stateInt = 0;
        if(state) {
            stateInt = 1;
        }
        String jsonString = "{\"" +
                EcoWateringDevice.TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_SET_IRRIGATION_SYSTEM_STATE + "\",\"" +
                TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME + "\":\"" + irrigationSystemID + "\",\"" +
                TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME + "\":" + stateInt + "}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.LOG_NORMAL, "irrSysChangeState response: " + response);
            if(response != null) {
                if(state && response.equals(IRRIGATION_SYSTEM_STATE_ON_RESPONSE)) this.state = true;
                else if(!state && response.equals(IRRIGATION_SYSTEM_STATE_OFF_RESPONSE)) this.state = false;
            }
        }).start();
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
