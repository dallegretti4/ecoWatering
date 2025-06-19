package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlan;

public class IrrigationSystem implements Parcelable {
    private static final String IRRIGATION_SYSTEM_SIMULATION_MODEL_NAME = "DALL - Irrigation System - SIMULATED";
    private static final String IRRIGATION_SYSTEM_STATE_ON_RESPONSE = "irrigationSystemSwitchedOn";
    public static final String IRRIGATION_SYSTEM_STATE_OFF_RESPONSE = "irrigationSystemSwitchedOff";
    public static final String IRRIGATION_SYSTEM_SET_SCHEDULING_RESPONSE = "irrSysSchedulingSet";
    private static final String STATE_TRUE_VALUE = "1";
    private String model;
    private boolean state;
    private ArrayList<IrrigationSystemActivityLogInstance> activityLog;
    private IrrigationSystemScheduling irrigationSystemScheduling;

    // CONSTRUCTOR
    public IrrigationSystem(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            // MODEL RECOVERING
            this.model = jsonOBJ.getString(SqlDbHelper.TABLE_IRR_SYS_MODEL_COLUMN_NAME);
            // STATE RECOVERING
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_IRR_SYS_STATE_COLUMN_NAME)) this.state = jsonOBJ.getString(SqlDbHelper.TABLE_IRR_SYS_STATE_COLUMN_NAME).equals(STATE_TRUE_VALUE);
            else this.state = false;
            // ACTIVITY LOG RECOVERING
            this.activityLog = new ArrayList<>();
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_IRR_SYS_ACTIVITY_LOG_COLUMN_NAME)) {
                String tmpString = jsonOBJ.getString(SqlDbHelper.TABLE_IRR_SYS_ACTIVITY_LOG_COLUMN_NAME);
                JSONArray jsonArray = new JSONArray(tmpString);
                for(int i=0; i<jsonArray.length(); i++) {
                    this.activityLog.add(new IrrigationSystemActivityLogInstance(jsonArray.getString(i)));
                }
            }
            // IRRIGATION SYSTEM SCHEDULING RECOVERING
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_IRR_SYS_SCHEDULING_COLUMN_NAME))
                this.irrigationSystemScheduling = new IrrigationSystemScheduling(jsonOBJ.getString(SqlDbHelper.TABLE_IRR_SYS_SCHEDULING_COLUMN_NAME));
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
        if(state)
            stateInt = 1;
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, deviceID);
        contentValues.put(SqlDbHelper.TABLE_IRR_SYS_ID_COLUMN_NAME, irrigationSystemID);
        contentValues.put(SqlDbHelper.TABLE_IRR_SYS_STATE_COLUMN_NAME, stateInt);
        SqlDbHelper.setIrrSysState(contentValues, (response -> {
            if(response != null) {
                if(state && response.equals(IRRIGATION_SYSTEM_STATE_ON_RESPONSE)) this.state = true;
                else if(!state && response.equals(IRRIGATION_SYSTEM_STATE_OFF_RESPONSE)) this.state = false;
            }
        }));
    }

    public static void setScheduling(@NonNull Context context, Calendar calendar, int[] irrigationDuration, Common.OnStringResponseGivenCallback callback) {
        SqlDbHelper.setIrrSysScheduling(context, calendar, irrigationDuration, (callback));
    }

    public IrrigationSystemScheduling getIrrigationSystemScheduling() {
        return this.irrigationSystemScheduling;
    }

    @NonNull
    @Override
    public String toString() {
        return SqlDbHelper.TABLE_IRR_SYS_MODEL_COLUMN_NAME + ": " + this.model + ", " + SqlDbHelper.TABLE_IRR_SYS_STATE_COLUMN_NAME + ": " + this.state;
    }

    // PARCELABLE IMPLEMENTATION
    protected IrrigationSystem(Parcel in) {
        model = in.readString();
        state = in.readByte() != 0;
        activityLog = in.createTypedArrayList(IrrigationSystemActivityLogInstance.CREATOR);
        irrigationSystemScheduling = in.readParcelable(IrrigationSystemScheduling.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(model);
        dest.writeByte((byte) (state ? 1 : 0));
        dest.writeTypedList(activityLog);
        dest.writeParcelable(irrigationSystemScheduling, flags);
    }

    @Override
    public int describeContents() {
        return 0;
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
}
