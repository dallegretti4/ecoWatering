package it.uniba.dib.sms2324.ecowateringhub.runnable;

import android.content.Context;

import androidx.annotation.NonNull;

import it.uniba.dib.sms2324.ecowateringcommon.WeatherInfo;

public class WeatherInfoRunnable implements Runnable {
    private final Context context;

    public WeatherInfoRunnable(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        WeatherInfo.updateWeatherInfo(this.context);
    }
}
