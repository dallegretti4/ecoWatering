package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class RelativeHumiditySensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub hub;
    private final long duration;

    public RelativeHumiditySensorRunnable(@NonNull Context context, @NonNull EcoWateringHub hub, long duration) {
        this.context = context;
        this.hub = hub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.hub.getSensorInfo() != null) && (this.hub.getSensorInfo().getRelativeHumidityChosenSensor() != null)) {

            RelativeHumiditySensor relativeHumiditySensor = new RelativeHumiditySensor(
                    this.context,
                    this.hub.getSensorInfo().getRelativeHumidityChosenSensor()
            );
            if(relativeHumiditySensor.getSensor() != null) {
                relativeHumiditySensor.register();
                try {
                    Thread.sleep(this.duration);
                    relativeHumiditySensor.unregister();
                    relativeHumiditySensor.updateSensorValueOnServerDb(this.context);
                }
                catch(InterruptedException e) {
                    relativeHumiditySensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
