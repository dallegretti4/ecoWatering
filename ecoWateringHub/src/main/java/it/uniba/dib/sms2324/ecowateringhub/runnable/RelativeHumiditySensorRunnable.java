package it.uniba.dib.sms2324.ecowateringhub.runnable;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.RelativeHumiditySensor;

public class RelativeHumiditySensorRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub ecoWateringHub;
    private final int duration;

    public RelativeHumiditySensorRunnable(@NonNull Context context, @NonNull EcoWateringHub ecoWateringHub, int duration) {
        this.context = context;
        this.ecoWateringHub = ecoWateringHub;
        this.duration = duration;
    }

    @Override
    public void run() {
        if((this.ecoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor() != null) &&
                (this.ecoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID() != null)) {

            RelativeHumiditySensor relativeHumiditySensor = new RelativeHumiditySensor(
                    this.context,
                    this.ecoWateringHub.getEcoWateringHubConfiguration().getRelativeHumiditySensor().getSensorID()
            );
            if(relativeHumiditySensor.getSelectedSensor() != null) {
                relativeHumiditySensor.register();
                try {
                    Thread.sleep(this.duration);
                    relativeHumiditySensor.unregister();
                    relativeHumiditySensor.updateSensorValueOnDbServer(this.context);
                }
                catch(InterruptedException e) {
                    relativeHumiditySensor.unregister();
                    e.printStackTrace();
                }
            }
        }
    }
}
