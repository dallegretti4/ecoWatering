package it.uniba.dib.sms2324.ecowateringhub.runnable.sensors;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.AmbientTemperatureSensor;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class AmbientTemperatureSensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub ecoWateringHub;
    private final int duration;

    public AmbientTemperatureSensorRunnable(@NonNull Context context, @NonNull EcoWateringHub ecoWateringHub, int duration) {
        this.context = context;
        this.ecoWateringHub = ecoWateringHub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.ecoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor() != null) &&
                (this.ecoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID() != null)) {

            AmbientTemperatureSensor ambientTemperatureSensor = new AmbientTemperatureSensor(
                    this.context,
                    this.ecoWateringHub.getEcoWateringHubConfiguration().getAmbientTemperatureSensor().getSensorID()
            );
            if(ambientTemperatureSensor.getSelectedSensor() != null) {
                ambientTemperatureSensor.register();
                try {
                    Thread.sleep(this.duration);
                    ambientTemperatureSensor.unregister();
                    ambientTemperatureSensor.updateSensorValueOnDbServer(this.context);
                }
                catch(InterruptedException e) {
                    ambientTemperatureSensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
