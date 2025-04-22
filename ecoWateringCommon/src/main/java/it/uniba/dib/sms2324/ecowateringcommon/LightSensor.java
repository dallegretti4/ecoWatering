package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

public class LightSensor extends EcoWateringSensor{
    public LightSensor(@NonNull Context context) {
        super(context);
    }
    public LightSensor(String sensorID) {
        super(sensorID);
    }
    public LightSensor(@NonNull Context context, String sensorID) {
        super(context, sensorID);
    }

    @Override
    public List<Sensor> getSensorListFromDevice() {
        return super.sensorManager.getSensorList(Sensor.TYPE_LIGHT);
    }

    @Override
    public void updateSensorValueOnDbServer(@NonNull Context context) {
        new Thread(() -> {
            String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"UPDATE_LIGHT_SENSOR\",\"VALUE\":" + this.currentValue + "}";
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "updateLightSensor response: " + response);
        }).start();
    }

    // PARCELABLE IMPLEMENTATION
    public LightSensor(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    public static final Creator<LightSensor> CREATOR = new Creator<LightSensor>() {
        @Override
        public LightSensor createFromParcel(Parcel parcel) {
            return new LightSensor(parcel);
        }

        @Override
        public LightSensor[] newArray(int i) {
            return new LightSensor[i];
        }
    };
}

