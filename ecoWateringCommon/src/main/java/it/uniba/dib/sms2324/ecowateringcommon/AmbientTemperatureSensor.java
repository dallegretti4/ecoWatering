package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

public class AmbientTemperatureSensor extends EcoWateringSensor{
    public AmbientTemperatureSensor(@NonNull Context context) {
        super(context);
    }
    public AmbientTemperatureSensor(String sensorID) {
        super(sensorID);
    }
    public AmbientTemperatureSensor(@NonNull Context context, String sensorID) {
        super(context, sensorID);
    }

    @Override
    public List<Sensor> getSensorListFromDevice() {
        return sensorManager.getSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    @Override
    public void updateSensorValueOnDbServer(@NonNull Context context) {
        new Thread(() -> {
            String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"UPDATE_AMBIENT_TEMPERATURE_SENSOR\",\"VALUE\":" + this.currentValue + "}";
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "updateAmbTempSensor response: " + response);
        }).start();
    }

    // PARCELABLE IMPLEMENTATION

    public AmbientTemperatureSensor(Parcel in) {
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

    public static final Creator<AmbientTemperatureSensor> CREATOR = new Creator<AmbientTemperatureSensor>() {
        @Override
        public AmbientTemperatureSensor createFromParcel(Parcel parcel) {
            return new AmbientTemperatureSensor(parcel);
        }

        @Override
        public AmbientTemperatureSensor[] newArray(int i) {
            return new AmbientTemperatureSensor[i];
        }
    };
}
