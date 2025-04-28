package it.uniba.dib.sms2324.ecowateringhub.runnable.sensors;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;

public class LightSensorEventRunnable implements Runnable {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public LightSensorEventRunnable() {}

    @Override
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault());
        String date = sdf.format(new Date());
        Log.i(Common.THIS_LOG, date);
        String jsonString = "{\"" + HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_LIGHT_SENSOR_EVENT + "\",\"" + HttpHelper.TIME_PARAMETER + "\":\"" + date + "\"}";
        String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
        Log.i(Common.THIS_LOG, "light sensor event response: " + response);
    }
}
