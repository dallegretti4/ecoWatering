package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

public class RelativeHumiditySensor extends EcoWateringSensor{
    public RelativeHumiditySensor(@NonNull Context context) {
        super(context);
    }
    public RelativeHumiditySensor(String sensorID) {
        super(sensorID);
    }
    public RelativeHumiditySensor(@NonNull Context context, String sensorID) {
        super(context, sensorID);
    }
    @Override
    public List<Sensor> getSensorListFromDevice() {
        return sensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY);
    }

    @Override
    public void updateSensorValueOnDbServer(@NonNull Context context) {
        new Thread(() -> {
            String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"UPDATE_RELATIVE_HUMIDITY_SENSOR\",\"VALUE\":" + this.currentValue + "}";
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "updateRelativeHumiditySensor response: " + response);
        }).start();
    }

    // PARCELABLE IMPLEMENTATION
    public RelativeHumiditySensor(Parcel in) {
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

    public static final Creator<RelativeHumiditySensor> CREATOR = new Creator<RelativeHumiditySensor>() {
        @Override
        public RelativeHumiditySensor createFromParcel(Parcel parcel) {
            return new RelativeHumiditySensor(parcel);
        }

        @Override
        public RelativeHumiditySensor[] newArray(int i) {
            return new RelativeHumiditySensor[i];
        }
    };
}
