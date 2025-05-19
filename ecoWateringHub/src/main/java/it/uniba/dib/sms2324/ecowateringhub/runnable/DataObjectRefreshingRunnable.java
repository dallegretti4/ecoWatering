package it.uniba.dib.sms2324.ecowateringhub.runnable;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.runnable.hub.RefreshHubRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.AmbientTemperatureSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.LightSensorEventRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.LightSensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.sensors.RelativeHumiditySensorRunnable;
import it.uniba.dib.sms2324.ecowateringhub.runnable.weather.WeatherInfoRunnable;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;

public class DataObjectRefreshingRunnable implements Runnable {
    private static final long DATA_OBJECT_SENSORS_WEATHER_POST_DELAY = 15 * 1000;
    private static final long DATA_OBJECT_DIFFERENCE_DELAY = 45 * 1000;
    private final Context context;
    private final EcoWateringHub hub;
    public DataObjectRefreshingRunnable(@NonNull Context context, @NonNull EcoWateringHub hub) {
        this.context = context;
        this.hub = hub;
    }

    @Override
    public void run() {
        while(EcoWateringForegroundService.isDataObjectRefreshingServiceRunning) {
            new Thread(new AmbientTemperatureSensorRunnable(context, this.hub, 5000)).start();
            new Thread(new LightSensorRunnable(context, this.hub, 5000)).start();
            new Thread(new RelativeHumiditySensorRunnable(context, this.hub, 5000)).start();
            new Thread(new WeatherInfoRunnable(context)).start();
            // TO DEBUG BACKGROUND SERVICE WORK
            new Thread(new LightSensorEventRunnable()).start();

            try { Thread.sleep(DATA_OBJECT_SENSORS_WEATHER_POST_DELAY); }
            catch (InterruptedException e) { e.printStackTrace(); }
            new Thread(new RefreshHubRunnable(hub.getDeviceID())).start();

            try { Thread.sleep(DATA_OBJECT_DIFFERENCE_DELAY); }
            catch (InterruptedException e) { e.printStackTrace(); }
            EcoWateringForegroundService.updateNotification(this.context, this.hub);
        }
    }
}
