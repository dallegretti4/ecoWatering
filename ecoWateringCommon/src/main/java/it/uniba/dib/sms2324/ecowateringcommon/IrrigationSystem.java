package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class IrrigationSystem implements Parcelable {
    public static final String TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME = "id";
    public static final String TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME = "model";
    public static final String TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME = "state";
    private String id;
    private String model;
    private boolean state;

    public interface IrrigationSystemFromJsonStringCallback {
        void convert(String jsonString);
    }
    public interface OnModelSelectedCallback {
        void getResponse(String response);
    }
    public interface OnChangeStateCallback {
        void onStateChanged(boolean requestedState, boolean outcome);
    }

    // CONSTRUCTOR
    public IrrigationSystem(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            this.id = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME);
            // MODEL RECOVERING
            this.model = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME);
            // STATE RECOVERING
            String tmpString = jsonOBJ.getString(TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME) ;
            this.state = tmpString.equals("1");
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public IrrigationSystem(String id, String model) {
        this.id = id;
        this.model = model;
    }

    public String getId() {
        return this.id;
    }
    public String getModel() {
        return this.model;
    }

    public boolean getState() {
        return this.state;
    }

    public static IrrigationSystem discoverIrrigationSystem(@NonNull Context context, boolean isSimulatedMode) {
        if(isSimulatedMode) {
            return new IrrigationSystem(Common.getThisDeviceID(context), "DALL - Irrigation System - SIMULATED");
        }
        else {
            return null;
        }
    }

    /**
     *
     * @param deviceID deviceID of the device that called method.
     * @param state true if user want to switch on the irrigation system, false to switch off.
     * @param callback to manage server response.
     *
     */
    public void setState(String deviceID, boolean state, OnChangeStateCallback callback) {
        int stateInt = 0;
        if(state) {
            stateInt = 1;
        }
        String jsonString = "{\"" + EcoWateringDevice.TABLE_DEVICE_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID +
                "\",\"MODE\":\"SET_IRRIGATION_SYSTEM_STATE\",\"" +
                TABLE_IRRIGATION_SYSTEM_ID_COLUMN_NAME + "\":\"" + this.id +
                "\",\"" + TABLE_IRRIGATION_SYSTEM_STATE_COLUMN_NAME + "\":\"" + stateInt + "\"}";

        Log.i(Common.THIS_LOG, "irrSys query: " + jsonString);
        Thread changeStateThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "irrSysChangeState response: " + response);
            if(response != null) {
                if(state && response.equals(Common.IRRIGATION_SYSTEM_STATE_ON_RESPONSE)) {
                    this.state = true;
                    callback.onStateChanged(true, true);
                }
                else if(!state && response.equals(Common.IRRIGATION_SYSTEM_STATE_OFF_RESPONSE)) {
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
        return "irrSys " + this.id + "model " + this.model + ", state: " + this.state;
    }

    // PARCELABLE IMPLEMENTATION
    protected IrrigationSystem(Parcel in) {
        id = in.readString();
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
        parcel.writeString(id);
        parcel.writeString(model);
        parcel.writeByte((byte) (state ? 1 : 0));
    }
}
