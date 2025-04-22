package it.uniba.dib.sms2324.ecowateringhub.runnable;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.HttpHelper;

public class LightSensorEventRunnable implements Runnable {
    public LightSensorEventRunnable() {}

    @Override
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String date = sdf.format(new Date());
        Log.i(Common.THIS_LOG, date);
        String jsonString = "{\"MODE\":\"LIGHT_SENSOR_EVENT\",\"TIME\":\"" + date + "\"}";
        String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
        Log.i(Common.THIS_LOG, "light sensor event response: " + response);
    }
}
