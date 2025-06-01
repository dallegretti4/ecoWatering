package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;

public abstract class EcoWateringSensor implements SensorEventListener {
    protected final String sensorID;
    protected SensorManager sensorManager;
    protected Sensor sensor;
    protected double currentValue = 0;
    public EcoWateringSensor(@NonNull Context context, String sensorID) {
        this.sensorID = sensorID;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.sensor = recoverSensorFromSensorID(context);
    }

    protected abstract Sensor recoverSensorFromSensorID(@NonNull Context context);
    protected abstract void updateSensorValueOnServerDb(@NonNull Context context);
    public Sensor getSensor() {
        return this.sensor;
    }

    public void register() {
        if(this.sensor != null && this.sensorManager != null) this.sensorManager.registerListener(this, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregister() {
        this.sensorManager.unregisterListener(this);
    }


    // SENSOR EVENT LISTENER IMPLEMENTATION
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override
    public void onSensorChanged(SensorEvent event) {
        if((event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) || (event.sensor.getType() == Sensor.TYPE_LIGHT) || (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY)) {
            Log.i(Common.LOG_NORMAL, "light sensor event: " + event.values[0]);
            this.currentValue = event.values[0];
        }
    }

    public static ArrayList<Sensor> getConnectedSensorList(@NonNull Context context, String sensorType) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        int sensorTypeInt = Sensor.TYPE_AMBIENT_TEMPERATURE;
        if(sensorType.equals(SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT)) sensorTypeInt = Sensor.TYPE_LIGHT;
        else if(sensorType.equals(SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY)) sensorTypeInt = Sensor.TYPE_RELATIVE_HUMIDITY;
        if((sensorManager.getSensorList(sensorTypeInt) != null) && (!sensorManager.getSensorList(sensorTypeInt).isEmpty())) return new ArrayList<>(sensorManager.getSensorList(sensorTypeInt));
        else return new ArrayList<>();
    }
}
