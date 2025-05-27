package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class AmbientTemperatureSensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub hub;
    private final long duration;

    public AmbientTemperatureSensorRunnable(@NonNull Context context, @NonNull EcoWateringHub hub, long duration) {
        this.context = context;
        this.hub = hub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.hub.getSensorInfo() != null) && (this.hub.getSensorInfo().getAmbientTemperatureChosenSensor() != null)) {

            AmbientTemperatureSensor ambientTemperatureSensor = new AmbientTemperatureSensor(
                    this.context,
                    this.hub.getSensorInfo().getAmbientTemperatureChosenSensor()
            );
            if(ambientTemperatureSensor.getSensor() != null) {
                ambientTemperatureSensor.register();
                try {
                    Thread.sleep(this.duration);
                    ambientTemperatureSensor.unregister();
                    ambientTemperatureSensor.updateSensorValueOnServerDb(this.context);
                }
                catch(InterruptedException e) {
                    ambientTemperatureSensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
