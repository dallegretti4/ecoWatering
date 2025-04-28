package it.uniba.dib.sms2324.ecowateringhub.runnable.sensors;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.LightSensor;

public class LightSensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub ecoWateringHub;
    private final int duration;

    public LightSensorRunnable(@NonNull Context context, @NonNull EcoWateringHub ecoWateringHub, int duration) {
        this.context = context;
        this.ecoWateringHub = ecoWateringHub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.ecoWateringHub.getEcoWateringHubConfiguration().getLightSensor() != null) &&
                (this.ecoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID() != null)) {

            LightSensor lightSensor = new LightSensor(
                    this.context,
                    this.ecoWateringHub.getEcoWateringHubConfiguration().getLightSensor().getSensorID()
            );
            if(lightSensor.getSelectedSensor() != null) {
                lightSensor.register();
                try {
                    Thread.sleep(this.duration);
                    lightSensor.unregister();
                    lightSensor.updateSensorValueOnDbServer(this.context);
                }
                catch(InterruptedException e) {
                    lightSensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
