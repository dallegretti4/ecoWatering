package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class DataObjectRefreshingRunnable implements Runnable {
    public static final long DATA_OBJECT_REFRESHING_RUNNABLE_DURATION = 2 * 1000; // MILLISECONDS
    public static final int FORCE_SENSORS_DURATION = 500; // MILLISECONDS
    private final Context context;
    private final EcoWateringHub hub;
    private final long duration;
    public DataObjectRefreshingRunnable(@NonNull Context context, @NonNull EcoWateringHub hub, long duration) {
        this.context = context;
        this.hub = hub;
        this.duration = duration;
    }

    @Override
    public void run() {
        new Thread(() -> WeatherInfo.updateWeatherInfo(this.context, this.hub)).start();
        new Thread(new AmbientTemperatureSensorRunnable(context, this.hub, duration)).start();
        SensorsInfo.updateSensorList(this.context, this.hub.getDeviceID(), SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE, EcoWateringSensor.getConnectedSensorList(this.context, SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE));
        new Thread(new LightSensorRunnable(context, this.hub, duration)).start();
        SensorsInfo.updateSensorList(this.context, this.hub.getDeviceID(), SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT, EcoWateringSensor.getConnectedSensorList(this.context, SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT));
        new Thread(new RelativeHumiditySensorRunnable(context, this.hub, duration)).start();
        SensorsInfo.updateSensorList(this.context, this.hub.getDeviceID(), SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY, EcoWateringSensor.getConnectedSensorList(this.context, SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY));
        new Thread(new LightSensorEventRunnable()).start(); // TO DEBUG BACKGROUND SERVICE WORK
    }
}
