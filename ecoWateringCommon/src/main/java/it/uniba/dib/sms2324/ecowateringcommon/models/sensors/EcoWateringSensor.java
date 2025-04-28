package it.uniba.dib.sms2324.ecowateringcommon.models.sensors;

import android.content.Context;
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

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public abstract class EcoWateringSensor implements Parcelable, SensorEventListener {
    public static final String CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE = "ambient temperature sensor";
    public static final String CONFIGURE_SENSOR_TYPE_LIGHT = "light sensor";
    public static final String CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY = "relative humidity sensor";
    public static final String EW_SENSOR_ID_SEPARATOR = " - ";
    private String sensorID;
    protected SensorManager sensorManager;
    private Sensor selectedSensor;
    protected double currentValue;

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
        return sensor.getStringType() + EW_SENSOR_ID_SEPARATOR + sensor.getName() + EW_SENSOR_ID_SEPARATOR + sensor.getVendor() + EW_SENSOR_ID_SEPARATOR + sensor.getVersion();
    }

    public Sensor recoverSensorFromID() {
        String[] sensorInfo = this.sensorID.split(EW_SENSOR_ID_SEPARATOR);
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

    public static void addNewEcoWateringSensor(@NonNull Context context, @NonNull Sensor sensor, String hubId, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubId + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_ADD_NEW_SENSOR +  "\",\"" +
                HttpHelper.REMOTE_DEVICE_PARAMETER + "\":\""+ Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.SENSOR_TYPE_PARAMETER + "\":\"" + sensor.getStringType() + "\",\"" +
                HttpHelper.SENSOR_ID_PARAMETER + "\":\"" + EcoWateringSensor.getSensorId(sensor) + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "addNewEWSensorObj response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public void detachSelectedSensor(@NonNull Context context, Common.OnStringResponseGivenCallback callback) {
        String sensorType = Common.VOID_STRING_VALUE;
        if(this instanceof AmbientTemperatureSensor) {
            sensorType = CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE;
        }
        else if(this instanceof LightSensor) {
            sensorType = CONFIGURE_SENSOR_TYPE_LIGHT;
        }
        else if(this instanceof RelativeHumiditySensor) {
            sensorType = CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY;
        }
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_DETACH_SENSOR + "\",\"" +
                HttpHelper.SENSOR_TYPE_PARAMETER + "\":\"" + sensorType + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "detach sensor response: " + response);
            callback.getResponse(response);
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
