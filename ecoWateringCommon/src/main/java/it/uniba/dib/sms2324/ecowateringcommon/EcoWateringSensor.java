package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public abstract class EcoWateringSensor implements Parcelable, SensorValueProvider, SensorEventListener {
    private String sensorID;
    protected SensorManager sensorManager;
    protected Sensor selectedSensor;
    protected double currentValue;

    public interface AddNewEcoWateringSensorCallback {
        void getResponse(String response);
    }

    // CONSTRUCTORS
    public EcoWateringSensor(@NonNull Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
    public EcoWateringSensor(@NonNull Context context, String sensorID) {
        this.sensorID = sensorID;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.selectedSensor = this.recoverSensorFromID();
    }
    // UNIQUE CONSTRUCTOR FOR ECO WATERING DEVICE
    public EcoWateringSensor(String sensorID) {
        this.sensorID = sensorID;
    }


    // METHODS

    public abstract List<Sensor> getSensorListFromDevice();
    public abstract void updateSensorValueOnDbServer(@NonNull Context context);

    public static String getSensorId(Sensor sensor) {
        return sensor.getStringType() + Common.EW_SENSOR_ID_SEPARATOR + sensor.getName() + Common.EW_SENSOR_ID_SEPARATOR + sensor.getVendor() + Common.EW_SENSOR_ID_SEPARATOR + sensor.getVersion();
    }

    public Sensor recoverSensorFromID() {
        String[] sensorInfo = this.sensorID.split(Common.EW_SENSOR_ID_SEPARATOR);
        ArrayList<Sensor> deviceSensors = new ArrayList<>(getSensorListFromDevice());
        if(!deviceSensors.isEmpty()) {
            for(Sensor sensor : deviceSensors) {
                if((sensor.getStringType().equals(sensorInfo[0])) &&
                        (sensor.getName().equals(sensorInfo[1])) &&
                        (sensor.getVendor().equals(sensorInfo[2])) &&
                        (String.valueOf(sensor.getVersion()).equals(sensorInfo[3]))) {
                    return sensor;
                }
            }
        }
        return null;
    }

    public String getSensorID() {
        return this.sensorID;
    }

    public Sensor getSelectedSensor() {
        return this.selectedSensor;
    }

    public static void addNewEcoWateringSensor(@NonNull Context context, @NonNull Sensor sensor, String hubId, AddNewEcoWateringSensorCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubId + "\",\"REMOTE_DEVICE\":\""+ Common.getThisDeviceID(context) + "\",\"MODE\":\"ADD_NEW_SENSOR\",\"SENSOR_TYPE\":\"" + sensor.getStringType() + "\",\"SENSOR_ID\":\"" + EcoWateringSensor.getSensorId(sensor) + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "addNewEWSensorObj response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public void detachSelectedSensor(@NonNull Context context, String sensorType) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"DETACH_SENSOR\",\"SENSOR_TYPE\":\"" + sensorType + "\"}";
        Log.i(Common.THIS_LOG, "detach query: " + jsonString);
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "detach " + sensorType + " sensor response: " + response);
            Common.restartApp(context);
        }).start();
    }

    public void register() {
        if(this.selectedSensor != null && this.sensorManager != null) {
            this.sensorManager.registerListener(this, this.selectedSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unregister() {
        this.sensorManager.unregisterListener(this);
    }

    // SENSOR PROVIDER
    @Override
    public double getSensorValue() {
        return this.currentValue;
    }

    // SENSOR EVENT LISTENER IMPLEMENTATION
    @Override
    public void onSensorChanged(SensorEvent event) {
        if((event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) || (event.sensor.getType() == Sensor.TYPE_LIGHT) || (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY)) {
            Log.i(Common.THIS_LOG, "light sensor event: " + event.values[0]);
            this.currentValue = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }


    // PARCELABLE IMPLEMENTATION

    public EcoWateringSensor(Parcel in) {
        this.sensorID = in.readString();
        this.currentValue = in.readDouble();
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(this.sensorID);
        parcel.writeDouble(this.currentValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
