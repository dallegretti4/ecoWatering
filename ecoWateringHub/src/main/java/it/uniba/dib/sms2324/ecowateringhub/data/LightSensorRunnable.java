package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class LightSensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub hub;
    private final long duration;

    public LightSensorRunnable(@NonNull Context context, @NonNull EcoWateringHub hub, long duration) {
        this.context = context;
        this.hub = hub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.hub.getSensorInfo() != null) && (this.hub.getSensorInfo().getLightChosenSensor() != null)) {
            LightSensor lightSensor = new LightSensor(
                    this.context,
                    this.hub.getSensorInfo().getLightChosenSensor()
            );
            if(lightSensor.getSensor() != null) {
                lightSensor.register();
                try {
                    Thread.sleep(this.duration);
                    lightSensor.unregister();
                    lightSensor.updateSensorValueOnServerDb(this.context);
                }
                catch(InterruptedException e) {
                    lightSensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
